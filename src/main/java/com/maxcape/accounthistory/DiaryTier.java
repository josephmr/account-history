package com.maxcape.accounthistory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.api.gameval.VarbitID;

/**
 * Tier completion vars (not reward-claim vars). Karamja's original three tiers
 * use 0 = not started, 1 = started, 2 = complete.
 * Reference: https://github.com/pajlads/DinkPlugin/blob/master/src/main/java/dinkplugin/domain/AchievementDiary.java
 */
enum DiaryTier
{
	ARDOUGNE_EASY(VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, "Ardougne", "Easy", 1),
	ARDOUGNE_MEDIUM(VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, "Ardougne", "Medium", 1),
	ARDOUGNE_HARD(VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, "Ardougne", "Hard", 1),
	ARDOUGNE_ELITE(VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE, "Ardougne", "Elite", 1),
	DESERT_EASY(VarbitID.DESERT_DIARY_EASY_COMPLETE, "Desert", "Easy", 1),
	DESERT_MEDIUM(VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, "Desert", "Medium", 1),
	DESERT_HARD(VarbitID.DESERT_DIARY_HARD_COMPLETE, "Desert", "Hard", 1),
	DESERT_ELITE(VarbitID.DESERT_DIARY_ELITE_COMPLETE, "Desert", "Elite", 1),
	FALADOR_EASY(VarbitID.FALADOR_DIARY_EASY_COMPLETE, "Falador", "Easy", 1),
	FALADOR_MEDIUM(VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, "Falador", "Medium", 1),
	FALADOR_HARD(VarbitID.FALADOR_DIARY_HARD_COMPLETE, "Falador", "Hard", 1),
	FALADOR_ELITE(VarbitID.FALADOR_DIARY_ELITE_COMPLETE, "Falador", "Elite", 1),
	FREMENNIK_EASY(VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, "Fremennik", "Easy", 1),
	FREMENNIK_MEDIUM(VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, "Fremennik", "Medium", 1),
	FREMENNIK_HARD(VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, "Fremennik", "Hard", 1),
	FREMENNIK_ELITE(VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE, "Fremennik", "Elite", 1),
	KANDARIN_EASY(VarbitID.KANDARIN_DIARY_EASY_COMPLETE, "Kandarin", "Easy", 1),
	KANDARIN_MEDIUM(VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, "Kandarin", "Medium", 1),
	KANDARIN_HARD(VarbitID.KANDARIN_DIARY_HARD_COMPLETE, "Kandarin", "Hard", 1),
	KANDARIN_ELITE(VarbitID.KANDARIN_DIARY_ELITE_COMPLETE, "Kandarin", "Elite", 1),
	KARAMJA_EASY(VarbitID.ATJUN_EASY_DONE, "Karamja", "Easy", 2),
	KARAMJA_MEDIUM(VarbitID.ATJUN_MED_DONE, "Karamja", "Medium", 2),
	KARAMJA_HARD(VarbitID.ATJUN_HARD_DONE, "Karamja", "Hard", 2),
	KARAMJA_ELITE(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, "Karamja", "Elite", 1),
	KOUREND_EASY(VarbitID.KOUREND_DIARY_EASY_COMPLETE, "Kourend & Kebos", "Easy", 1),
	KOUREND_MEDIUM(VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, "Kourend & Kebos", "Medium", 1),
	KOUREND_HARD(VarbitID.KOUREND_DIARY_HARD_COMPLETE, "Kourend & Kebos", "Hard", 1),
	KOUREND_ELITE(VarbitID.KOUREND_DIARY_ELITE_COMPLETE, "Kourend & Kebos", "Elite", 1),
	LUMBRIDGE_EASY(VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, "Lumbridge & Draynor", "Easy", 1),
	LUMBRIDGE_MEDIUM(VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, "Lumbridge & Draynor", "Medium", 1),
	LUMBRIDGE_HARD(VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, "Lumbridge & Draynor", "Hard", 1),
	LUMBRIDGE_ELITE(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, "Lumbridge & Draynor", "Elite", 1),
	MORYTANIA_EASY(VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, "Morytania", "Easy", 1),
	MORYTANIA_MEDIUM(VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, "Morytania", "Medium", 1),
	MORYTANIA_HARD(VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, "Morytania", "Hard", 1),
	MORYTANIA_ELITE(VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE, "Morytania", "Elite", 1),
	VARROCK_EASY(VarbitID.VARROCK_DIARY_EASY_COMPLETE, "Varrock", "Easy", 1),
	VARROCK_MEDIUM(VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, "Varrock", "Medium", 1),
	VARROCK_HARD(VarbitID.VARROCK_DIARY_HARD_COMPLETE, "Varrock", "Hard", 1),
	VARROCK_ELITE(VarbitID.VARROCK_DIARY_ELITE_COMPLETE, "Varrock", "Elite", 1),
	WESTERN_EASY(VarbitID.WESTERN_DIARY_EASY_COMPLETE, "Western Provinces", "Easy", 1),
	WESTERN_MEDIUM(VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, "Western Provinces", "Medium", 1),
	WESTERN_HARD(VarbitID.WESTERN_DIARY_HARD_COMPLETE, "Western Provinces", "Hard", 1),
	WESTERN_ELITE(VarbitID.WESTERN_DIARY_ELITE_COMPLETE, "Western Provinces", "Elite", 1),
	WILDERNESS_EASY(VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, "Wilderness", "Easy", 1),
	WILDERNESS_MEDIUM(VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, "Wilderness", "Medium", 1),
	WILDERNESS_HARD(VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, "Wilderness", "Hard", 1),
	WILDERNESS_ELITE(VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE, "Wilderness", "Elite", 1);

	static final Map<Integer, DiaryTier> BY_VARBIT = Collections.unmodifiableMap(
		Arrays.stream(values()).collect(Collectors.toMap(tier -> tier.varbitId, Function.identity()))
	);

	final int varbitId;
	final String area;
	final String difficulty;
	private final int completionThreshold;

	DiaryTier(int varbitId, String area, String difficulty, int completionThreshold)
	{
		this.varbitId = varbitId;
		this.area = area;
		this.difficulty = difficulty;
		this.completionThreshold = completionThreshold;
	}

	boolean isComplete(int value)
	{
		return value >= completionThreshold;
	}

	static DiaryTier fromMessage(String area, String difficulty)
	{
		String normalized = area.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
		switch (normalized)
		{
			case "western":
			case "western province":
				normalized = "western provinces";
				break;
			case "kourend":
			case "kourend and kebos":
				normalized = "kourend & kebos";
				break;
			case "lumbridge":
			case "lumbridge and draynor":
				normalized = "lumbridge & draynor";
				break;
			default:
				break;
		}
		for (DiaryTier tier : values())
		{
			if (tier.area.toLowerCase(Locale.ROOT).equals(normalized)
				&& tier.difficulty.equalsIgnoreCase(difficulty))
			{
				return tier;
			}
		}
		return null;
	}
}
