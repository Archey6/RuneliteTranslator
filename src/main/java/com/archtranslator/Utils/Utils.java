package com.archtranslator.Utils;

import java.text.Normalizer;
import java.util.regex.Pattern;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;

import javax.inject.Inject;

public class Utils {
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
        if (input == null) return "";

        // Normalize to decomposed form: e.g. "č" → "c + ̌"
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Remove all combining diacritic marks
        return DIACRITICS.matcher(normalized).replaceAll("");
    }
}
