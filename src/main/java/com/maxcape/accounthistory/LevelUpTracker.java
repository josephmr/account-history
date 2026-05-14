package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
class LevelUpTracker extends BaseTracker
{
	private final Client client;
	private final EnumMap<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private boolean initialized = false;

	LevelUpTracker(Client client, AccountHistoryPlugin plugin, AccountHistoryConfig config,
				   OkHttpClient httpClient, Gson gson, File storeFile)
	{
		super(plugin, config, httpClient, gson, storeFile);
		this.client = client;
	}

	@Override
	void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN && !initialized)
		{
			for (Skill skill : Skill.values())
			{
				previousLevels.put(skill, client.getRealSkillLevel(skill));
			}
			initialized = true;
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			previousLevels.clear();
			initialized = false;
		}
	}

	@Override
	void onStatChanged(StatChanged event)
	{
		if (!initialized)
		{
			return;
		}
		Skill skill = event.getSkill();
		int newLevel = event.getLevel();
		int oldLevel = previousLevels.getOrDefault(skill, 0);
		previousLevels.put(skill, newLevel);
		if (oldLevel > 0 && newLevel > oldLevel)
		{
			log.debug("Skill level up: {} -> {}", skill.getName(), newLevel);
			sendEvent("SKILL_LEVEL_UP", Map.of("skill", skill.getName(), "level", newLevel));
		}
	}
}
