package com.vibecheck.organizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
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

public class MainActivity extends AppCompatActivity {

    private EditText personName, personDocument, personEmail, personPassword;
    private EditText companyName, companyDocument, companyEmail, companyPassword;
    private Button btnRegister;
    private TextView textGoToRegister;
    private LinearLayout personForm, legalForm;
    private RadioButton rdNaturalPerson, rdLegalPerson;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
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

        this.personName = findViewById(R.id.editPersonName);
        this.personEmail = findViewById(R.id.edtLoginEmail);
        this.personDocument = findViewById(R.id.editPersonDocument);
        this.personPassword = findViewById(R.id.edtLoginPassword);

        this.companyName = findViewById(R.id.editCompanyName);
        this.companyEmail = findViewById(R.id.editCompanyEmail);
        this.companyDocument = findViewById(R.id.editCompanyDocument);
        this.companyPassword = findViewById(R.id.editCompanyPassword);

        this.btnRegister = findViewById(R.id.btnLogin);

        this.textGoToRegister = findViewById(R.id.linkToLogin);

        this.personForm = findViewById(R.id.lnlyNaturalPerson);
        this.legalForm = findViewById(R.id.lnlyLegalPerson);

        this.rdLegalPerson = findViewById(R.id.radioButtonLegal);
        this.rdNaturalPerson = findViewById(R.id.radioButtonPerson);

        apiService = new ApiService(ContextCompat.getMainExecutor(this));

    }

    public void showWhichDataForm(View view){

        int personTypeRadioBtnId = view.getId();

        if (personTypeRadioBtnId == R.id.radioButtonPerson){

            this.personForm.setVisibility(View.VISIBLE);
            this.legalForm.setVisibility(View.GONE);

        } else if (personTypeRadioBtnId == R.id.radioButtonLegal) {

            this.personForm.setVisibility(View.GONE);
            this.legalForm.setVisibility(View.VISIBLE);

        }

    }

    public void sendToLoginForm(View view){

        Intent i = new Intent(this.getApplicationContext(), Login.class);

        startActivity(i);

    }

    public void registerEntity(View view) { // Remova 'throws JsonProcessingException' daqui
        Map<String, Object> jsonMap = new HashMap<>();

        // 1. Coletar dados da UI e construir o Map
        boolean isNaturalPerson = this.rdNaturalPerson.isChecked(); // Melhor nome de variável

        if (isNaturalPerson) { // Pessoa Física
            jsonMap.put("organizer_type", 1);//true - para pessoa
            jsonMap.put("full_name", personName.getText().toString());
            jsonMap.put("cpf", personDocument.getText().toString());
            jsonMap.put("email", personEmail.getText().toString());
            jsonMap.put("password", personPassword.getText().toString());
        } else { // Pessoa Jurídica
            jsonMap.put("organizer_type", 0); //false - para empresa
            jsonMap.put("company_name", companyName.getText().toString());
            jsonMap.put("legal_name", companyName.getText().toString()); // Pode ser diferente de companyName
            jsonMap.put("full_name", companyName.getText().toString()); // Quem representa a empresa?
            jsonMap.put("cnpj", companyDocument.getText().toString());
            jsonMap.put("email", companyEmail.getText().toString());
            jsonMap.put("password", companyPassword.getText().toString());
        }

        String jsonString;
        try {
            // 2. Converter Map para String JSON
            jsonString = new ObjectMapper().writeValueAsString(jsonMap);
            Log.d("Register", "JSON a ser enviado: " + jsonString);
        } catch (JsonProcessingException e) {
            Log.e("Register", "Erro ao converter Map para JSON: " + e.getMessage());
            Toast.makeText(this, "Erro interno ao preparar dados.", Toast.LENGTH_SHORT).show();
            return; // Sai do método se o JSON não puder ser gerado
        }

        // 3. Chamar a requisição POST assíncrona
        String registerUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/organizers"; // <-- Defina a URL CORRETA aqui!

        apiService.post(registerUrl, jsonString, new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                // Código será executado na UI Thread por causa do runOnUiThread() no ApiService
                Log.d("Register", "Sucesso no registro: " + responseBody);
                Toast.makeText(MainActivity.this, "Registro realizado com sucesso!", Toast.LENGTH_LONG).show();

                ObjectMapper localMapper = new ObjectMapper();
                try {
                    Map<String, Object> responseMap = localMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

                    clearAllUserData(MainActivity.this);

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
                    // Geralmente NÃO se salva a senha diretamente no SharedPreferences por segurança.
                    // Se precisar de autenticação persistente, é melhor salvar um token de autenticação.
                    // if (responseMap.containsKey("password")) {
                    //     editor.putString("password", (String) responseMap.get("password"));
                    // }
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
                    Toast.makeText(MainActivity.this, "Dados do usuário salvos!", Toast.LENGTH_SHORT).show();

                    // --- ADIÇÃO PARA NAVEGAR PARA ListEventsActivity ---
                    Intent intent = new Intent(MainActivity.this, ListEvents.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Opcional: Finaliza a MainActivity para que o usuário não possa voltar a ela com o botão "Voltar"
                    // --- FIM DA ADIÇÃO ---

                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    Log.e("Register", "Erro ao parsear resposta JSON de sucesso: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Erro interno: Não foi possível processar a resposta do servidor.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                // Código será executado na UI Thread
                Log.e("Register", "Erro no registro: Status " + statusCode + ", Mensagem: " + errorMessage);

                if (errorMessage != null && !errorMessage.isEmpty()) {

                    ObjectMapper errorMapper = new ObjectMapper();

                    try {
                        Map<String, Object> errorMap = errorMapper.readValue(errorMessage, new TypeReference<Map<String, Object>>() {});

                        Toast.makeText(MainActivity.this, errorMap.get("message").toString(), Toast.LENGTH_SHORT).show();

                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }


                }

            }

            @Override
            public void onFailure(IOException e) {
                // Código será executado na UI Thread
                Log.e("Register", "Falha de rede no registro: " + e.getMessage(), e);
                Toast.makeText(MainActivity.this, "Erro de conexão: " + e.getMessage(), Toast.LENGTH_LONG).show();
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