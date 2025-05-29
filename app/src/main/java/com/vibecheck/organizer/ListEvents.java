package com.vibecheck.organizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListEvents extends AppCompatActivity {

    private ApiService apiService;
    private ListView lvEvents;
    // Chaves para o SimpleAdapter
    private ArrayList<String> dadosFormatados; // Lista para as strings formatadas
    private ArrayAdapter<String> meuAdapter;
    private TextView txtLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.apiService = new ApiService(ContextCompat.getMainExecutor(this));
        this.lvEvents = findViewById(R.id.lvEvents);
        this.txtLoading = findViewById(R.id.txtLoadingParticipants);

        loadEventsData();

        //ao clicar em um evento
        this.lvEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String eventoSelecionado = (String) adapterView.getItemAtPosition(i);

                String id = "";
                String nome = "";
                String capacidade = "";
                String endereco = "";
                String data = "";

                for (String linha : eventoSelecionado.split("\n")) {
                    if (linha.startsWith("ID:")) {
                        id = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Nome:")) {
                        nome = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Capacidade:")) {
                        capacidade = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Endereço:")) {
                        endereco = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Data:")) {
                        data = linha.substring(linha.indexOf(":") + 1).trim();
                    }
                }

                Intent in = new Intent(getApplicationContext(), Event.class);

                in.putExtra("id", id);
                in.putExtra("nome", nome);
                in.putExtra("capacidade", capacidade);
                in.putExtra("endereco", endereco);
                in.putExtra("data", data);

                startActivity(in);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsData();
    }

    private void loadEventsData(){

        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String organizerId = String.valueOf(sharedPref.getLong("id", 0));

        if (organizerId == "0") {
            Toast.makeText(this, "Id do organizador não encontrado nas preferências.", Toast.LENGTH_SHORT).show();
            txtLoading.setText("Organizador não está autenticado.");
            return;
        }

        String eventsUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/organizers/" + organizerId + "/events";

        ListView list = this.lvEvents;
        apiService.get(eventsUrl, new ApiService.ApiResponseCallback() {

            @Override
            public void onSuccess(String responseBody) {

                txtLoading.setVisibility(View.GONE);

                Log.d("List Events Body:", responseBody);

                // Initialize dadosFormatados here to ensure it's never null
                // and clear it to remove any previous data (important for onResume)
                if (dadosFormatados == null) {
                    dadosFormatados = new ArrayList<>();
                } else {
                    dadosFormatados.clear(); // Clear previous data
                }

                // Handle cases where responseBody is empty or represents an empty JSON array directly
                // This is less likely if your API always returns an object wrapper, but good for robustness.
                if (responseBody.isEmpty() || responseBody.equals("{\"message\":\"Nenhum evento encontrado para este organizador.\"}")) {
                    ArrayList<String> noDataListView = new ArrayList<>();
                    noDataListView.add("Você não tem eventos registrados.");
                    ArrayAdapter<String> listEvents = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, noDataListView);
                    list.setAdapter(listEvents);
                    return; // Exit here if no data
                }

                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> rawEventsList = new ArrayList<>();

                try {
                    // Direct deserialization to a List of Maps, as the JSON starts with '['
                    rawEventsList = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});

                    Log.d("ListEvents", "Dados brutos carregados: " + rawEventsList.size());

                    // Iterate over the rawEventsList and format each item into a single string
                    for (Map<String, Object> eventMap : rawEventsList) {

                        Integer id = (Integer) eventMap.get("id");
                        String name = (String) eventMap.get("name");
                        Object capacityObj = eventMap.get("capacity");
                        String capacity = (capacityObj != null) ? String.valueOf(capacityObj) : "N/A";

                        String address = "Endereço Indisponível";
                        Object eventAddressObj = eventMap.get("event_address");
                        if (eventAddressObj instanceof Map) {
                            Map<String, Object> addressMap = (Map<String, Object>) eventAddressObj;
                            String street = (String) addressMap.get("street");
                            String number = (String) addressMap.get("number");
                            String city = (String) addressMap.get("city");
                            String state = (String) addressMap.get("state");

                            StringBuilder addressBuilder = new StringBuilder();
                            if (street != null && !street.isEmpty()) addressBuilder.append(street);
                            if (number != null && !number.isEmpty()) addressBuilder.append(", ").append(number);
                            if (city != null && !city.isEmpty()) addressBuilder.append(" - ").append(city);
                            if (state != null && !state.isEmpty()) addressBuilder.append("/").append(state);
                            address = addressBuilder.toString();
                            if (address.isEmpty()) address = "Endereço Indisponível";
                        }

                        String createdAt = (String) eventMap.get("created_at");
                        String date = "Data Indisponível";
                        if (createdAt != null && createdAt.length() >= 10) {
                            // Extract just the date part (YYYY-MM-DD)
                            date = createdAt.substring(0, 10);
                        }

                        String itemText =
                                "ID: " + (id != null ? String.valueOf(id) : "N/A") + "\n" +
                                "Nome: " + (name != null ? name : "N/A") + "\n" +
                                //"Capacidade: " + capacity + "\n" +
                                "Endereço: " + address + "\n" +
                                "Data: " + date;

                        dadosFormatados.add(itemText);
                    }

                    // If after processing, the list is still empty (shouldn't happen with valid JSON array, but for safety)
                    if (dadosFormatados.isEmpty()) {
                        dadosFormatados.add("Nenhum evento encontrado no momento.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("ListEvents", "Erro ao desserializar/processar JSON com Jackson: " + e.getMessage());
                    Toast.makeText(ListEvents.this, "Erro ao processar dados de eventos.", Toast.LENGTH_LONG).show();

                    // In case of a parsing error, display an error message in the ListView
                    dadosFormatados.clear(); // Clear any partial data
                    dadosFormatados.add("Ocorreu um erro ao carregar os eventos.");
                }

                // Always create and set the adapter with dadosFormatados
                meuAdapter = new ArrayAdapter<>(ListEvents.this, android.R.layout.simple_list_item_1, dadosFormatados);
                list.setAdapter(meuAdapter);
            }

            @Override
            public void onError(int statusCode, String errorMessage) {

                // Código será executado na UI Thread
                Log.e("Load Events", "Erro ao carregar eventos: Status " + statusCode + ", Mensagem: " + errorMessage);

                if (errorMessage != null && !errorMessage.isEmpty()) {

                    ObjectMapper errorMapper = new ObjectMapper();

                    try {
                        Map<String, Object> errorMap = errorMapper.readValue(errorMessage, new TypeReference<Map<String, Object>>() {});

                        txtLoading.setText(errorMap.get("message").toString());

                        Toast.makeText(ListEvents.this, errorMap.get("message").toString(), Toast.LENGTH_SHORT).show();

                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                }

            }

            @Override
            public void onFailure(IOException e) {
                Log.e("Load Events", "Erro ao carregar eventos: " + e.getMessage());
            }

        });

    }

    public void signOut(View view){

        clearAllUserData(ListEvents.this);

        Intent intent = new Intent(this, MainActivity.class);
        // Use FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK to prevent going back
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

    }

    public void linkToCreateEvent(View view){

        Intent i = new Intent(ListEvents.this, CreateEvent.class);

        startActivity(i);

    }

    public void linkToQRCode(View view){

        Intent i = new Intent(ListEvents.this, QrCodeScannerActivity.class);

        startActivity(i);

    }

    public void linkToProfile(View view){
        Intent intent = new Intent(this, Profile.class);
        startActivity(intent);
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