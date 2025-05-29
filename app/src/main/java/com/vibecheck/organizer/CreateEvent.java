package com.vibecheck.organizer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vibecheck.organizer.network.ApiService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CreateEvent extends AppCompatActivity {

    private EditText etNomeEvento;
    private EditText etDataEvento;
    private EditText etLocalEvento;
    private EditText etDescricao;

    private ApiService apiService;

    private static final String BASE_URL = "https://3e46-179-119-53-133.ngrok-free.app/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNomeEvento = findViewById(R.id.etNomeEvento);
        etDataEvento = findViewById(R.id.etDataEvento);
        etLocalEvento = findViewById(R.id.etLocalEvento);
        etDescricao = findViewById(R.id.etDescricao);

        this.apiService = new ApiService(ContextCompat.getMainExecutor(this));
    }

    public void criarEvento(View view) {
        // Recuperar o organizer_id do SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        long organizerId = sharedPreferences.getLong("id", -1);

        if (organizerId == -1) {
            Toast.makeText(this, "Organizer ID inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        String nomeEvento = etNomeEvento.getText().toString();
        String localEvento = etLocalEvento.getText().toString();
        String descricao = etDescricao.getText().toString();

        // Primeiro POST: event-addresses (apenas street)
        JSONObject enderecoJson = new JSONObject();
        try {
            enderecoJson.put("street", localEvento);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao criar JSON do endereço", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.post(BASE_URL + "/event-addresses", enderecoJson.toString(), new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    Log.d("Evento cadastrado", responseBody);
                    JSONObject responseJson = new JSONObject(responseBody);
                    String eventAddressId = String.valueOf(responseJson.getInt("id"));

                    Log.d("Id do endereço", String.valueOf(eventAddressId));

                    // Segundo POST: events
                    JSONObject eventoJson = new JSONObject();
                    eventoJson.put("organizer_id", organizerId);
                    eventoJson.put("name", nomeEvento);
                    eventoJson.put("description", descricao);
                    eventoJson.put("event_address_id", eventAddressId);
                    eventoJson.put("is_active", true);

                    apiService.post(BASE_URL + "/events", eventoJson.toString(), new ApiService.ApiResponseCallback() {
                        @Override
                        public void onSuccess(String responseBody) {
                            runOnUiThread(() ->
                                    Toast.makeText(CreateEvent.this, "Evento criado com sucesso!", Toast.LENGTH_LONG).show()
                            );
                        }

                        @Override
                        public void onError(int statusCode, String errorMessage) {
                            Log.d("CreateEvent", "Erro ao criar o evento: " + errorMessage);
                            runOnUiThread(() ->
                                    Toast.makeText(CreateEvent.this, "Erro ao criar evento: " + errorMessage, Toast.LENGTH_LONG).show()
                            );
                        }

                        @Override
                        public void onFailure(IOException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(CreateEvent.this, "Falha na rede ao criar evento", Toast.LENGTH_LONG).show()
                            );
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(CreateEvent.this, "Erro ao processar resposta do endereço", Toast.LENGTH_SHORT).show()
                    );
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(CreateEvent.this, "Erro ao criar endereço: " + errorMessage, Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onFailure(IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(CreateEvent.this, "Falha na rede ao criar endereço", Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}
