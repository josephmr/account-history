package com.maxcape.accounthistory;

import java.util.EnumSet;
import net.runelite.api.WorldType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountHistoryPluginWorldTypeTest
{
	private static final EnumSet<WorldType> ALLOWED_WORLD_TYPES = EnumSet.of(
		WorldType.MEMBERS,
		WorldType.PVP,
		WorldType.BOUNTY,
		WorldType.SKILL_TOTAL,
		WorldType.HIGH_RISK,
		WorldType.LAST_MAN_STANDING
	);

	@Test
	public void allowsRegularWorldTypes()
	{
		assertTrue(AccountHistoryPlugin.isAllowedWorld(EnumSet.noneOf(WorldType.class)));

		for (WorldType type : ALLOWED_WORLD_TYPES)
		{
			assertTrue(AccountHistoryPlugin.isAllowedWorld(EnumSet.of(type)));
		}

		assertTrue(AccountHistoryPlugin.isAllowedWorld(ALLOWED_WORLD_TYPES));
	}

	@Test
	public void rejectsSpecialWorldTypes()
	{
		EnumSet<WorldType> restrictedWorldTypes = EnumSet.complementOf(ALLOWED_WORLD_TYPES);
		for (WorldType type : restrictedWorldTypes)
		{
			assertFalse(AccountHistoryPlugin.isAllowedWorld(EnumSet.of(type)));
		}
	}

	@Test
	public void rejectsWorldWithAllowedAndSpecialTypes()
	{
		assertFalse(AccountHistoryPlugin.isAllowedWorld(EnumSet.of(
			WorldType.MEMBERS,
			WorldType.PVP_ARENA
		)));
	}
}
