package com.maxcape.accounthistory;

import com.google.gson.Gson;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.Map;

class AccountIdentifyTracker extends BaseTracker
{
	AccountIdentifyTracker(AccountHistoryPlugin plugin, AccountHistoryConfig config,
						   OkHttpClient httpClient, Gson gson, File storeFile)
	{
		super(plugin, config, httpClient, gson, storeFile);
	}

	@Override
	void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			sendEvent("ACCOUNT_IDENTIFY", Map.of());
		}
	}
}
