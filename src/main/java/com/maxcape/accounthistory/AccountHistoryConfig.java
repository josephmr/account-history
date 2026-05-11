package com.maxcape.accounthistory;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("account-history")
public interface AccountHistoryConfig extends Config
{
	@ConfigItem(
		keyName = "apiUrl",
		name = "API URL",
		description = "Base URL of the account history server",
		position = 1
	)
	default String apiUrl()
	{
		return "http://localhost:5173";
	}

	@ConfigItem(
		keyName = "sendEvents",
		name = "Send events to server",
		description = "Send skill level ups and collection log events to the account history server",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 2
	)
	default boolean sendEvents()
	{
		return false;
	}
}
