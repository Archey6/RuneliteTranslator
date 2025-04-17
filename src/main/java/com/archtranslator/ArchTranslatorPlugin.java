package com.archtranslator;

import com.archtranslator.Utils.Utils;

import com.google.inject.Provides;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import com.archtranslator.ArchTranslate;

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
	private int lastTranslatedDialogId = -1;

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
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		List<CompletableFuture<MenuEntry>> futureEntries = new ArrayList<>();
		CompletableFuture<String> optionFuture = new CompletableFuture<>();
		CompletableFuture<String> targetFuture = new CompletableFuture<>();

		for (MenuEntry entry : entries)
		{
			String option = entry.getOption();
			String target = entry.getTarget();
			Player player = entry.getPlayer();

			optionFuture = ArchTranslate.translate(option, desiredLang);
			targetFuture = !target.isEmpty() && player == null
				? ArchTranslate.translate(target, desiredLang)
				: CompletableFuture.completedFuture("");

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
	public void onChatMessage(ChatMessage message)
	{
		if (message.getType() == ChatMessageType.DIALOG && !message.getName().startsWith("[TRANSLATED]"))
		{
			//CompletableFuture<String> dialogFuture = new CompletableFuture<>();
			String npcMessage = message.getMessage();


			//CompletableFuture<String> finalDialogFuture = dialogFuture;
			clientThread.invokeLater(() ->
			{
				CompletableFuture<String> dialogFuture = ArchTranslate.translate(npcMessage, desiredLang);
				String[] translatedMsg = null;
				try
				{
					translatedMsg = dialogFuture.get().split("\\|", 3);
				}
				catch (InterruptedException | ExecutionException e)
				{
					throw new RuntimeException(e);
				}
				String newName = translatedMsg[0].trim();
				String newDialog = translatedMsg.length > 1 ? translatedMsg[1].trim() : "";

				Widget npcDialog = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
				Widget npcName = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
				Widget player = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);


				if (npcDialog != null)
				{
					npcDialog.setText(newDialog);

				}

				if (player != null)
				{
					player.setText(newDialog);
				}

				if (npcName != null)
				{
					npcName.setText(newName);
				}

			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		Widget options = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);

		if (options != null && options.getChildren() != null)
		{
			Widget[] dialogOptions = options.getChildren();
			int currentDialogId = Arrays.hashCode(
				Arrays.stream(options.getChildren())
					.map(Widget::getText)
					.toArray());

			if (currentDialogId != lastTranslatedDialogId)
			{
				for (Widget child : dialogOptions)
				{
					String original = child.getText();

					ArchTranslate.translate(original, desiredLang).thenAcceptAsync(translatedChild ->
					{
						clientThread.invokeLater(() ->
						{
							child.setText(translatedChild);
						});
					});
				}
				log.info("Dialog options translated successfully");
				lastTranslatedDialogId = currentDialogId;
			}
		}
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
}
