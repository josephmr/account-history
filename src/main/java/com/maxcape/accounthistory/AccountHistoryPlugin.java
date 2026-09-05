package com.maxcape.accounthistory;

import com.google.gson.Gson;
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
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

@Slf4j(topic = "maxcape.AccountHistoryPlugin")
@PluginDescriptor(
	name = "MaxCape",
	description = "Tracks skill level ups, collection log entries, boss kills, achievement diaries, and quests",
	tags = {"skill", "collection log", "tracker", "history", "boss", "diary", "quest"}
)
public class AccountHistoryPlugin extends Plugin
{
	private static final EnumSet<WorldType> ALLOWED_WORLD_TYPES = EnumSet.of(
		WorldType.MEMBERS,
		WorldType.PVP,
		WorldType.BOUNTY,
		WorldType.SKILL_TOTAL,
		WorldType.HIGH_RISK,
		WorldType.LAST_MAN_STANDING
	);

	@Inject
	private Client client;

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
			new AccountIdentifyTracker(this, httpClient, gson),
			new LevelUpTracker(client, this, httpClient, gson),
			new CollectionLogTracker(this, httpClient, gson),
			new BossKillTracker(this, httpClient, gson),
			new DiaryTracker(this, httpClient, gson),
			new QuestTracker(client, this, httpClient, gson)
		);
		log.debug("MaxCape started");

		clientThread.invoke(() -> {
			GameStateChanged initialGameState = new GameStateChanged();
			initialGameState.setGameState(client.getGameState());
			trackers.forEach((tracker) -> tracker.onGameStateChanged(initialGameState));
		});
	}

	@Override
	protected void shutDown()
	{
		List<BaseTracker> toFlush = trackers;
		trackers = Collections.emptyList();
		clientThread.invoke(() -> toFlush.forEach(BaseTracker::flush));
		log.debug("MaxCape stopped");
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
		return !isAllowedWorld(client.getWorldType());
	}

	static boolean isAllowedWorld(EnumSet<WorldType> types)
	{
		return ALLOWED_WORLD_TYPES.containsAll(types);
	}

}
