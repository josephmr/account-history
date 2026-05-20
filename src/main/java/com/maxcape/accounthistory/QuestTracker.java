package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

@Slf4j(topic = "maxcape.QuestTracker")
class QuestTracker extends BaseTracker {
	private final Client client;
	private final EnumMap<Quest, QuestState> prevStates = new EnumMap<>(Quest.class);
	private boolean dirty;

	QuestTracker(Client client, AccountHistoryPlugin plugin, AccountHistoryConfig config,
			OkHttpClient httpClient, Gson gson, File storeFile) {
		super(plugin, config, httpClient, gson, storeFile);
		this.client = client;
	}

	@Override
	void onGameStateChanged(GameStateChanged event) {
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING) {
			prevStates.clear();
			dirty = false;
		}
	}

	@Override
	void onVarbitChanged(VarbitChanged event) {
		dirty = true;
	}

	@Override
	void onGameTick(GameTick event) {
		if (!dirty) {
			return;
		}
		dirty = false;
		for (Quest quest : Quest.values()) {
			QuestState curr = quest.getState(client);
			QuestState prev = prevStates.put(quest, curr);
			if (prev != null && prev != QuestState.FINISHED && curr == QuestState.FINISHED) {
				log.debug("Quest completed: {}", quest.getName());
				sendEvent("QUEST_COMPLETED", Map.of("questName", quest.getName()));
			}
		}
	}
}
