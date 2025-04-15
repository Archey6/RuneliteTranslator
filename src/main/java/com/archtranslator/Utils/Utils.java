package com.archtranslator.Utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Pattern;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;

@Slf4j
public class Utils
{
	private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	@Inject
	private Client client;

	public MenuEntry copyEntry(MenuEntry entry)
	{
		MenuEntry newEntry = client.createMenuEntry(-1);
		newEntry.setOption(entry.getOption());
		newEntry.setTarget(entry.getTarget());
		newEntry.setType(entry.getType());
		newEntry.setIdentifier(entry.getIdentifier());
		newEntry.setParam0(entry.getParam0());
		newEntry.setParam1(entry.getParam1());
		newEntry.setItemId(entry.getItemId());
		return newEntry;
	}

	public static String dStrip(String input)
	{
		/*osros font bitmap doesnt support diacritics so we need to strip them. jagex pls fix*/
		if (input == null)
		{
			return "";
		}

		// Normalize to decomposed form: e.g. "č" → "c + ̌"
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

		// Remove all combining diacritic marks
		return DIACRITICS.matcher(normalized).replaceAll("");
	}

	public static Map<String, String> loadLanguages()
	{
		Gson gson = new Gson();
		InputStream stream = Utils.class.getResourceAsStream("/languages.json");
		Map<String, String> langMap = new HashMap<>();

		if (stream == null)
		{
			throw new RuntimeException("languages.json not found in resources");
		}

		InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

		Type type = new TypeToken<List<JsonObject>>()
		{
		}.getType();

		List<JsonObject> jsonData = gson.fromJson(reader, type);

		for (JsonObject element : jsonData)
		{
			langMap.put(element.get("code").toString(), element.get("name").toString());
		}
		return langMap;
	}

	//public static mapToEnum
}
