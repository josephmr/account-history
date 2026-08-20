package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j(topic = "maxcape.CollectionLogTracker")
class CollectionLogTracker extends BaseTracker
{
	private static final Pattern COLLECTION_LOG_PATTERN =
		Pattern.compile("New item added to your collection log: (.+)", Pattern.CASE_INSENSITIVE);

	CollectionLogTracker(AccountHistoryPlugin plugin, OkHttpClient httpClient, Gson gson)
	{
		super(plugin, httpClient, gson);
	}

	@Override
	void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String message = stripTags(event.getMessage());
		Matcher matcher = COLLECTION_LOG_PATTERN.matcher(message);
		if (matcher.matches())
		{
			String itemName = matcher.group(1);
			log.debug("Collection log item: {}", itemName);
			sendEvent("COLLECTION_LOG", Map.of("itemName", itemName));
		}
	}
}
