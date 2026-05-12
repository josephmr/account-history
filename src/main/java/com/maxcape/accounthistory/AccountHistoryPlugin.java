package com.maxcape.accounthistory;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "Account History",
	description = "Tracks skill level ups, collection log entries, boss kills, and achievement diaries",
	tags = {"skill", "collection log", "tracker", "history", "boss", "diary"}
)
public class AccountHistoryPlugin extends Plugin
{
	private static final String API_URL = "https://maxcape.net/api/events";
	private static final MediaType JSON = MediaType.parse("application/json");
	private static final int FLUSH_TICKS = 500; // ~5 minutes

	@Inject
	private Client client;

	@Inject
	private AccountHistoryConfig config;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	private LevelUpTracker levelUpTracker;
	private CollectionLogTracker collectionLogTracker;
	private BossKillTracker bossKillTracker;
	private DiaryTracker diaryTracker;
	private int tickCount;

	@Override
	protected void startUp()
	{
		File storeFile = new File(new File(RuneLite.RUNELITE_DIR, "account-history"), "pending-events.json");
		EventBatcher batcher = new EventBatcher(storeFile, gson);
		batcher.load();

		levelUpTracker      = new LevelUpTracker(client, this, config);
		collectionLogTracker = new CollectionLogTracker(this, config);
		bossKillTracker     = new BossKillTracker(batcher, this, config);
		diaryTracker        = new DiaryTracker(this, config);
		tickCount = 0;

		log.debug("Account History started");
	}

	@Override
	protected void shutDown()
	{
		if (bossKillTracker != null)
		{
			bossKillTracker.flush();
		}
		levelUpTracker      = null;
		collectionLogTracker = null;
		bossKillTracker     = null;
		diaryTracker        = null;
		tickCount = 0;
		log.debug("Account History stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			if (levelUpTracker != null)
			{
				levelUpTracker.onLoggedIn();
			}
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			if (bossKillTracker != null)
			{
				bossKillTracker.flush();
			}
			if (levelUpTracker != null)
			{
				levelUpTracker.onLoggedOut();
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (levelUpTracker != null)
		{
			levelUpTracker.onStatChanged(event);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		if (collectionLogTracker != null)
		{
			collectionLogTracker.onChatMessage(event);
		}
		if (bossKillTracker != null)
		{
			bossKillTracker.onChatMessage(event);
		}
		if (diaryTracker != null)
		{
			diaryTracker.onChatMessage(event);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (++tickCount >= FLUSH_TICKS)
		{
			tickCount = 0;
			if (bossKillTracker != null)
			{
				bossKillTracker.flush();
			}
		}
	}

	void sendEvent(String type, Object data)
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", type);
		payload.put("playerName", client.getLocalPlayer().getName());
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

	@Provides
	AccountHistoryConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccountHistoryConfig.class);
	}
}
