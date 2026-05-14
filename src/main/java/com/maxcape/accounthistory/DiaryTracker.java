package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class DiaryTracker extends BaseTracker
{
	private static final Pattern DIARY_PATTERN = Pattern.compile(
		"Congratulations! You have completed all of the (?<difficulty>\\w+) tasks in the (?<area>.+) area\\."
	);

	DiaryTracker(AccountHistoryPlugin plugin, AccountHistoryConfig config,
				 OkHttpClient httpClient, Gson gson, File storeFile)
	{
		super(plugin, config, httpClient, gson, storeFile);
	}

	@Override
	void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
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
		sendEvent("ACHIEVEMENT_DIARY", Map.of("area", area, "difficulty", difficulty));
	}
}
