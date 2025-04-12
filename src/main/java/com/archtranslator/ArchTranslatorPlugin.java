package com.archtranslator;

import com.archtranslator.Utils.Utils;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.MenuEntry;
import net.runelite.client.eventbus.EventBus;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "ArchTranslator"
)
public class ArchTranslatorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private ArchTranslatorConfig config;

	@Inject
	private Utils utils;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
		//eventBus.register(this);

		//TranslatorApi translator = new TranslatorApi();

		System.out.println("--ATTEMPTING TRANSLATION--");
		//test.archTranslate("Examine", "bs", (translated) -> {System.out.println(translated);});
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		//eventBus.unregister(this);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened menuOpened)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "--MENU OPENED--", null);
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		List<CompletableFuture<MenuEntry>> futureEntriesOption = new ArrayList<>();

        for (MenuEntry entry : entries) {
			String option = entry.getOption();
			String target = entry.getTarget();
			String optionAndOrTarget;
			CompletableFuture<MenuEntry> asyncEntry = new CompletableFuture<>();

			TranslatorApi.archTranslateMyMem(option, "bs", (translated) ->{
				clientThread.invokeLater(() -> {
					log.info("Translating: " + option + " -> " + translated);
					MenuEntry copiedEntry = utils.copyEntry(entry);
					copiedEntry.setOption(translated);

					asyncEntry.complete(copiedEntry);
				});

			});

			/*asyncEntry = asyncEntry.exceptionally(ex ->{
				log.warn("Translation failed for: " + option);
				return null;
			});*/
			//This code is just the above code, while adding the entry, cant use the above code because
			//then asyncEntry is not final
			futureEntriesOption.add(asyncEntry.exceptionally(ex ->{
				log.warn("Translation failed for: " + option);
				return null;
			}));
        }

		CompletableFuture
				.allOf(futureEntriesOption.toArray(new CompletableFuture[0]))
				.thenRunAsync(() -> {
					List<MenuEntry> translated = futureEntriesOption.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());

					MenuEntry[] translatedEntries = translated.toArray(new MenuEntry[0]);

					clientThread.invokeLater(() -> {
						client.getMenu().setMenuEntries(translatedEntries);
					});
				});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Provides
	ArchTranslatorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArchTranslatorConfig.class);
	}
}
