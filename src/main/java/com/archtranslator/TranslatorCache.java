package com.archtranslator;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Function;
import net.runelite.client.RuneLite;

@Slf4j
public class TranslatorCache
{
	private static final Map<String, Map<String, CompletableFuture<String>>> parentCache = new ConcurrentHashMap<>();

	public static CompletableFuture<String> cacheCheckOrTranslate(String sourceString, String desiredLang, Function<String, CompletableFuture<String>> check)
	{
		Map<String, CompletableFuture<String>> cache = parentCache.computeIfAbsent(desiredLang, key -> new ConcurrentHashMap<>());
		return cache.computeIfAbsent(sourceString, check);
	}

	private static Map<String, Map<String, String>> prepHashMapForWrite()
	{
		Map<String, Map<String, String>> convertedParentMap = new HashMap<>();


		parentCache.forEach((parentKey, parentValue) ->
		{
			Map<String, String> convertedChildMap = new HashMap<>();
			parentValue.forEach((key, value) ->
			{
				try
				{
					if (value.isDone())
					{
						convertedChildMap.put(key, value.get());
					}
				}
				catch (Exception ex)
				{
					log.warn("Exception while converting hash map to str str: ", ex);
				}
			});
			convertedParentMap.put(parentKey, convertedChildMap);
		});
		return convertedParentMap;
	}

	public static void saveMapToFile()
	{
		Map<String, Map<String, String>> toWrite = prepHashMapForWrite();


		File f = new File(RuneLite.CACHE_DIR, "ArchTranslatorCache.dat");
		if (f.isFile())
		{
			try (Writer writer = new FileWriter(f))
			{
				Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
				gson.toJson(toWrite, writer);
			}
			catch (Exception ex)
			{
				log.warn("Error writing ArchTranslator Cache", ex);
			}
		}
		else
		{
			try
			{
				log.info("ArchTranslatorCache.dat does not exist... creating");
				f.createNewFile();
				saveMapToFile();
			}
			catch (Exception ex)
			{
				log.warn("Cannot create ArchTranslator cache file!", ex);
			}
		}
	}

	private static Map<String, Map<String, String>> loadCacheFromFile()
	{
		System.out.println("Attemping to load cache from disk");
		Gson gson = new Gson();
		File f = new File(RuneLite.CACHE_DIR, "ArchTranslatorCache.dat");
		Type type = new TypeToken<Map<String, Map<String, String>>>()
		{
		}.getType();

		try (Reader reader = new FileReader(f))
		{
			System.out.println("Cache loading from disk successful");
			return gson.fromJson(reader, type);
		}
		catch (Exception ex)
		{
			log.warn("Could not load cache file: ", ex);
		}
		return null;
	}

	public static void loadCacheIntoMemory()
	{
		Map<String, Map<String, String>> rawCache = loadCacheFromFile();
		if (rawCache != null)
		{
			Map<String, Map<String, CompletableFuture<String>>> preppedCache = prepCacheForLoad(rawCache);
			System.out.println(preppedCache);
			parentCache.putAll(preppedCache);
			log.info("Cache from disk loaded into memory");
			System.out.println(parentCache);
		}
	}

	public static Map<String, Map<String, CompletableFuture<String>>> prepCacheForLoad(Map<String, Map<String, String>> toPrep)
	{
		Map<String, Map<String, CompletableFuture<String>>> parentToLoad = new ConcurrentHashMap<>();

		toPrep.forEach((parentKey, parentValue) ->
		{
			Map<String, CompletableFuture<String>> childToLoad = new ConcurrentHashMap<>();

			parentValue.forEach((childKey, childValue) ->
			{
				childToLoad.putIfAbsent(childKey, CompletableFuture.completedFuture(childValue));
			});
			parentToLoad.putIfAbsent(parentKey, childToLoad);
		});
		return parentToLoad;
	}
}
