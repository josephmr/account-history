package com.maxcape.accounthistory;

import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CollectionLogTracker
{
	private static final Pattern COLLECTION_LOG_PATTERN =
		Pattern.compile("New item added to your collection log: (.+)");

	private final AccountHistoryPlugin plugin;
	private final AccountHistoryConfig config;

	CollectionLogTracker(AccountHistoryPlugin plugin, AccountHistoryConfig config)
	{
		this.plugin = plugin;
		this.config = config;
	}

	void onChatMessage(ChatMessage event)
	{
		if (!config.sendEvents() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		Matcher matcher = COLLECTION_LOG_PATTERN.matcher(event.getMessage());
		if (matcher.matches())
		{
			plugin.sendEvent("COLLECTION_LOG", Map.of("itemName", matcher.group(1)));
		}
	}
}
