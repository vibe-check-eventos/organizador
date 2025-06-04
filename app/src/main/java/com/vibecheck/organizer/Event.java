package com.vibecheck.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibecheck.organizer.network.ApiService;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Event extends AppCompatActivity {

    private EditText edtEventName, edtEventData, edtEventLocal, edtEventDescription;
    private ApiService apiService;
    private Map<String, Object> eventData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.apiService = new ApiService(ContextCompat.getMainExecutor(this));

        // Referências aos EditTexts
        edtEventName = findViewById(R.id.edtEventName);
        edtEventData = findViewById(R.id.edtEventData);
        edtEventLocal = findViewById(R.id.edtEventLocal);
        edtEventDescription = findViewById(R.id.edtEventDescription);

        // Carrega dados do evento e depois preenche os EditText
        fetchEventData();

        // Continua carregando participantes
        loadParticipantsData();

        // ao clicar em algum participante
        ListView list = findViewById(R.id.lvParticipantsEvent);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String participanteSelecionado = (String) adapterView.getItemAtPosition(position);
                String registrationId = "";

                // Extrair o ID da string (Ex: "ID do Ingresso: 10")
                for (String linha : participanteSelecionado.split("\n")) {
                    if (linha.contains("ID do Ingresso")) {
                        String[] partes = linha.split(":");
                        if (partes.length > 1) {
                            registrationId = partes[1].trim();
                        }
                        break; // encontrou, pode sair do loop
                    }
                }

                // Criar o Intent e passar o ID como extra
                Intent intent = new Intent(getApplicationContext(), Checkins.class);

                int registrationIdInt = Integer.parseInt(registrationId);
                intent.putExtra("registration_id", registrationIdInt);

                // Iniciar a Checkins Activity
                startActivity(intent);
            }
        });


    }

    public void updateEvent(View view) {
        String eventId = getIntent().getStringExtra("id");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "ID do evento não encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String updateUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/events/" + eventId;

        // Pega os valores dos EditTexts
        String nome = edtEventName.getText().toString().trim();
        String data = edtEventData.getText().toString().trim();
        String local = edtEventLocal.getText().toString().trim();
        String description = edtEventDescription.getText().toString().trim();

        // Converte a data para o formato ##/##/####
        String dataFormatada = "";
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("yyyy-MM-dd"); // ou outro, conforme o input
            SimpleDateFormat formatoSaida = new SimpleDateFormat("dd/MM/yyyy");
            Date dataFormat = formatoEntrada.parse(data);
            dataFormatada = formatoSaida.format(dataFormat);
        } catch (ParseException e) {
            e.printStackTrace();
            // Pode definir uma data padrão ou avisar o usuário
            dataFormatada = data; // ou ""
        }

        // Monta a String JSON
        String jsonBody = "{"
                + "\"name\":\"" + nome + "\","
                + "\"description\":\"" + description + "\","
                + "\"address\":\"" + local + "\","
                + "\"date\":\"" + dataFormatada + "\""
                + "}";

        Log.d("UpdateEvent", "JSON Body: " + jsonBody);

        apiService.put(updateUrl, jsonBody, new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                Log.d("UpdateEvent", "Evento atualizado com sucesso: " + responseBody);
                runOnUiThread(() -> Toast.makeText(Event.this, "Evento atualizado com sucesso!", Toast.LENGTH_SHORT).show());
                finish();
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                Log.e("UpdateEvent", "Erro ao atualizar evento: " + errorMessage);
                runOnUiThread(() -> Toast.makeText(Event.this, "Erro ao atualizar evento.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFailure(IOException e) {
                Log.e("UpdateEvent", "Falha na atualização: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(Event.this, "Falha na atualização.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void fetchEventData() {
        String eventId = getIntent().getStringExtra("id");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "ID do evento não encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String eventUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/events/" + eventId;

        apiService.get(eventUrl, new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                Log.d("EventData", responseBody);

                ObjectMapper objectMapper = new ObjectMapper();

                try {
                    eventData = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                    Log.d("EventData", "Dados do evento armazenados com sucesso.");

                    // ✅ Preenche os EditTexts com os dados do evento
                    runOnUiThread(() -> populateEventFields());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("EventData", "Erro ao processar JSON do evento: " + e.getMessage());
                    Toast.makeText(Event.this, "Erro ao carregar dados do evento.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                Log.e("EventData", "Erro ao carregar dados do evento: Status " + statusCode + ", Mensagem: " + errorMessage);
                Toast.makeText(Event.this, "Erro ao carregar dados do evento.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IOException e) {
                Log.e("EventData", "Falha na requisição: " + e.getMessage());
                Toast.makeText(Event.this, "Falha ao carregar dados do evento.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateEventFields() {
        if (eventData == null) return;

        String nome = eventData.get("name") != null ? eventData.get("name").toString() : "";
        String data = eventData.get("date") != null ? eventData.get("date").toString() : "";

        // Endereço
        Map<String, Object> eventAddress = (Map<String, Object>) eventData.get("event_address");
        String endereco = "";
        if (eventAddress != null) {
            String street = eventAddress.get("street") != null ? eventAddress.get("street").toString() : "";
            String number = eventAddress.get("number") != null ? eventAddress.get("number").toString() : "";
            String complement = eventAddress.get("complement") != null ? eventAddress.get("complement").toString() : "";
            String city = eventAddress.get("city") != null ? eventAddress.get("city").toString() : "";
            String state = eventAddress.get("state") != null ? eventAddress.get("state").toString() : "";
            String zip = eventAddress.get("zip_code") != null ? eventAddress.get("zip_code").toString() : "";

            endereco = street ;
        }

        // Descrição
        String description = eventData.get("description") != null ? eventData.get("description").toString() : "";

        edtEventName.setText(nome);
        edtEventData.setText(convertYyyyMmDdHhSsToDdMmYyyyHhMm(data));
        edtEventLocal.setText(endereco);
        edtEventDescription.setText(description);
    }

    private void loadParticipantsData() {

        String eventId = getIntent().getStringExtra("id");  // pega o event_id da Intent

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "ID do evento não encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String participantsUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/registrations/" + eventId + "/event";

        ListView list = findViewById(R.id.lvParticipantsEvent);

        apiService.get(participantsUrl, new ApiService.ApiResponseCallback() {

            @Override
            public void onSuccess(String responseBody) {
                //NA VERDADE TUDO AQUI É O INGRESSO
                Log.d("Participants Body:", responseBody);

                findViewById(R.id.txtLoadingParticipants).setVisibility(View.GONE);

                ArrayList<String> participantsFormatted = new ArrayList<>();

                if (responseBody.isEmpty() || responseBody.equals("{\"message\":\"Nenhuma inscrição encontrada para este evento.\"}")) {
                    participantsFormatted.add("Nenhum participante encontrado para este evento.");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(Event.this, android.R.layout.simple_list_item_1, participantsFormatted);
                    list.setAdapter(adapter);
                    return;
                }

                ObjectMapper objectMapper = new ObjectMapper();

                try {
                    List<Map<String, Object>> rawParticipantsList = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});

                    for (Map<String, Object> registrationMap : rawParticipantsList) {

                        String registrationId = registrationMap.get("id") != null ? registrationMap.get("id").toString() : "N/A";

                        Map<String, Object> participantMap = (Map<String, Object>) registrationMap.get("participant");

                        String id = participantMap.get("id") != null ? participantMap.get("id").toString() : "N/A";
                        String name = participantMap.get("name") != null ? participantMap.get("name").toString() : "N/A";
                        String email = participantMap.get("email") != null ? participantMap.get("email").toString() : "N/A";

                        String itemText =
                                "ID do Ingresso: " + registrationId + "\n" +
                                "ID: " + id + "\n" +
                                "Nome: " + name + "\n" +
                                "Email: " + email;

                        participantsFormatted.add(itemText);
                    }

                    if (participantsFormatted.isEmpty()) {
                        participantsFormatted.add("Nenhum participante encontrado.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Participants", "Erro ao desserializar/processar JSON com Jackson: " + e.getMessage());
                    participantsFormatted.clear();
                    participantsFormatted.add("Erro ao carregar participantes.");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(Event.this, android.R.layout.simple_list_item_1, participantsFormatted);
                list.setAdapter(adapter);
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                findViewById(R.id.txtLoadingParticipants).setVisibility(View.GONE);

                Log.e("Participants", "Erro ao carregar participantes: Status " + statusCode + ", Mensagem: " + errorMessage);
                Toast.makeText(Event.this, "Erro ao carregar participantes: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IOException e) {
                findViewById(R.id.txtLoadingParticipants).setVisibility(View.GONE);

                Log.e("Participants", "Falha na requisição: " + e.getMessage());
                Toast.makeText(Event.this, "Falha na requisição: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Converte uma string de data e hora do formato "yyyy-MM-dd HH:mm:ss"
     * para o formato "dd/MM/yyyy HH:mm".
     *
     * @param yyyyMmDdHhSs A string de data e hora no formato "yyyy-MM-dd HH:mm:ss".
     * @return A string de data e hora formatada como "dd/MM/yyyy HH:mm", ou null se o formato de entrada for inválido.
     */
    public static String convertYyyyMmDdHhSsToDdMmYyyyHhMm(String yyyyMmDdHhSs) {
        // 1. Defina o formato de entrada
        // Use "HH" para formato de 24 horas, "hh" para 12 horas com AM/PM
        SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date;
        try {
            // 2. Parse a String de entrada para um objeto Date
            date = inputFormatter.parse(yyyyMmDdHhSs);
        } catch (ParseException e) {
            // Lida com o erro se a string de entrada não corresponder ao formato esperado
            System.err.println("Erro ao parsear a data: " + yyyyMmDdHhSs + ". Formato esperado: yyyy-MM-dd HH:mm:ss");
            return null; // Retorna null ou lança uma exceção, dependendo da sua necessidade de tratamento de erro
        }

        // 3. Defina o formato de saída
        SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // 4. Formate o objeto Date para o String desejado
        return outputFormatter.format(date);
    }

}
