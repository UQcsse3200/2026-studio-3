package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.chance.*;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.encounters.results.*;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceMineRiskIntegrationTest {
  private final MockPlayerStateGateway player = new MockPlayerStateGateway(40, 100, 35);
  private final ItemRewardGateway items = mock(ItemRewardGateway.class);
  private final ChanceEncounter mine = ChanceEncounterConfigLoader.loadEncounters().get(3);

  private ChanceEncounterSession session(Random random) {
    when(items.containsItem(anyString())).thenReturn(true);
    when(items.addItem(anyString())).thenReturn(true);
    when(items.rollbackItemAddition(anyString())).thenReturn(true);
    return new ChanceEncounterSession(
        1,
        mine,
        ChanceEncounterBehaviourFactory.create(mine, random, TestCardService.withCards()),
        new ChanceOutcomeApplier(
            player, mock(CardCatalogGateway.class), mock(DeckGateway.class), items),
        null);
  }

  @Test
  void shouldMapOnlyGoldWhenNoCurseAndNeverRepeatTheReward() {
    TestMineRandom random = new TestMineRandom(0.9, 0);
    ChanceEncounterSession session = session(random);
    assertTrue(session.resolveChoice("search-tunnels").isSuccess());
    AcquisitionResult result = session.getAcquisitionResult(null).orElseThrow();
    assertEquals(List.of(AcquisitionResult.Entry.gold(60)), result.entries());
    assertEquals(95, player.getCurrency());
    assertEquals(40, player.getHealth());
    assertEquals(
        ChanceResolution.Status.ALREADY_RESOLVED,
        session.resolveChoice("search-tunnels").getStatus());
    assertSame(result, session.getAcquisitionResult(null).orElseThrow());
    assertEquals(95, player.getCurrency());
    assertEquals(1, random.chanceDraws);
    verifyNoInteractions(items);
  }

  @Test
  void shouldRetryTheSameCurseAfterCompleteRollbackWithoutRedrawing() {
    TestMineRandom random = new TestMineRandom(0.1, 3);
    ChanceEncounterSession session = session(random);
    player.failNextCurrencyUpdate();
    ChanceResolution failed = session.resolveChoice("search-tunnels");
    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, failed.getStatus());
    assertEquals(35, player.getCurrency());
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    session.getAvailableChoices();
    session.getChoicePrompt();
    assertTrue(session.resolveChoice("search-tunnels").isSuccess());
    assertSame(failed.getOutcome(), session.getResolution().getOutcome());
    AcquisitionResult result = session.getAcquisitionResult(null).orElseThrow();
    assertEquals(2, result.entries().size());
    assertEquals(60, result.entries().getFirst().delta());
    assertEquals("ASHEN_EMBER", result.entries().getLast().id());
    assertEquals(1, result.entries().getLast().quantity());
    assertEquals(95, player.getCurrency());
    assertEquals(40, player.getHealth());
    assertEquals(1, random.chanceDraws);
    assertEquals(1, random.itemDraws);
    verify(items).rollbackItemAddition("ASHEN_EMBER");
    verify(items, times(2)).addItem("ASHEN_EMBER");
  }

  @Test
  void shouldBlockPartialRollbackWithoutShowingAcquisitionOrRedrawing() {
    TestMineRandom random = new TestMineRandom(0, 0);
    ChanceEncounterSession session = session(random);
    when(items.rollbackItemAddition("BRAND_OF_THE_DAMNED")).thenReturn(false);
    player.failNextCurrencyUpdate();
    ChanceResolution failed = session.resolveChoice("search-tunnels");
    assertEquals(ChanceResolution.Status.ROLLBACK_FAILED, failed.getStatus());
    assertSame(failed, session.resolveChoice("search-tunnels"));
    assertSame(failed, session.resolveChoice("leave"));
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    assertFalse(session.complete());
    assertEquals(35, player.getCurrency());
    assertEquals(1, random.chanceDraws);
    assertEquals(1, random.itemDraws);
    verify(items).addItem("BRAND_OF_THE_DAMNED");
  }

  @Test
  void shouldReuseNoCurseOutcomeAfterGoldFailure() {
    TestMineRandom random = new TestMineRandom(0.9, 0);
    ChanceEncounterSession session = session(random);
    player.failNextCurrencyUpdate();
    assertFalse(session.resolveChoice("search-tunnels").isSuccess());
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    assertTrue(session.resolveChoice("search-tunnels").isSuccess());
    assertEquals(1, random.chanceDraws);
    assertEquals(0, random.itemDraws);
    assertEquals(95, player.getCurrency());
  }

  @Test
  void shouldNotRerollOrRegrantAfterDisplayFailureCloseOrContinue() {
    TestMineRandom random = new TestMineRandom(0.1, 2);
    ChanceEncounterSession session = session(random);
    assertTrue(session.resolveChoice("search-tunnels").isSuccess());
    ChanceAcquisitionSession acquisitions = session.getAcquisitionSession(null);
    AcquisitionResult result = acquisitions.getResult().orElseThrow();
    AcquisitionView view = mock(AcquisitionView.class);
    doThrow(new IllegalStateException("Display failed")).when(view).show(any(), any(), any());
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    AtomicReference<Runnable> close = new AtomicReference<>();
    AtomicReference<Runnable> next = new AtomicReference<>();
    doAnswer(
            invocation -> {
              next.set(invocation.getArgument(1));
              close.set(invocation.getArgument(2));
              return null;
            })
        .when(view)
        .show(any(), any(), any());
    AtomicInteger restored = new AtomicInteger();
    assertTrue(acquisitions.show(view, session::complete, restored::incrementAndGet));
    close.get().run();
    close.get().run();
    next.get().run();
    assertEquals(1, restored.get());
    assertFalse(session.isCompleted());
    assertTrue(session.complete());
    assertSame(result, acquisitions.getResult().orElseThrow());
    assertEquals(95, player.getCurrency());
    assertEquals(1, random.chanceDraws);
    assertEquals(1, random.itemDraws);
    verify(items).addItem("SHATTERED_AEGIS");
  }

  @Test
  void shouldGuardReentryDuringTheRandomDrawItself() {
    AtomicReference<ChanceEncounterSession> reference = new AtomicReference<>();
    AtomicInteger draws = new AtomicInteger();
    Random random =
        new Random() {
          @Override
          public double nextDouble() {
            draws.incrementAndGet();
            assertEquals(
                ChanceResolution.Status.RESOLUTION_IN_PROGRESS,
                reference.get().resolveChoice("search-tunnels").getStatus());
            assertEquals(
                ChanceResolution.Status.RESOLUTION_IN_PROGRESS,
                reference.get().resolveChoice("leave").getStatus());
            return 0.9;
          }
        };
    ChanceEncounterSession session = session(random);
    reference.set(session);
    assertTrue(session.resolveChoice("search-tunnels").isSuccess());
    assertEquals(1, draws.get());
    assertEquals(95, player.getCurrency());
  }

  @Test
  void shouldRejectUnknownItemWithoutGoldAndKeepItsCachedRoll() {
    TestMineRandom random = new TestMineRandom(0, 0);
    ChanceEncounterSession session = session(random);
    when(items.containsItem("BRAND_OF_THE_DAMNED")).thenReturn(false);
    assertEquals(
        ChanceResolution.Status.ITEM_NOT_FOUND,
        session.resolveChoice("search-tunnels").getStatus());
    assertEquals(
        ChanceResolution.Status.ITEM_NOT_FOUND,
        session.resolveChoice("search-tunnels").getStatus());
    assertEquals(35, player.getCurrency());
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    assertEquals(1, random.chanceDraws);
    assertEquals(1, random.itemDraws);
    verify(items, never()).addItem(anyString());
  }

  @Test
  void shouldDrawAgainForNewSessionWithoutChangingFirstResult() {
    TestMineRandom firstRandom = new TestMineRandom(0.9, 0);
    ChanceEncounterSession first = session(firstRandom);
    assertTrue(first.resolveChoice("search-tunnels").isSuccess());
    AcquisitionResult firstResult = first.getAcquisitionResult(null).orElseThrow();
    assertTrue(first.complete());
    TestMineRandom nextRandom = new TestMineRandom(0.1, 1);
    ChanceEncounterSession next = session(nextRandom);
    assertTrue(next.resolveChoice("search-tunnels").isSuccess());
    AcquisitionResult nextResult = next.getAcquisitionResult(null).orElseThrow();
    assertEquals(1, firstResult.entries().size());
    assertEquals("BLOOD_OATH_SIGIL", nextResult.entries().getLast().id());
    assertNotEquals(firstResult.resultId(), nextResult.resultId());
    assertSame(firstResult, first.getAcquisitionResult(null).orElseThrow());
    assertEquals(155, player.getCurrency());
    assertEquals(1, firstRandom.chanceDraws);
    assertEquals(1, nextRandom.chanceDraws);
    verify(items).addItem("BLOOD_OATH_SIGIL");
  }

  @Test
  void shouldLeaveWithoutRewardOrRandomDraw() {
    TestMineRandom random = new TestMineRandom(0, 0);
    ChanceEncounterSession session = session(random);
    assertTrue(session.resolveChoice("leave").isSuccess());
    assertEquals(35, player.getCurrency());
    assertEquals(40, player.getHealth());
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    assertTrue(session.complete());
    assertEquals(0, random.chanceDraws);
    verifyNoInteractions(items);
  }
}
