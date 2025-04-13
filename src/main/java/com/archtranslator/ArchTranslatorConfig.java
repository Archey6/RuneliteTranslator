package com.archtranslator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("ArchTranslator")
public interface ArchTranslatorConfig extends Config
{
	@ConfigSection(
		name = "Settings",
		description = "ArchTranslator Settings",
		position = 0
	)
	String cfgSettings = "cfgSettings";

	@ConfigItem(
		keyName = "cfgEmail",
		name = "E-mail Address",
		description = "Provide email to increase MyMemory quota",
		section = cfgSettings,
		secret = true,
		position = 1
	)
	default String cfgEmail()
	{
		return "";
	}

	default String greeting()
	{
		return "TRANSLATOR STARTED";
	}
}
