package com.archtranslator;

import com.archtranslator.Utils.Utils;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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
	private ArchTranslatorConfig config;

	@Inject
	private Utils utils;

	private String desiredLang;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
		TranslatorApi.init(config);

		Languages lang = config.cfgLang();
		desiredLang = lang.getCode();
		log.info("Language code set to: " + desiredLang);

		TranslatorCache.loadCacheIntoMemory();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		TranslatorCache.saveMapToFile();
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown e)
	{
		log.info("Example stopped!");
		TranslatorCache.saveMapToFile();
	}

	@Subscribe
	public void onMenuOpened(MenuOpened menuOpened)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "--MENU OPENED--", null);
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		List<CompletableFuture<MenuEntry>> futureEntries = new ArrayList<>();

		for (MenuEntry entry : entries)
		{
			String option = entry.getOption();
			String target = entry.getTarget();
			Player player = entry.getPlayer();
			String playerName;
			final CompletableFuture<String> asyncOptionEntry = new CompletableFuture<>();
			final CompletableFuture<String> asyncTargetEntry = new CompletableFuture<>();
			CompletableFuture<String> targetFuture = new CompletableFuture<>();


			CompletableFuture<String> optionFuture = TranslatorCache.cacheCheckOrTranslate(option, desiredLang, checkedOption ->
			{
				TranslatorApi.archTranslateMyMem(checkedOption, desiredLang, translated ->
				{
					asyncOptionEntry.complete(Utils.dStrip(translated));
				});
				return asyncOptionEntry;
			});
			if (optionFuture.isDone())
			{
				System.out.println("♻️ Option cache HIT: " + option + " → " + optionFuture.getNow("[not done]"));
			}

			if (!target.isEmpty() && player == null)
			{
				targetFuture = TranslatorCache.cacheCheckOrTranslate(target, desiredLang, checkedTarget ->
				{
					TranslatorApi.archTranslateMyMem(checkedTarget, desiredLang, translated ->
					{
						asyncTargetEntry.complete(Utils.dStrip(translated));
					});
					return asyncTargetEntry;
				});
				if (targetFuture.isDone())
				{
					System.out.println("♻️ Target cache HIT: " + target + " → " + targetFuture.getNow("[not done]"));
				}
			}
			else
			{
				targetFuture.complete("");
			}

			CompletableFuture<MenuEntry> futureEntry = optionFuture.thenCombineAsync(targetFuture, (translatedOption, translatedTarget) ->
				{
					CompletableFuture<MenuEntry> safe = new CompletableFuture<>();

					clientThread.invokeLater(() ->
					{
						MenuEntry copiedEntry = utils.copyEntry(entry);
						copiedEntry.setOption(translatedOption);
						if (player == null)
						{
							copiedEntry.setTarget(translatedTarget); //only translate target if its not a player
						}
						safe.complete(copiedEntry);
					});
					return safe;
				})
					.thenCompose(Function.identity())
				.exceptionally(ex ->
				{
					log.warn("entryFuture failed", ex);
					return null;
				});

			futureEntries.add(futureEntry);
		}

		CompletableFuture
			.allOf(futureEntries.toArray(new CompletableFuture[0]))
			.thenRunAsync(() ->
			{
				List<MenuEntry> translated = futureEntries.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());

				MenuEntry[] translatedEntries = translated.toArray(new MenuEntry[0]);

				clientThread.invokeLater(() ->
				{
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

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals("ArchTranslator"))
		{
			return;
		}
		String key = configChanged.getKey();

		switch (key)
		{
			case "cfgEmail":
				configChanged.setNewValue(configChanged.getNewValue());
				break;

			case "cfgLang":
				Languages lang = config.cfgLang();
				desiredLang = lang.getCode();
				log.info("Language code set to: " + desiredLang);
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		/*CompletableFuture<String> item;
		item = TranslatorCache.getKey("Cancel");

		try
		{
			if (item.isDone())
			{
				System.out.println(item.get());
			}
		}
		catch (Exception ex)
		{
			System.out.println("item is null try caching something");
		}*/
		//System.out.println(client.getCac);
	}
}
