package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

@Slf4j(topic = "maxcape.LevelUpTracker")
class LevelUpTracker extends BaseTracker
{
	private static final int INIT_TICKS = 10;
	private static final int SKILL_COUNT = Skill.values().length;

	private final Client client;
	private final EnumMap<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);

	// -1 = not started; >= 0 = counting toward next init attempt
	private int initTicksWaited = -1;
	private boolean initialized = false;

	LevelUpTracker(Client client, AccountHistoryPlugin plugin, AccountHistoryConfig config,
				   OkHttpClient httpClient, Gson gson, File storeFile)
	{
		super(plugin, config, httpClient, gson, storeFile);
		this.client = client;
	}

	@Override
	void onLogin()
	{
		log.debug("Login detected, starting level initialization");
		resetState(0);
	}

	@Override
	void onLogout()
	{
		log.debug("Logout detected, clearing level state");
		resetState(-1);
	}

	private void resetState(int initTicks)
	{
		initialized = false;
		previousLevels.clear();
		initTicksWaited = initTicks;
	}

	@Override
	void onGameTick(GameTick event)
	{
		if (initialized || initTicksWaited < 0)
		{
			return;
		}
		if (++initTicksWaited >= INIT_TICKS)
		{
			initLevels();
			initTicksWaited = 0;
		}
	}

	private void initLevels()
	{
		for (Skill skill : Skill.values())
		{
			int level = client.getRealSkillLevel(skill);
			if (level > 0)
			{
				previousLevels.put(skill, level);
			}
		}
		if (previousLevels.size() >= SKILL_COUNT)
		{
			initialized = true;
			log.debug("Level tracker initialized with {} skills", previousLevels.size());
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
