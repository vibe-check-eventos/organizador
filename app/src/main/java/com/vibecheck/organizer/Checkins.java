package com.vibecheck.organizer;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vibecheck.organizer.network.ApiService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale; // Import Locale for SimpleDateFormat
import java.util.TimeZone; // Import TimeZone for SimpleDateFormat

public class Checkins extends AppCompatActivity {

    private ListView lvCheckins;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> checkinList = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkins);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        lvCheckins = findViewById(R.id.lvCheckins);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, checkinList);
        lvCheckins.setAdapter(adapter);

        // Recuperar registration_id do Intent
        int registrationId = getIntent().getIntExtra("registration_id", -1);

        Log.d("REGISTRATION ID:", Integer.toString(registrationId));

        if (registrationId == -1) {
            Toast.makeText(this, "Registration ID inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService = new ApiService(this::runOnUiThread);

        fetchCheckins(registrationId);
    }

    private void fetchCheckins(int registrationId) {
        String url = "https://3e46-179-119-53-133.ngrok-free.app/api/checkins/" + registrationId + "/registration";

        apiService.get(url, new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(responseBody);
                    checkinList.clear();

                    // Verifica se o array JSON está vazio
                    if (jsonArray.length() == 0) {
                        Toast.makeText(Checkins.this, "Nenhum dado de check-in encontrado para esta inscrição.", Toast.LENGTH_LONG).show();
                    } else {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject checkinObj = jsonArray.getJSONObject(i);
                            // Correctly get "created_at"
                            String createdAt = checkinObj.getString("created_at");

                            JSONObject registrationObj = checkinObj.getJSONObject("registration");
                            JSONObject participantObj = registrationObj.getJSONObject("participant");
                            String participantName = participantObj.getString("name");

                            JSONObject eventObj = registrationObj.getJSONObject("event");
                            String eventName = eventObj.getString("name");

                            String item =
                                    "Check-in: " + convertIsoToDdMmYyyyHhMm(createdAt) + "\n" +
                                            "Participante: " + participantName + "\n" +
                                            "Evento: " + eventName;

                            checkinList.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(Checkins.this, "Erro ao processar dados: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                Toast.makeText(Checkins.this, "Erro " + statusCode + ": " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IOException e) {
                Toast.makeText(Checkins.this, "Falha na conexão: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Converte uma string de data e hora do formato ISO 8601 (e.g., "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
     * para o formato "dd/MM/yyyy HH:mm" no fuso horário local do dispositivo.
     *
     * @param isoDateString A string de data e hora no formato ISO 8601.
     * @return A string de data e hora formatada como "dd/MM/yyyy HH:mm", ou null se o formato de entrada for inválido.
     */
    public static String convertIsoToDdMmYyyyHhMm(String isoDateString) {
        // Define o formato de entrada para ISO 8601 com milissegundos e 'Z' para UTC
        SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
        inputFormatter.setTimeZone(TimeZone.getTimeZone("UTC")); // Indica que a string de entrada está em UTC

        Date date;
        try {
            // Parse a String de entrada para um objeto Date
            date = inputFormatter.parse(isoDateString);
        } catch (ParseException e) {
            System.err.println("Erro ao parsear a data ISO: " + isoDateString + ". Erro: " + e.getMessage());
            return null;
        }

        // Define o formato de saída para o fuso horário local do dispositivo
        SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        // Não é necessário definir o fuso horário para o outputFormatter, ele usa o default do dispositivo

        // Formate o objeto Date para o String desejado
        return outputFormatter.format(date);
    }
}