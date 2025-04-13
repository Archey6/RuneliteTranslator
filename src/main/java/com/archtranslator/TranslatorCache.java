package com.archtranslator;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TranslatorCache
{
	private static final Map<String, CompletableFuture<String>> cache = new ConcurrentHashMap<>();

	public static CompletableFuture<String> cacheCheckOrTranslate(String sourceString, Function<String, CompletableFuture<String>> check)
	{
		return cache.computeIfAbsent(sourceString, check);
	}

	public static void clear()
	{
		cache.clear();
	}

	public static int size()
	{
		return cache.size();
	}

	public static void debugPrintCache()
	{
		System.out.println("🧠 Translation Cache Dump (" + cache.size() + " items):");
		cache.forEach((original, future) ->
		{
			boolean isDone = future.isDone();
			String result = isDone ? future.getNow("[pending]") : "[pending]";
			System.out.println("→ " + original + " = " + result + " | Done? " + isDone);
		});
	}
}
