package com.maxcape.accounthistory;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;

import java.util.EnumMap;
import java.util.Map;

class LevelUpTracker
{
	private final Client client;
	private final AccountHistoryPlugin plugin;
	private final AccountHistoryConfig config;

	private final EnumMap<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private boolean initialized = false;

	LevelUpTracker(Client client, AccountHistoryPlugin plugin, AccountHistoryConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	void onLoggedIn()
	{
		if (initialized)
		{
			return;
		}
		for (Skill skill : Skill.values())
		{
			previousLevels.put(skill, client.getRealSkillLevel(skill));
		}
		initialized = true;
	}

	void onLoggedOut()
	{
		previousLevels.clear();
		initialized = false;
	}

	void onStatChanged(StatChanged event)
	{
		if (!initialized || !config.sendEvents())
		{
			return;
		}
		Skill skill = event.getSkill();
		int newLevel = event.getLevel();
		int oldLevel = previousLevels.getOrDefault(skill, 0);
		previousLevels.put(skill, newLevel);
		if (oldLevel > 0 && newLevel > oldLevel)
		{
			plugin.sendEvent("SKILL_LEVEL_UP", Map.of("skill", skill.getName(), "level", newLevel));
		}
	}
}
