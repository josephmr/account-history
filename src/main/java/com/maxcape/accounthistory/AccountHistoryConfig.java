package com.maxcape.accounthistory;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("account-history")
public interface AccountHistoryConfig extends Config
{
	@ConfigItem(
		keyName = "sendEvents",
		name = "Send events to maxcape.net",
		description = "Send skill level ups and collection log events to maxcape.net",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean sendEvents()
	{
		return false;
	}
}
