package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
abstract class BaseTracker
{
	private static final String API_URL = "https://maxcape.net/api/events";
	private static final MediaType JSON = MediaType.parse("application/json");

	private final AccountHistoryPlugin plugin;
	private final AccountHistoryConfig config;
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final EventBatcher batcher;

	BaseTracker(AccountHistoryPlugin plugin, AccountHistoryConfig config,
				OkHttpClient httpClient, Gson gson, File storeFile)
	{
		this.plugin = plugin;
		this.config = config;
		this.httpClient = httpClient;
		this.gson = gson;
		this.batcher = new EventBatcher(storeFile, gson);
	}

	protected final void sendEvent(String type, Object data)
	{
		if (!config.sendEvents())
		{
			return;
		}
		if (plugin.isRestrictedWorld())
		{
			return;
		}
		long accountHash = plugin.getCachedAccountHash();
		if (accountHash == 0)
		{
			log.debug("sendEvent: no account hash available, dropping {} event", type);
			return;
		}
		String playerName = plugin.getPlayerName();
		if (playerName == null)
		{
			log.debug("sendEvent: no player name available, dropping {} event", type);
			return;
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", type);
		payload.put("accountHash", String.valueOf(accountHash));
		payload.put("playerName", playerName);
		payload.put("timestamp", Instant.now().toString());
		payload.put("data", data);

		String json = gson.toJson(payload);
		Request request = new Request.Builder()
			.url(API_URL)
			.post(RequestBody.create(JSON, json))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to send {} event: {}", type, e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	protected final void batchEvent(String key, String type, Map<String, Object> data)
	{
		if (!config.sendEvents())
		{
			return;
		}
		if (plugin.isRestrictedWorld())
		{
			return;
		}
		batcher.record(key, type, data);
	}

	protected final List<EventBatcher.PendingBatch> drainBatch()
	{
		return batcher.drain();
	}

	void flush()
	{
		drainBatch().forEach(batch -> sendEvent(batch.getEventType(), batch.getExtraData()));
	}

	void loadBatch()
	{
		batcher.load();
	}

	void onGameStateChanged(GameStateChanged event) {}
	void onStatChanged(StatChanged event) {}
	void onChatMessage(ChatMessage event) {}
	void onGameTick(GameTick event) {}
}
