package com.vibecheck.organizer;

import android.app.DatePickerDialog;
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
    private EditText etDataEvento, etHoraEvento;
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
        etHoraEvento = findViewById(R.id.etHoraEvento);
        etLocalEvento = findViewById(R.id.etLocalEvento);
        etDescricao = findViewById(R.id.etDescricao);

        this.apiService = new ApiService(ContextCompat.getMainExecutor(this));

        //para qnd o usuario clicar no campo de data abrir um teclado que faça sentido...
        etDataEvento.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            int year = c.get(java.util.Calendar.YEAR);
            int month = c.get(java.util.Calendar.MONTH);
            int day = c.get(java.util.Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(CreateEvent.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String dataFormatada = String.format("%02d/%02d/%04d", dayOfMonth, (monthOfYear + 1), year1);
                        etDataEvento.setText(dataFormatada);
                    }, year, month, day);

            // Impede selecionar data inferior a hoje
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

            datePickerDialog.show();
        });
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

        String dataEvento = etDataEvento.getText().toString();  // exemplo: "31/05/2025"
        String horaEvento = etHoraEvento.getText().toString();  // exemplo: "14:30"

        // Mesclar os dois
        String dataHora = dataEvento + " " + horaEvento;  // "31/05/2025 14:30"

        // Converter para "yyyy-MM-dd HH:mm:ss"
        String dataHoraConvertida;
        java.util.Date dateEvento;

        try {
            java.text.SimpleDateFormat formatoEntrada = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            java.text.SimpleDateFormat formatoSaida = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formatoEntrada.setLenient(false);

            dateEvento = formatoEntrada.parse(dataHora);
            dataHoraConvertida = formatoSaida.format(dateEvento);

            // Validação: data e hora não podem ser anteriores a agora
            java.util.Date agora = new java.util.Date();
            if (dateEvento.before(agora)) {
                Toast.makeText(this, "A data e hora não podem ser anteriores ao momento atual", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Data ou hora inválida", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    eventoJson.put("created_at", dataHoraConvertida);

                    apiService.post(BASE_URL + "/events", eventoJson.toString(), new ApiService.ApiResponseCallback() {
                        @Override
                        public void onSuccess(String responseBody) {
                            runOnUiThread(() ->
                                    Toast.makeText(CreateEvent.this, "Evento criado com sucesso!", Toast.LENGTH_LONG).show()
                            );
                            finish();
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
