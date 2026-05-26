package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j(topic = "maxcape.BaseTracker")
abstract class BaseTracker
{
	private static final String API_URL = "https://maxcape.net/api/events";
	private static final MediaType JSON = MediaType.parse("application/json");

	protected final AccountHistoryPlugin plugin;
	private final OkHttpClient httpClient;
	private final Gson gson;

	private boolean loggedIn = false;
	private boolean pendingLogin = false;

	private String lastKnownPlayerName;
	private long lastKnownAccountHash;

	BaseTracker(AccountHistoryPlugin plugin, OkHttpClient httpClient, Gson gson)
	{
		this.plugin = plugin;
		this.httpClient = httpClient;
		this.gson = gson;
	}

	protected final void sendEvent(String type, Object data)
	{
		if (plugin.isRestrictedWorld())
		{
			return;
		}
		long accountHash = plugin.getAccountHash();
		if (accountHash == 0)
		{
			accountHash = lastKnownAccountHash;
		}
		if (accountHash == 0)
		{
			log.debug("sendEvent: no account hash available, dropping {} event", type);
			return;
		}
		String playerName = plugin.getPlayerName();
		if (playerName == null)
		{
			playerName = lastKnownPlayerName;
		}
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

	void flush() {}

	void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				pendingLogin = true;
				if (loggedIn)
				{
					loggedIn = false;
					onLogout();
				}
				break;
			case LOGGED_IN:
				if (pendingLogin || !loggedIn)
				{
					pendingLogin = false;
					loggedIn = true;
					onLogin();
				}
				break;
			default:
				break;
		}
	}

	protected static String stripTags(String s)
	{
		return s.replaceAll("<[^>]+>", "");
	}

	void onLogin() {}
	void onLogout() {}
	void onStatChanged(StatChanged event) {}
	void onChatMessage(ChatMessage event) {}
	void onVarbitChanged(VarbitChanged event) {}
	void onGameTick(GameTick event) {}
}
