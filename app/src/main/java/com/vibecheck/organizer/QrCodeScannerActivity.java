package com.vibecheck.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vibecheck.organizer.model.QrCodeData;
import com.vibecheck.organizer.network.ApiService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class QrCodeScannerActivity extends AppCompatActivity {

    private ApiService apiService;
    private final ObjectMapper mapper = new ObjectMapper();  // Reutiliza o mesmo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = new ApiService(Executors.newSingleThreadExecutor());

        // Inicia o scanner ZXing
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                handleScannedData(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedData(String scannedText) {
        Log.d("QRCode", "Scanned content: " + scannedText);

        try {
            JsonNode jsonNode = mapper.readTree(scannedText);
            QrCodeData qrCodeData;

            if (jsonNode.isArray()) {
                // Se for array, pega o primeiro item
                List<QrCodeData> dataList = mapper.readValue(scannedText, new TypeReference<List<QrCodeData>>() {});
                if (dataList.isEmpty()) {
                    throw new IOException("Lista vazia no QR Code.");
                }
                qrCodeData = dataList.get(0);
            } else if (jsonNode.isObject()) {
                // Se for objeto, parse direto
                qrCodeData = mapper.treeToValue(jsonNode, QrCodeData.class);
            } else {
                throw new IOException("Formato inválido de QR Code.");
            }

            enviarCheckin(qrCodeData.id);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao processar JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void enviarCheckin(int registrationId) {
        String postData = "{\"registration_id\": " + registrationId + "}";

        apiService.post("https://3e46-179-119-53-133.ngrok-free.app/api/checkins", postData,
                new ApiService.ApiResponseCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> goToConfirmActivity());
                    }

                    @Override
                    public void onError(int statusCode, String errorMessage) {
                        runOnUiThread(() -> Toast.makeText(QrCodeScannerActivity.this,
                                "Erro: " + errorMessage, Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onFailure(IOException e) {
                        runOnUiThread(() -> Toast.makeText(QrCodeScannerActivity.this,
                                "Falha na requisição: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void goToConfirmActivity() {
        Intent intent = new Intent(this, ConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
