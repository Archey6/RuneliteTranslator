package com.archtranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

	public static CompletableFuture<String> getKey(String key)
	{
		return cache.get(key);
	}

	private static Map<String, String> convertHashMapToStrStr()
	{
		Map<String, String> convertedMap = new HashMap<>();
		cache.forEach((key, value) ->
		{
			try
			{
				if (value.isDone())
				{
					convertedMap.put(key, value.get());
				}
			}
			catch (Exception ex)
			{
				log.warn("Exception while converting hash map to str str: ", ex);
			}
		});
		return convertedMap;
	}

	public static void saveMapToFile()
	{
		Map<String, String> toSave = convertHashMapToStrStr();

		Properties prop = new Properties();
		prop.putAll(toSave);
		//prop.store(new FileOutputStream(f, true), null);

		File f = new File(RuneLite.CACHE_DIR, "ArchTranslatorCache.dat");
		if (f.isFile())
		{
			try
			{
				log.info("Attempting to save ArchTranslator cache");
				/*ObjectOutputStream writeCache = new ObjectOutputStream(new FileOutputStream(f, true));
				writeCache.writeObject(toSave);
				writeCache.close();*/
				prop.store(new FileOutputStream(f, true), null);
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
}
