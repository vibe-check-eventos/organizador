package com.vibecheck.organizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Profile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar TextViews
        TextView tvFullName = findViewById(R.id.tvFullName);
        TextView tvUpdatedAt = findViewById(R.id.tvEmail);
        TextView tvDocument = findViewById(R.id.tvDocument);
        TextView tvOrganizerType = findViewById(R.id.tvOrganizerType);
        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvId = findViewById(R.id.tvId);

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        // Preencher os TextViews com os dados EXISTENTES
        tvId.setText(Long.toString(prefs.getLong("id", -1L)));
        tvFullName.setText(prefs.getString("full_name", "Não disponível"));
        tvUpdatedAt.setText(prefs.getString("updated_at", "Não disponível"));
        tvDocument.setText(prefs.getInt("organizer_type", 0) == 1 ? prefs.getString("cpf", "Não disponível") : prefs.getString("cnpj", "Não disponível"));
        tvOrganizerType.setText((prefs.getInt("organizer_type", 0) == 1 ? "Pessoa Física" : "Pessoa Jurídica"));
        tvCreatedAt.setText(convertIsoToDdMmYyyyHhSs(prefs.getString("created_at", "1970-01-01T00:00:00.000Z")));
        tvEmail.setText(prefs.getString("email", "Não disponível"));


    }

    public void signOut(View view){

        clearAllUserData(Profile.this);

        Intent intent = new Intent(this, MainActivity.class);
        // Use FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK to prevent going back
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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