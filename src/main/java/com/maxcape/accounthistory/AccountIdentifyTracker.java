package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import okhttp3.OkHttpClient;

import java.util.Map;

@Slf4j(topic = "maxcape.AccountIdentifyTracker")
class AccountIdentifyTracker extends BaseTracker {
	private boolean shouldSendIdentify = true;

	AccountIdentifyTracker(AccountHistoryPlugin plugin, OkHttpClient httpClient, Gson gson) {
		super(plugin, httpClient, gson);
	}

	@Override
	void onLogin() {
		log.debug("Login detected, arming identify");
		shouldSendIdentify = true;
	}

	@Override
	void onLogout() {
		log.debug("Logout detected, clearing identify flag");
		shouldSendIdentify = false;
	}

	@Override
	void onGameTick(GameTick tick) {
		if (shouldSendIdentify && plugin.getPlayerName() != null) {
			log.debug("Identifying account: {} -- {}", plugin.getPlayerName(), plugin.getAccountHash());
			sendEvent("ACCOUNT_IDENTIFY", Map.of());
			shouldSendIdentify = false;
		}
	}
}
