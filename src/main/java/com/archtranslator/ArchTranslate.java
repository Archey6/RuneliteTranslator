package com.archtranslator;

import com.archtranslator.Utils.Utils;
import java.util.concurrent.CompletableFuture;

public class ArchTranslate
{
	public static CompletableFuture<String> translate(String sourceString, String desiredLang)
	{
		return TranslatorCache.cacheCheckOrTranslate(sourceString, desiredLang, key ->
		{
			CompletableFuture<String> f = new CompletableFuture<>();
			TranslatorApi.archTranslateMyMem(key, desiredLang, result -> f.complete(Utils.dStrip(result)));
			return f;
		});
	}
}
