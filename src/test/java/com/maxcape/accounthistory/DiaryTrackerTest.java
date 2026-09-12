package com.maxcape.accounthistory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class DiaryTrackerTest
{
	private final Client client = mock(Client.class);
	private final AccountHistoryPlugin plugin = mock(AccountHistoryPlugin.class);
	private final OkHttpClient http = mock(OkHttpClient.class);
	private final Gson gson = new Gson();
	private DiaryTracker tracker;

	@Before
	public void setUp()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(plugin.getAccountHash()).thenReturn(123L);
		when(plugin.getPlayerName()).thenReturn("Test Player");
		when(http.newCall(any(Request.class))).thenReturn(mock(Call.class));
		tracker = new DiaryTracker(client, plugin, http, gson);
		state(GameState.LOGGED_IN);
	}

	@Test
	public void all48TiersEmitCanonicalPayloadsOnlyOnCompletion() throws IOException
	{
		assertEquals(48, DiaryTier.values().length);
		assertEquals(48, DiaryTier.BY_VARBIT.size());
		ticks(4);
		for (DiaryTier tier : DiaryTier.values())
		{
			boolean originalKaramja = tier.area.equals("Karamja") && !tier.difficulty.equals("Elite");
			int threshold = originalKaramja ? 2 : 1;
			assertFalse(tier.isComplete(threshold - 1));
			assertTrue(tier.isComplete(threshold));
			change(tier, threshold - 1);
			change(tier, threshold);
			change(tier, threshold);
			change(tier, threshold + 1);
		}
		List<Request> requests = requests(48);
		int index = 0;
		for (DiaryTier tier : DiaryTier.values())
		{
			JsonObject payload = payload(requests.get(index++));
			assertEquals("ACHIEVEMENT_DIARY", payload.get("type").getAsString());
			assertEquals("123", payload.get("accountHash").getAsString());
			assertEquals("Test Player", payload.get("playerName").getAsString());
			assertNotNull(payload.get("timestamp"));
			assertEquals(tier.area, payload.getAsJsonObject("data").get("area").getAsString());
			assertEquals(tier.difficulty, payload.getAsJsonObject("data").get("difficulty").getAsString());
		}
	}

	@Test
	public void initializesSilentlyAndIgnoresEarlyVarChanges()
	{
		when(client.getVarbitValue(anyInt())).thenReturn(2);
		change(DiaryTier.DESERT_EASY, 1);
		ticks(3);
		verify(client, never()).getVarbitValue(anyInt());
		ticks(1);
		for (DiaryTier tier : DiaryTier.values())
		{
			change(tier, 2);
		}
		message(ChatMessageType.MESBOX, "Desert", "easy");
		verifyNoInteractions(http);
	}

	@Test
	public void karamjaStartedIsNotCompleteAndEliteUsesNormalThreshold()
	{
		ticks(4);
		change(DiaryTier.KARAMJA_EASY, 1);
		change(DiaryTier.KARAMJA_MEDIUM, 1);
		change(DiaryTier.KARAMJA_HARD, 1);
		verifyNoInteractions(http);
		change(DiaryTier.KARAMJA_EASY, 2);
		change(DiaryTier.KARAMJA_MEDIUM, 2);
		change(DiaryTier.KARAMJA_HARD, 2);
		change(DiaryTier.KARAMJA_ELITE, 1);
		requests(4);
	}

	@Test
	public void messageBoxAndLegacyChatNormalizeNamesAndFormatting() throws IOException
	{
		chat(ChatMessageType.MESBOX, "<col=ff0000>Congratulations!</col><br>You have completed all of the HARD tasks in the Western Province area. Speak to someone.");
		message(ChatMessageType.GAMEMESSAGE, "Kourend and Kebos", "medium");
		message(ChatMessageType.MESBOX, "Lumbridge", "elite");
		List<Request> requests = requests(3);
		assertData(requests.get(0), "Western Provinces", "Hard");
		assertData(requests.get(1), "Kourend & Kebos", "Medium");
		assertData(requests.get(2), "Lumbridge & Draynor", "Elite");
	}

	@Test
	public void deduplicatesBothSignalOrdersWithoutSuppressingOtherTiers()
	{
		ticks(4);
		message(ChatMessageType.MESBOX, "Desert", "easy");
		change(DiaryTier.DESERT_EASY, 1);
		change(DiaryTier.DESERT_MEDIUM, 1);
		message(ChatMessageType.MESBOX, "Desert", "medium");
		message(ChatMessageType.GAMEMESSAGE, "Desert", "easy");
		requests(2);
	}

	@Test
	public void retainsMessagesDuringInitializationEvenIfBaselineLags()
	{
		message(ChatMessageType.MESBOX, "Karamja", "easy");
		ticks(4);
		change(DiaryTier.KARAMJA_EASY, 2);
		message(ChatMessageType.MESBOX, "Karamja", "easy");
		requests(1);
	}

	@Test
	public void ignoresUnrelatedMessagesAndVarbits()
	{
		ticks(4);
		message(ChatMessageType.PUBLICCHAT, "Desert", "easy");
		message(ChatMessageType.MESBOX, "Unknown", "easy");
		message(ChatMessageType.MESBOX, "Desert", "master");
		chat(ChatMessageType.MESBOX, "You have completed a task in the Desert area.");
		chat(ChatMessageType.MESBOX, null);
		VarbitChanged event = new VarbitChanged();
		event.setVarbitId(Integer.MAX_VALUE);
		event.setValue(1);
		tracker.onVarbitChanged(event);
		verifyNoInteractions(http);
	}

	@Test
	public void coalescesVarplayerUpdatesAndReadsVarbitsOnNextTick()
	{
		ticks(4);
		clearInvocations(client);
		when(client.getVarbitValue(DiaryTier.DESERT_EASY.varbitId)).thenReturn(1);
		VarbitChanged event = new VarbitChanged();
		event.setVarpId(123);
		event.setValue(1);
		tracker.onVarbitChanged(event);
		tracker.onVarbitChanged(event);
		verifyNoInteractions(http);
		ticks(1);
		requests(1);
		verify(client, times(48)).getVarbitValue(anyInt());
		ticks(1);
		verify(client, times(48)).getVarbitValue(anyInt());
	}

	@Test
	public void flushesPendingScanWhileLoggedIn()
	{
		ticks(4);
		when(client.getVarbitValue(DiaryTier.DESERT_EASY.varbitId)).thenReturn(1);
		tracker.onVarbitChanged(new VarbitChanged());
		tracker.flush();
		tracker.flush();
		requests(1);
	}

	@Test
	public void hopAndLogoutRebaselineWithoutReplayingCompletedTiers()
	{
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		when(client.getVarbitValue(DiaryTier.DESERT_EASY.varbitId)).thenReturn(1);
		state(GameState.HOPPING);
		state(GameState.LOGGED_IN);
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		state(GameState.LOGIN_SCREEN);
		state(GameState.LOGGED_IN);
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		requests(1);
	}

	@Test
	public void loadingAndRegressionsDoNotForgetLiveCompletions()
	{
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		state(GameState.LOADING);
		ticks(1);
		state(GameState.LOGGED_IN);
		change(DiaryTier.DESERT_EASY, 0);
		change(DiaryTier.DESERT_EASY, 1);
		change(DiaryTier.DESERT_MEDIUM, 1);
		requests(2);
	}

	@Test
	public void catchesVariableChangesDuringSceneLoading()
	{
		ticks(4);
		state(GameState.LOADING);
		when(client.getVarbitValue(DiaryTier.DESERT_EASY.varbitId)).thenReturn(1);
		change(DiaryTier.DESERT_EASY, 1);
		ticks(1);
		verifyNoInteractions(http);
		state(GameState.LOGGED_IN);
		ticks(1);
		change(DiaryTier.DESERT_EASY, 1);
		requests(1);
	}

	@Test
	public void leavingRestrictedWorldEstablishesANewSilentBaseline()
	{
		ticks(4);
		when(plugin.isRestrictedWorld()).thenReturn(true);
		change(DiaryTier.DESERT_EASY, 1);
		message(ChatMessageType.MESBOX, "Desert", "easy");
		when(plugin.isRestrictedWorld()).thenReturn(false);
		when(client.getVarbitValue(DiaryTier.DESERT_EASY.varbitId)).thenReturn(1);
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		verifyNoInteractions(http);
		change(DiaryTier.DESERT_MEDIUM, 1);
		requests(1);
	}

	@Test
	public void accountChangeDiscardsOldBaselineAndUsesNewIdentity() throws IOException
	{
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		when(plugin.getAccountHash()).thenReturn(456L);
		change(DiaryTier.DESERT_EASY, 1); // Before the new account's baseline.
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		List<Request> requests = requests(2);
		assertEquals("456", payload(requests.get(1)).get("accountHash").getAsString());
	}

	@Test
	public void restrictedWorldAndMissingIdentityCannotInitializeOrSend()
	{
		when(plugin.isRestrictedWorld()).thenReturn(true);
		ticks(5);
		message(ChatMessageType.MESBOX, "Desert", "easy");
		when(plugin.isRestrictedWorld()).thenReturn(false);
		when(plugin.getAccountHash()).thenReturn(0L);
		ticks(5);
		message(ChatMessageType.MESBOX, "Desert", "easy");
		when(plugin.getAccountHash()).thenReturn(123L);
		when(plugin.getPlayerName()).thenReturn(null);
		ticks(5);
		message(ChatMessageType.MESBOX, "Desert", "easy");
		verifyNoInteractions(http);
		verify(client, never()).getVarbitValue(anyInt());
		when(plugin.getPlayerName()).thenReturn("Test Player");
		ticks(4);
		change(DiaryTier.DESERT_EASY, 1);
		requests(1);
	}

	private void state(GameState state)
	{
		when(client.getGameState()).thenReturn(state);
		GameStateChanged event = new GameStateChanged();
		event.setGameState(state);
		tracker.onGameStateChanged(event);
	}

	private void ticks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			tracker.onGameTick(new GameTick());
		}
	}

	private void change(DiaryTier tier, int value)
	{
		VarbitChanged event = new VarbitChanged();
		event.setVarbitId(tier.varbitId);
		event.setValue(value);
		tracker.onVarbitChanged(event);
	}

	private void message(ChatMessageType type, String area, String difficulty)
	{
		chat(type, "You have completed all of the " + difficulty + " tasks in the " + area + " area. Speak to someone to claim your reward.");
	}

	private void chat(ChatMessageType type, String text)
	{
		ChatMessage event = new ChatMessage();
		event.setType(type);
		event.setMessage(text);
		tracker.onChatMessage(event);
	}

	private List<Request> requests(int count)
	{
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(http, times(count)).newCall(captor.capture());
		return captor.getAllValues();
	}

	private JsonObject payload(Request request) throws IOException
	{
		Buffer buffer = new Buffer();
		request.body().writeTo(buffer);
		return gson.fromJson(buffer.readUtf8(), JsonObject.class);
	}

	private void assertData(Request request, String area, String difficulty) throws IOException
	{
		JsonObject data = payload(request).getAsJsonObject("data");
		assertEquals(area, data.get("area").getAsString());
		assertEquals(difficulty, data.get("difficulty").getAsString());
	}
}
