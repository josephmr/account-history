package com.maxcape.accounthistory;

import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DiaryTracker
{
	private static final Pattern DIARY_PATTERN = Pattern.compile(
		"Congratulations! You have completed all of the (?<difficulty>\\w+) tasks in the (?<area>.+) area\\."
	);

	private final AccountHistoryPlugin plugin;
	private final AccountHistoryConfig config;

	DiaryTracker(AccountHistoryPlugin plugin, AccountHistoryConfig config)
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
		Matcher m = DIARY_PATTERN.matcher(event.getMessage());
		if (!m.matches())
		{
			return;
		}
		plugin.sendEvent("ACHIEVEMENT_DIARY", Map.of(
			"area", m.group("area"),
			"difficulty", m.group("difficulty")
		));
	}
}
