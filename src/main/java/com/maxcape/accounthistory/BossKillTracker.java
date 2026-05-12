package com.maxcape.accounthistory;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class BossKillTracker
{
	private static final Pattern KILL_COUNT_PATTERN =
		Pattern.compile("Your (.+) kill count is: ([\\d,]+)\\.");

	private final EventBatcher batcher;
	private final AccountHistoryPlugin plugin;
	private final AccountHistoryConfig config;

	BossKillTracker(EventBatcher batcher, AccountHistoryPlugin plugin, AccountHistoryConfig config)
	{
		this.batcher = batcher;
		this.plugin = plugin;
		this.config = config;
	}

	void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		if (!config.sendEvents())
		{
			return;
		}

		String message = event.getMessage().replaceAll("<[^>]+>", "");

		if (!message.contains("kill count"))
		{
			return;
		}

		Matcher m = KILL_COUNT_PATTERN.matcher(message);
		if (!m.matches())
		{
			log.debug("Kill count message did not match pattern: '{}'", message);
			return;
		}

		String bossName = m.group(1);
		int totalKc = Integer.parseInt(m.group(2).replace(",", ""));
		log.debug("Boss kill recorded: boss='{}' totalKc={}", bossName, totalKc);
		batcher.record(bossName, "BOSS_KILL", Map.of("bossName", bossName, "totalKc", totalKc));
	}

	// Drain all pending batches and send. Not guarded by sendEvents() so kills
	// accumulated before a config toggle are not silently discarded on logout.
	void flush()
	{
		List<EventBatcher.PendingBatch> batches = batcher.drain();
		for (EventBatcher.PendingBatch batch : batches)
		{
			Map<String, Object> data = new LinkedHashMap<>(batch.getExtraData());
			data.put("kills", batch.getCount());
			data.put("periodStart", batch.getFirstAt());
			data.put("periodEnd", batch.getLastAt());
			log.debug("Boss kills sent: boss='{}' kills={} totalKc={}",
				data.get("bossName"), data.get("kills"), data.get("totalKc"));
			plugin.sendEvent(batch.getEventType(), data);
		}
	}
}
