package com.github.ALEX_THE_NUGGET.bot_assistant.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;

public class YandexTranslate {
    private static final String TRANSLATE_URL = "https://translate.api.cloud.yandex.net/translate/v2/translate";

    private OkHttpClient client = new OkHttpClient();
    ;
    private String folderID = System.getenv("FOLDER_ID");
    private String IAMToken = System.getenv("IAM_TOKEN");

    public String translate(String text, String targetLanguage) throws IOException {
        String folderId = folderID;
        String iamToken = IAMToken;
        JSONArray texts = new JSONArray(Collections.singletonList(text));
        JSONObject body = new JSONObject()
                .put("targetLanguageCode", targetLanguage)
                .put("texts", texts)
                .put("folderId", folderId);
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.create(mediaType, body.toString());
        Request request = new Request.Builder()
                .url(TRANSLATE_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + iamToken)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray translations = jsonResponse.getJSONArray("translations");
                if (translations.length() > 0) {
                    JSONObject translationObject = translations.getJSONObject(0);
                    String translatedText = translationObject.getString("text");
                    return translatedText;
                } else {
                    throw new IOException("No translations found in the response");
                }
            } else {
                throw new IOException("Error: " + response.code() + " - " + response.message());
            }
        }
    }
}