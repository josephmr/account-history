package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class BossKillTracker extends BaseTracker
{
	private static final Pattern KILL_COUNT_PATTERN =
		Pattern.compile("Your (.+) kill count is: ([\\d,]+)\\.");
	private static final int FLUSH_TICKS = 500;

	private int tickCount;

	BossKillTracker(AccountHistoryPlugin plugin, AccountHistoryConfig config,
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
		batchEvent(bossName, "BOSS_KILL", Map.of("bossName", bossName, "totalKc", totalKc));
	}

	@Override
	void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			tickCount = 0;
		}
	}

	@Override
	void onGameTick(GameTick event)
	{
		if (++tickCount >= FLUSH_TICKS)
		{
			tickCount = 0;
			flush();
		}
	}

	@Override
	void flush()
	{
		drainBatch().forEach(batch ->
		{
			Map<String, Object> data = new LinkedHashMap<>(batch.getExtraData());
			data.put("kills", batch.getCount());
			data.put("periodStart", batch.getFirstAt());
			data.put("periodEnd", batch.getLastAt());
			log.debug("Boss kills sent: boss='{}' kills={} totalKc={}",
				data.get("bossName"), data.get("kills"), data.get("totalKc"));
			sendEvent(batch.getEventType(), data);
		});
	}
}
