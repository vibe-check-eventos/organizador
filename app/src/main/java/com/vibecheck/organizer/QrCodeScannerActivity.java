package com.vibecheck.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vibecheck.organizer.network.ApiService;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Activity responsável por escanear códigos QR, analisar os dados
 * e enviar uma solicitação de check-in para uma API.
 */
public class QrCodeScannerActivity extends AppCompatActivity {

    private ApiService apiService;
    // Reutiliza a mesma instância do ObjectMapper para eficiência
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializa o ApiService com um executor de thread única para operações de rede
        apiService = new ApiService(Executors.newSingleThreadExecutor());

        // Inicia o scanner de código QR ZXing imediatamente quando a atividade é criada
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Analisa o resultado do scanner de código QR
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            // Verifica se a leitura foi cancelada (por exemplo, o usuário pressionou o botão Voltar)
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelado", Toast.LENGTH_SHORT).show();
                finish(); // Fecha a atividade se a leitura for cancelada
            } else {
                // Se o conteúdo estiver disponível, manipule os dados lidos
                handleScannedData(result.getContents());
            }
        } else {
            // Se o resultado não for do scanner de código QR, passe-o para o método super
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Manipula o conteúdo do código QR escaneado.
     * Tenta analisar o conteúdo como JSON e extrair o 'id'.
     * @param scannedText O conteúdo bruto escaneado do código QR.
     */
    private void handleScannedData(String scannedText) {
        Log.d("QRCode", "Conteúdo escaneado: " + scannedText);

        try {
            // Lê o texto escaneado como um nó JSON genérico para determinar seu tipo (objeto ou array)
            JsonNode jsonNode = mapper.readTree(scannedText);
            int registrationId = -1; // Valor padrão para ID inválido

            if (jsonNode.isArray()) {
                // Se o JSON for um array, assume que contém objetos e tenta pegar o 'id' do primeiro
                if (jsonNode.size() > 0 && jsonNode.get(0).isObject()) {
                    JsonNode firstObject = jsonNode.get(0);
                    if (firstObject.has("id") && firstObject.get("id").isInt()) {
                        registrationId = firstObject.get("id").asInt();
                    }
                }
            } else if (jsonNode.isObject()) {
                // Se o JSON for um único objeto, tenta pegar o 'id' diretamente
                if (jsonNode.has("id") && jsonNode.get("id").isInt()) {
                    registrationId = jsonNode.get("id").asInt();
                }
            }

            // Verifica se o ID de registro é válido antes de enviar a solicitação de check-in
            if (registrationId > 0) {
                enviarCheckin(registrationId);
            } else {
                Toast.makeText(this, "ID de registro inválido ou não encontrado no QR Code.", Toast.LENGTH_LONG).show();
            }

        } catch (JsonProcessingException e) {
            // Captura erros durante a análise do JSON
            Log.e("QRCode", "Erro ao processar JSON: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao processar JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            // Captura outros erros de I/O (por exemplo, lista vazia, formato inválido)
            Log.e("QRCode", "Erro de I/O: " + e.getMessage(), e);
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Envia uma solicitação de check-in para a API com o ID de registro fornecido.
     * @param registrationId O ID extraído do código QR.
     */
    private void enviarCheckin(int registrationId) {
        // Constrói a string JSON diretamente
        String postData = "{\"registration_id\": " + registrationId + "}";

        // Faz a solicitação POST para o endpoint da API de check-ins
        apiService.post("https://3e46-179-119-53-133.ngrok-free.app/api/checkins", postData,
                new ApiService.ApiResponseCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        // Em caso de sucesso, navega para ConfirmActivity na thread da UI
                        runOnUiThread(() -> goToConfirmActivity());
                    }

                    @Override
                    public void onError(int statusCode, String errorMessage) {
                        // Em caso de erro da API, exibe uma mensagem de erro na thread da UI
                        runOnUiThread(() -> Toast.makeText(QrCodeScannerActivity.this,
                                "Erro do servidor (" + statusCode + "): " + errorMessage, Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onFailure(IOException e) {
                        // Em caso de falha na rede, exibe uma mensagem de falha na thread da UI
                        runOnUiThread(() -> Toast.makeText(QrCodeScannerActivity.this,
                                "Falha na requisição de rede: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
    }

    /**
     * Navega para a ConfirmActivity, limpando a pilha de atividades.
     */
    private void goToConfirmActivity() {
        Intent intent = new Intent(this, ConfirmActivity.class);
        // Define flags para limpar a pilha de atividades, impedindo o retorno ao scanner
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
