package com.maxcape.accounthistory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import okhttp3.OkHttpClient;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j(topic = "maxcape.BossKillTracker")
class BossKillTracker extends BaseTracker
{
	// "Your X kill/chest/completion/harvest/success/opened count is: N"
	private static final Pattern PRIMARY_PATTERN =
		Pattern.compile("Your (.+?)\\s(kill|chest|completion|harvest|success|opened)\\s?count is: ?([\\d,]+)\\.", Pattern.CASE_INSENSITIVE);
	// "Your (completed|subdued) X count is: N" — raids, Wintertodt
	private static final Pattern SECONDARY_PATTERN =
		Pattern.compile("Your (?:completed|subdued) (.+?) count is: ([\\d,]+)\\.", Pattern.CASE_INSENSITIVE);

	BossKillTracker(AccountHistoryPlugin plugin, OkHttpClient httpClient, Gson gson)
	{
		super(plugin, httpClient, gson);
	}

	@Override
	void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = stripTags(event.getMessage());

		if (!message.contains("count is:"))
		{
			return;
		}

		String bossName = null;
		int totalKc = 0;

		Matcher m = PRIMARY_PATTERN.matcher(message);
		if (m.matches())
		{
			String rawName = m.group(1);
			String type = m.group(2);
			totalKc = parseKc(m.group(3));
			bossName = normalizeBossName(rawName, type);
		}
		else
		{
			m = SECONDARY_PATTERN.matcher(message);
			if (m.matches())
			{
				bossName = m.group(1);
				totalKc = parseKc(m.group(2));
			}
		}

		if (bossName == null)
		{
			log.debug("Kill count message did not match any pattern: '{}'", message);
			return;
		}

		log.debug("Boss kill recorded: boss='{}' totalKc={}", bossName, totalKc);
		sendEvent("BOSS_KILL", Map.of("bossName", bossName, "totalKc", totalKc));
	}

	private static int parseKc(String s)
	{
		return Integer.parseInt(s.replace(",", ""));
	}

	private static String normalizeBossName(String rawName, String type)
	{
		switch (type.toLowerCase(Locale.ROOT))
		{
			case "kill":
			case "success":
			case "opened":
				return rawName;
			case "chest":
				// Barrows chests and Lunar Chest (Perilous Moons)
				if (rawName.equals("Barrows") || rawName.contains("Lunar"))
				{
					return rawName;
				}
				return null;
			case "completion":
				if (rawName.equals("Gauntlet"))
				{
					return "Crystalline Hunllef";
				}
				if (rawName.equals("Corrupted Gauntlet"))
				{
					return "Corrupted Hunllef";
				}
				return null;
			case "harvest":
				if (rawName.equalsIgnoreCase("herbiboar"))
				{
					return "Herbiboar";
				}
				return null;
			default:
				return null;
		}
	}
}
