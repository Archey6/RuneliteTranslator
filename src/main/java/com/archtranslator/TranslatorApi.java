package com.archtranslator;

import lombok.extern.slf4j.Slf4j;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.translate.TranslateAsyncClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.io.StringReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class TranslatorApi {

    private static String queryUrl = "https://api.mymemory.translated.net/get?q=%s&langpair=%s&de=%s";
    private static String email = "archeysgames@gmail.com";
    //private static String queryUrl = "";

    public static void archTranslateMyMem(String sourceString, String desiredLang, Consumer<String> callback)
    {
        String tempUrl = String.format(queryUrl, URLEncoder.encode(sourceString), URLEncoder.encode("en|"+desiredLang), URLEncoder.encode(email));

        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();

        client.sendAsync(
                        HttpRequest.newBuilder(URI.create(tempUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> gson.fromJson(json, JsonObject.class))
                .thenApply(TranslatorApi::parseJson)
                .thenAccept(callback)
                .exceptionally(ex ->{
                    log.warn("Translation failed for: " + sourceString, ex);
                    return null;
                });
    }

    private static String parseJson(JsonObject json)
    {
        return json.getAsJsonObject("responseData").get("translatedText").getAsString();
    }

    private static final TranslateAsyncClient translateClient = TranslateAsyncClient.builder()
            .region(Region.US_EAST_2) // choose your AWS region
            .build();

    public static void archTranslateAmazon(String sourceText, String targetLang, Consumer<String> callback)
    {
        TranslateTextRequest request = TranslateTextRequest.builder()
                .text(sourceText)
                .sourceLanguageCode("en") // or autodetect with "auto"
                .targetLanguageCode(targetLang)
                .build();

        CompletableFuture<TranslateTextResponse> future = translateClient.translateText(request);

        future.thenAccept(response -> {
            String translatedText = response.translatedText();
            callback.accept(translatedText);
        }).exceptionally(ex -> {
            ex.printStackTrace(); // or log error
            callback.accept("[Translation failed]");
            return null;
        });
    }
}
