package com.maxcape.accounthistory;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
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
		String area = m.group("area");
		String difficulty = m.group("difficulty");
		log.debug("Achievement diary completed: {} {}", difficulty, area);
		plugin.sendEvent("ACHIEVEMENT_DIARY", Map.of("area", area, "difficulty", difficulty));
	}
}
