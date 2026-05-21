package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

@Slf4j(topic = "maxcape.QuestTracker")
class QuestTracker extends BaseTracker {
	// 500 ticks (~5 minutes) after the first varbit change before we check quest
	// states.
	// Varbits change very frequently; this avoids running getState() for every
	// quest each tick.
	private static final int CHECK_DELAY_TICKS = 500;
	private static final Quest[] QUESTS = Quest.values();

	private final Client client;
	private final EnumMap<Quest, QuestState> prevStates = new EnumMap<>(Quest.class);
	private int ticksUntilCheck;

	QuestTracker(Client client, AccountHistoryPlugin plugin, AccountHistoryConfig config,
			OkHttpClient httpClient, Gson gson, File storeFile) {
		super(plugin, config, httpClient, gson, storeFile);
		this.client = client;
	}

	@Override
	void onLogout() {
		log.debug("Logout detected, clearing quest state");
		prevStates.clear();
		ticksUntilCheck = 0;
	}

	@Override
	void onVarbitChanged(VarbitChanged event) {
		if (ticksUntilCheck == 0) {
			ticksUntilCheck = CHECK_DELAY_TICKS;
		}
	}

	@Override
	void onGameTick(GameTick event) {
		if (ticksUntilCheck > 0 && --ticksUntilCheck == 0) {
			checkQuestStates();
		}
	}

	@Override
	void flush() {
		if (ticksUntilCheck > 0) {
			ticksUntilCheck = 0;
			checkQuestStates();
		}
		super.flush();
	}

	private void checkQuestStates() {
		boolean initializing = prevStates.isEmpty();
		for (Quest quest : QUESTS) {
			QuestState curr = quest.getState(client);
			QuestState prev = prevStates.put(quest, curr);
			// prev == null on the first check after login — skip to avoid re-firing
			// events for quests already completed before this session started.
			if (prev != null && prev != QuestState.FINISHED && curr == QuestState.FINISHED) {
				log.debug("Quest completed: {}", quest.getName());
				sendEvent("QUEST_COMPLETED", Map.of("questName", quest.getName()));
			}
		}
		if (initializing) {
			log.debug("Quest state initialized with {} quests", prevStates.size());
		}
	}
}
