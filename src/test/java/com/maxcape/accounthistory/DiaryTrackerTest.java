package com.maxcape.accounthistory;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiaryTrackerTest
{
	@Test
	public void matchesMessageWithoutCongratulationsPrefix()
	{
		Matcher matcher = DiaryTracker.DIARY_PATTERN.matcher(
			"You have completed all of the easy tasks in the Karamja area. Speak to Pirate Jackie the Fruit to claim your reward."
		);

		assertTrue(matcher.matches());
		assertEquals("easy", matcher.group("difficulty"));
		assertEquals("Karamja", matcher.group("area"));
	}

	@Test
	public void stillMatchesMessageWithCongratulationsPrefix()
	{
		Matcher matcher = DiaryTracker.DIARY_PATTERN.matcher(
			"Congratulations! You have completed all of the hard tasks in the Karamja area."
		);

		assertTrue(matcher.matches());
		assertEquals("hard", matcher.group("difficulty"));
		assertEquals("Karamja", matcher.group("area"));
	}
}
