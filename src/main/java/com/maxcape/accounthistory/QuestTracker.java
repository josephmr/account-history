package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import okhttp3.OkHttpClient;

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
	private int ticksElapsed = -1; // -1 = inactive, >=0 = ticks elapsed since armed

	QuestTracker(Client client, AccountHistoryPlugin plugin, AccountHistoryConfig config,
			OkHttpClient httpClient, Gson gson) {
		super(plugin, config, httpClient, gson);
		this.client = client;
	}

	@Override
	void onLogin() {
		log.debug("Login detected, initializing quest state");
		prevStates.clear();
		ticksElapsed = CHECK_DELAY_TICKS - 10;
	}

	@Override
	void onLogout() {
		log.debug("Logout detected, clearing quest state");
		prevStates.clear();
		ticksElapsed = -1;
	}

	@Override
	void onVarbitChanged(VarbitChanged event) {
		if (ticksElapsed < 0) {
			ticksElapsed = 0;
		}
	}

	@Override
	void onGameTick(GameTick event) {
		if (ticksElapsed >= 0 && ++ticksElapsed >= CHECK_DELAY_TICKS) {
			ticksElapsed = -1;
			checkQuestStates();
		}
	}

	@Override
	void flush() {
		if (ticksElapsed >= 0) {
			ticksElapsed = -1;
			try {
				checkQuestStates();
			} catch (Exception e) {
				// quest.getState will throw during plugin shutdown, silently ignore since we
				// cannot do anything about it at this point
			}

		}
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
