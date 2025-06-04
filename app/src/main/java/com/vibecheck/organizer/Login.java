package com.vibecheck.organizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibecheck.organizer.network.ApiService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private EditText email, password;
    private TextView txtLinkToRegister;
    private ApiService apiService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //verificar se usuário já está logado
        SharedPreferences sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        if (sharedPreferences.contains("id")) {
            Log.d("MainActivity", "User ID found in SharedPreferences. Redirecting to ListEvents.");
            Intent intent = new Intent(this, ListEvents.class);
            // Use FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK to prevent going back
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish MainActivity so it's removed from the back stack
            return; // Stop further execution of onCreate
        }

        email = findViewById(R.id.edtLoginEmail);
        password = findViewById(R.id.edtLoginPassword);

        txtLinkToRegister = findViewById(R.id.linkToRegister);

        apiService = new ApiService(ContextCompat.getMainExecutor(this));

    }

    public void sendToRegisterForm(View view){

        finish();

    }

    public void loginEntity(View view) { // Remova 'throws JsonProcessingException' daqui
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("email", email.getText().toString());
        jsonMap.put("password", password.getText().toString());


        String jsonString;
        try {
            // 2. Converter Map para String JSON
            jsonString = new ObjectMapper().writeValueAsString(jsonMap);
            Log.d("Login", "JSON a ser enviado: " + jsonString);
        } catch (JsonProcessingException e) {
            Log.e("Login", "Erro ao converter Map para JSON: " + e.getMessage());
            Toast.makeText(this, "Erro interno ao preparar dados.", Toast.LENGTH_SHORT).show();
            return; // Sai do método se o JSON não puder ser gerado
        }

        // 3. Chamar a requisição POST assíncrona
        String loginUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/organizers/login"; // <-- Defina a URL CORRETA aqui!

        apiService.post(loginUrl, jsonString, new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                // Código será executado na UI Thread por causa do runOnUiThread() no ApiService
                Log.d("Login", "Sucesso no login: " + responseBody);
                Toast.makeText(Login.this, "Login realizado com sucesso!", Toast.LENGTH_LONG).show();

                ObjectMapper localMapper = new ObjectMapper();
                try {
                    Map<String, Object> responseMap = localMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

                    clearAllUserData(Login.this);

                    // Obter uma instância do SharedPreferences
                    // "user_data" é o nome do arquivo XML onde os dados serão salvos
                    // MODE_PRIVATE garante que apenas o seu app pode acessar este arquivo
                    SharedPreferences sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);

                    // Obter um editor para colocar os dados
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    // Salvar cada campo no SharedPreferences
                    // Use as chaves exatamente como estão no JSON para facilitar
                    // Faça verificações para garantir que os valores não são nulos antes de salvar,
                    // caso algum campo possa faltar na resposta da API.

                    if (responseMap.containsKey("organizer_type")) {
                        editor.putInt("organizer_type", (Integer) responseMap.get("organizer_type"));
                    }
                    if (responseMap.containsKey("company_name")) {
                        editor.putString("company_name", (String) responseMap.get("company_name"));
                    }
                    if (responseMap.containsKey("legal_name")) {
                        editor.putString("legal_name", (String) responseMap.get("legal_name"));
                    }
                    if (responseMap.containsKey("cnpj")) {
                        editor.putString("cnpj", (String) responseMap.get("cnpj"));
                    }
                    if (responseMap.containsKey("full_name")) {
                        editor.putString("full_name", (String) responseMap.get("full_name"));
                    }
                    if (responseMap.containsKey("cpf")) {
                        editor.putString("cpf", (String) responseMap.get("cpf"));
                    }
                    if (responseMap.containsKey("email")) {
                        editor.putString("email", (String) responseMap.get("email"));
                    }
                    if (responseMap.containsKey("updated_at")) {
                        editor.putString("updated_at", (String) responseMap.get("updated_at"));
                    }
                    if (responseMap.containsKey("date")) {
                        editor.putString("date", (String) responseMap.get("date"));
                    }
                    if (responseMap.containsKey("id")) {
                        if (responseMap.get("id") instanceof Number) {
                            editor.putLong("id", ((Number) responseMap.get("id")).longValue());
                        }
                    }

                    // Aplicar as mudanças (salvar no arquivo XML)
                    editor.apply();

                    Log.d("SharedPreferences", "Dados do usuário salvos em user_data.xml");
                    Toast.makeText(Login.this, "Usuário entrou!", Toast.LENGTH_SHORT).show();

                    // --- ADIÇÃO PARA NAVEGAR PARA ListEventsActivity ---
                    Intent intent = new Intent(Login.this, ListEvents.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    Log.e("Login", "Erro ao parsear resposta JSON de sucesso: " + e.getMessage(), e);
                    Toast.makeText(Login.this, "Erro interno: Não foi possível processar a resposta do servidor.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                // Código será executado na UI Thread
                Log.e("Login", "Erro no login: Status " + statusCode + ", Mensagem: " + errorMessage);

                if (errorMessage != null && !errorMessage.isEmpty()) {

                    ObjectMapper errorMapper = new ObjectMapper();

                    try {
                        Map<String, Object> errorMap = errorMapper.readValue(errorMessage, new TypeReference<Map<String, Object>>() {});

                        Toast.makeText(Login.this, errorMap.get("message").toString(), Toast.LENGTH_SHORT).show();

                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }


                }

            }

            @Override
            public void onFailure(IOException e) {
                // Código será executado na UI Thread
                Log.e("Login", "Falha de rede no login: " + e.getMessage(), e);
                Toast.makeText(Login.this, "Erro de conexão: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    public static void clearAllUserData(Context context) {
        // Define o nome do arquivo de SharedPreferences
        final String PREF_NAME = "user_data";

        // Obtém o SharedPreferences. O Context é necessário para isso.
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Obtém um editor para modificar os dados.
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Limpa todos os dados.
        editor.clear();

        // Aplica as mudanças de forma assíncrona (não bloqueia a UI).
        editor.apply();
    }

}