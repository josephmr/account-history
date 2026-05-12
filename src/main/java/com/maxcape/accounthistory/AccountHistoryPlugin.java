package com.maxcape.accounthistory;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
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
	description = "Tracks skill level ups and collection log entries",
	tags = {"skill", "collection log", "tracker", "history"}
)
public class AccountHistoryPlugin extends Plugin
{
	private static final String API_URL = "https://maxcape.net/api/events";
	private static final MediaType JSON = MediaType.parse("application/json");
	private static final Pattern COLLECTION_LOG_PATTERN =
		Pattern.compile("New item added to your collection log: (.+)");

	@Inject
	private Client client;

	@Inject
	private AccountHistoryConfig config;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	private final EnumMap<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private boolean levelsInitialized = false;

	@Override
	protected void startUp()
	{
		log.debug("Account History started");
	}

	@Override
	protected void shutDown()
	{
		previousLevels.clear();
		levelsInitialized = false;
		log.debug("Account History stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN && !levelsInitialized)
		{
			for (Skill skill : Skill.values())
			{
				previousLevels.put(skill, client.getRealSkillLevel(skill));
			}
			levelsInitialized = true;
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			previousLevels.clear();
			levelsInitialized = false;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!levelsInitialized || !config.sendEvents())
		{
			return;
		}

		Skill skill = event.getSkill();
		int newLevel = event.getLevel();
		int oldLevel = previousLevels.getOrDefault(skill, 0);

		previousLevels.put(skill, newLevel);

		if (newLevel > oldLevel)
		{
			sendEvent("SKILL_LEVEL_UP", Map.of("skill", skill.getName(), "level", newLevel));
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.sendEvents() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		Matcher matcher = COLLECTION_LOG_PATTERN.matcher(event.getMessage());
		if (matcher.matches())
		{
			sendEvent("COLLECTION_LOG", Map.of("itemName", matcher.group(1)));
		}
	}

	private void sendEvent(String type, Object data)
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
