package com.archtranslator.Utils;

import net.runelite.api.MenuEntry;
import net.runelite.api.Client;

import javax.inject.Inject;

public class Utils {

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
}
