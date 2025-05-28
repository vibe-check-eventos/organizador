// src/main/java/com/vibecheck/organizer/network/ApiService.java
package com.vibecheck.organizer.network;

import java.io.IOException;
import java.util.concurrent.Executor; // Para gerenciar a thread do callback

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class ApiService {

    // Interface para o callback de resposta (adaptei do exemplo GenericHttpClient)
    public interface ApiResponseCallback {
        void onSuccess(String responseBody);
        void onError(int statusCode, String errorMessage);
        void onFailure(IOException e); // Para erros de rede (ex: sem internet)
    }

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8"); // Melhor especificar o charset

    private final OkHttpClient client;
    private final Executor callbackExecutor; // Executor para retornar o resultado na thread correta (UI Thread em Android)

    // Construtor: você deve passar um Executor que sabe como voltar para a UI thread
    public ApiService(Executor callbackExecutor) {
        this.client = new OkHttpClient();
        this.callbackExecutor = callbackExecutor;
    }

    // Método assíncrono para POST
    public void post(String url, String json, ApiResponseCallback callback) {
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Erro de rede (conexão, timeout, etc.)
                callbackExecutor.execute(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) { // try-with-resources para garantir que o Response seja fechado
                    String responseBody = null;
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }

                    if (response.isSuccessful()) {
                        // Requisição bem-sucedida (código 2xx)
                        final String finalResponseBody = responseBody; // Variável final para o lambda
                        callbackExecutor.execute(() -> callback.onSuccess(finalResponseBody != null ? finalResponseBody : ""));
                    } else {
                        // Requisição não bem-sucedida (códigos 4xx, 5xx)
                        final String finalResponseBody = responseBody; // Variável final para o lambda
                        callbackExecutor.execute(() -> callback.onError(response.code(), finalResponseBody != null ? finalResponseBody : response.message()));
                    }
                } catch (IOException e) {
                    // Erro ao ler o corpo da resposta
                    callbackExecutor.execute(() -> callback.onFailure(e));
                }
            }
        });
    }

    // Você pode adicionar um método GET assíncrono também
    public void get(String url, ApiResponseCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackExecutor.execute(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = null;
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }

                    if (response.isSuccessful()) {
                        final String finalResponseBody = responseBody;
                        callbackExecutor.execute(() -> callback.onSuccess(finalResponseBody != null ? finalResponseBody : ""));
                    } else {
                        final String finalResponseBody = responseBody;
                        callbackExecutor.execute(() -> callback.onError(response.code(), finalResponseBody != null ? finalResponseBody : response.message()));
                    }
                } catch (IOException e) {
                    callbackExecutor.execute(() -> callback.onFailure(e));
                }
            }
        });
    }

    // Método assíncrono para PUT
    public void put(String url, String json, ApiResponseCallback callback) {
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackExecutor.execute(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = null;
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }

                    if (response.isSuccessful()) {
                        final String finalResponseBody = responseBody;
                        callbackExecutor.execute(() -> callback.onSuccess(finalResponseBody != null ? finalResponseBody : ""));
                    } else {
                        final String finalResponseBody = responseBody;
                        callbackExecutor.execute(() -> callback.onError(response.code(), finalResponseBody != null ? finalResponseBody : response.message()));
                    }
                } catch (IOException e) {
                    callbackExecutor.execute(() -> callback.onFailure(e));
                }
            }
        });
    }

}