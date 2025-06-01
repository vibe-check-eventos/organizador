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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

        Executor mainExecutor = this::runOnUiThread;
        apiService = new ApiService(mainExecutor);

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

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject checkinObj = jsonArray.getJSONObject(i);
                        String createdAt = checkinObj.getString("created_at");

                        JSONObject registrationObj = checkinObj.getJSONObject("registration");
                        JSONObject participantObj = registrationObj.getJSONObject("participant");
                        String participantName = participantObj.getString("name");

                        JSONObject eventObj = registrationObj.getJSONObject("event");
                        String eventName = eventObj.getString("name");

                        String item =
                                "Check-in: " + convertIsoToDdMmYyyyHhSs(createdAt) + "\n" +
                                "Participante: " + participantName + "\n" +
                                "Evento: " + eventName;

                        checkinList.add(item);
                    }

                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(Checkins.this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
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

    public static String convertIsoToDdMmYyyyHhSs(String isoDateTime) {
        // 1. Parse o String ISO 8601 para um Instant
        Instant instant = Instant.parse(isoDateTime);

        // 2. Converta o Instant para LocalDateTime no fuso horário desejado.
        //    Se você quiser o horário local do dispositivo, use ZoneId.systemDefault().
        //    Se você quiser uma representação no fuso horário de São Paulo, use "America/Sao_Paulo".
        ZoneId zoneId = ZoneId.of("America/Sao_Paulo"); // Ou ZoneId.systemDefault();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);

        // 3. Defina o formato de saída
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"); // Note HH para 24h, hh para 12h AM/PM

        // 4. Formate o LocalDateTime para o String desejado
        return localDateTime.format(formatter);
    }
}
