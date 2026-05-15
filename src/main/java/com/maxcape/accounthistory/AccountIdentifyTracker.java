package com.maxcape.accounthistory;

import com.google.gson.Gson;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.Map;

class AccountIdentifyTracker extends BaseTracker
{
	private boolean shouldSendIdentify = true;

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
			shouldSendIdentify = true;
		} else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			shouldSendIdentify = false;
		}
	}

	@Override
	void onGameTick(GameTick tick)
	{
		if (shouldSendIdentify && plugin.getPlayerName() != null)
		{
			sendEvent("ACCOUNT_IDENTIFY", Map.of());
			shouldSendIdentify = false;
		}
	}
}
