package com.maxcape.accounthistory;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

@Slf4j(topic = "maxcape.AccountHistoryPlugin")
@PluginDescriptor(
	name = "Account History",
	description = "Tracks skill level ups, collection log entries, boss kills, achievement diaries, and quests",
	tags = {"skill", "collection log", "tracker", "history", "boss", "diary", "quest"}
)
public class AccountHistoryPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AccountHistoryConfig config;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	@Inject
	private ClientThread clientThread;

	private List<BaseTracker> trackers = Collections.emptyList();

	@Override
	protected void startUp()
	{
		trackers = List.of(
			new AccountIdentifyTracker(this, config, httpClient, gson),
			new LevelUpTracker(client, this, config, httpClient, gson),
			new CollectionLogTracker(this, config, httpClient, gson),
			new BossKillTracker(this, config, httpClient, gson),
			new DiaryTracker(this, config, httpClient, gson),
			new QuestTracker(client, this, config, httpClient, gson)
		);
		log.debug("Account History started");

		clientThread.invoke(() -> {
			GameStateChanged initialGameState = new GameStateChanged();
			initialGameState.setGameState(client.getGameState());
			trackers.forEach((tracker) -> tracker.onGameStateChanged(initialGameState));
		});
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() -> {
			trackers.forEach(BaseTracker::flush);
			trackers = Collections.emptyList();
		});
		log.debug("Account History stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			trackers.forEach(BaseTracker::flush);
		}
		trackers.forEach(t -> t.onGameStateChanged(event));
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		trackers.forEach(t -> t.onStatChanged(event));
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		trackers.forEach(t -> t.onChatMessage(event));
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		trackers.forEach(t -> t.onVarbitChanged(event));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		trackers.forEach(t -> t.onGameTick(event));
	}

	String getPlayerName()
	{
		if (client.getLocalPlayer() != null)
		{
			return client.getLocalPlayer().getName();
		}
		return null;
	}

	long getAccountHash()
	{
		return client.getAccountHash();
	}

	boolean isRestrictedWorld()
	{
		EnumSet<WorldType> types = client.getWorldType();
		return types.contains(WorldType.SEASONAL)
			|| types.contains(WorldType.DEADMAN)
			|| types.contains(WorldType.QUEST_SPEEDRUNNING)
			|| types.contains(WorldType.NOSAVE_MODE)
			|| types.contains(WorldType.FRESH_START_WORLD)
			|| types.contains(WorldType.TOURNAMENT_WORLD)
			|| types.contains(WorldType.BETA_WORLD);
	}

	@Provides
	AccountHistoryConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccountHistoryConfig.class);
	}
}
