package com.vibecheck.organizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QrCodeData {

    @JsonProperty("id")
    public int id;

    @JsonProperty("participant_id")
    public int participantId;

    @JsonProperty("event_id")
    public int eventId;

    @JsonProperty("qr_code_base64")
    public String qrCodeBase64;

    @JsonProperty("date")
    public String createdAt;

    @JsonProperty("updated_at")
    public String updatedAt;

    // Construtor vazio para o Jackson
    public QrCodeData() {
    }
}
