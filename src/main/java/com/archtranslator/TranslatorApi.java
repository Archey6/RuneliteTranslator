package com.archtranslator;

import lombok.extern.slf4j.Slf4j;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


import javax.inject.Inject;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

@Slf4j
public class TranslatorApi
{
	@Inject
	private static ArchTranslatorConfig config;

	private static String email;

	public static void init(ArchTranslatorConfig cfg)
	{
		config = cfg;
		email = config.cfgEmail();
	}

	public static void archTranslateMyMem(String sourceString, String desiredLang, Consumer<String> callback)
	{
		String queryUrl = "https://api.mymemory.translated.net/get?q=%s&langpair=%s";
		String tempUrl = String.format(queryUrl, URLEncoder.encode(sourceString), URLEncoder.encode("en|" + desiredLang));

		if (!email.isEmpty() && email != null)
		{
			queryUrl = queryUrl + "&de=" + URLEncoder.encode(email);
		}

		HttpClient client = HttpClient.newHttpClient();
		Gson gson = new Gson();

		client.sendAsync(
				HttpRequest.newBuilder(URI.create(tempUrl)).GET().build(),
				HttpResponse.BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenApply(json -> gson.fromJson(json, JsonObject.class))
			.thenApply(TranslatorApi::parseJson)
			.thenAccept(callback)
			.exceptionally(ex ->
			{
				return null;
			});
	}

	private static String parseJson(JsonObject json)
	{
		return json.getAsJsonObject("responseData").get("translatedText").getAsString();
	}
}
