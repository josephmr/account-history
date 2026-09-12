package com.maxcape.accounthistory;

import com.google.gson.Gson;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import okhttp3.OkHttpClient;

@Slf4j(topic = "maxcape.DiaryTracker")
class DiaryTracker extends BaseTracker
{
	private static final int INITIALIZATION_TICKS = 4;
	static final Pattern DIARY_PATTERN = Pattern.compile(
		"(?:Congratulations!\\s+)?You have completed all of the (?<difficulty>easy|medium|hard|elite) tasks in the (?<area>.+?) area(?=\\.|\\s|$)",
		Pattern.CASE_INSENSITIVE
	);

	private final Client client;
	// Includes the silent login baseline and live completions from either signal.
	// Never remove a tier within a session: reward updates or stale reads must not re-emit it.
	private final EnumSet<DiaryTier> completed = EnumSet.noneOf(DiaryTier.class);
	private long accountHash;
	private int initializationTicks;
	private boolean initialized;
	private boolean rescanPending;

	DiaryTracker(Client client, AccountHistoryPlugin plugin, OkHttpClient httpClient, Gson gson)
	{
		super(plugin, httpClient, gson);
		this.client = client;
	}

	@Override
	void onLogin()
	{
		reset();
	}

	@Override
	void onLogout()
	{
		reset();
	}

	private void reset()
	{
		completed.clear();
		accountHash = 0;
		initializationTicks = 0;
		initialized = false;
		rescanPending = false;
	}

	private boolean canTrack()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		long currentAccount = plugin.getAccountHash();
		if (plugin.isRestrictedWorld() || currentAccount == 0 || plugin.getPlayerName() == null)
		{
			reset();
			return false;
		}
		if (accountHash != currentAccount)
		{
			reset();
			accountHash = currentAccount;
		}
		return true;
	}

	@Override
	void onGameTick(GameTick event)
	{
		if (!canTrack())
		{
			return;
		}
		if (!initialized)
		{
			if (++initializationTicks >= INITIALIZATION_TICKS)
			{
				// Merge, rather than replace, to retain messages seen during initialization.
				for (DiaryTier tier : DiaryTier.values())
				{
					if (tier.isComplete(client.getVarbitValue(tier.varbitId)))
					{
						completed.add(tier);
					}
				}
				initialized = true;
				rescanPending = false;
				log.debug("Diary baseline initialized with {} completed tiers", completed.size());
			}
			return;
		}
		if (rescanPending)
		{
			rescan();
		}
	}

	@Override
	void onVarbitChanged(VarbitChanged event)
	{
		if (client.getGameState() == GameState.LOADING)
		{
			// Preserve the session through scene loading and read settled vars next tick.
			rescanPending = initialized;
			return;
		}
		if (!canTrack() || !initialized)
		{
			return;
		}
		if (event.getVarbitId() < 0)
		{
			// A server varplayer update can contain several diary varbits.
			rescanPending = true;
			return;
		}
		DiaryTier tier = DiaryTier.BY_VARBIT.get(event.getVarbitId());
		if (tier != null && tier.isComplete(event.getValue()))
		{
			complete(tier);
		}
	}

	private void rescan()
	{
		rescanPending = false;
		for (DiaryTier tier : DiaryTier.values())
		{
			if (tier.isComplete(client.getVarbitValue(tier.varbitId)))
			{
				complete(tier);
			}
		}
	}

	@Override
	void flush()
	{
		if (canTrack() && initialized && rescanPending)
		{
			rescan();
		}
	}

	@Override
	void onChatMessage(ChatMessage event)
	{
		if ((event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.MESBOX)
			|| event.getMessage() == null || !canTrack())
		{
			return;
		}
		String message = stripTags(event.getMessage().replaceAll("(?i)<br\\s*/?>", " "))
			.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
		Matcher matcher = DIARY_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return;
		}
		DiaryTier tier = DiaryTier.fromMessage(matcher.group("area"), matcher.group("difficulty"));
		if (tier != null)
		{
			complete(tier);
		}
	}

	private void complete(DiaryTier tier)
	{
		if (!completed.add(tier))
		{
			return;
		}
		log.debug("Achievement diary completed: {} {}", tier.difficulty, tier.area);
		sendEvent("ACHIEVEMENT_DIARY", Map.of("area", tier.area, "difficulty", tier.difficulty));
	}
}
