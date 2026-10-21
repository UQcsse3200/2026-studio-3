package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.encounters.results.AcquisitionResult;
import com.csse3200.game.encounters.results.AcquisitionView;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChanceAcquisitionSessionTest {
  private final MockPlayerStateGateway player = new MockPlayerStateGateway(40, 100, 35);
  private final PlayerRunState state = new PlayerRunState(40, 100, 35);
  private final MockDeckGateway deck = new MockDeckGateway();
  private final AtomicInteger completed = new AtomicInteger();
  private final ChanceEncounterSession session =
      new ChanceEncounterSession(
          1,
          new ChanceEncounter(
              "test",
              "Test",
              List.of(
                  new ChanceChoice(
                      "accept",
                      "Accept",
                      ChanceOutcome.withRewards(0, 5, List.of("bandage"), List.of("LUCKY_COIN"))),
                  new ChanceChoice("leave", "Leave", new ChanceOutcome(0, 0)))),
          new ChanceOutcomeApplier(
              player,
              new MockCardCatalogGateway("bandage"),
              deck,
              new PlayerRunStateItemAdapter(state)),
          (node, success) -> completed.incrementAndGet());
  private final ChanceAcquisitionSession acquisitions = session.getAcquisitionSession(null);
  private final TestView view = new TestView();

  private void settle() {
    assertTrue(session.resolveChoice("accept").isSuccess());
  }

  private void assertRewardsOnce() {
    assertEquals(40, player.getCurrency());
    assertEquals(40, player.getHealth());
    assertEquals(1, state.getOwnedItemCount(ItemType.LUCKY_COIN));
    assertEquals(List.of("bandage"), deck.getCardIds());
  }

  @Test
  void shouldContinueOnceThroughExistingCompletionAction() {
    settle();
    assertTrue(acquisitions.show(view, session::complete, () -> fail("Not Close")));
    view.onContinue.run();
    view.onContinue.run();
    view.onClose.run();
    assertEquals(1, completed.get());
    assertFalse(session.complete());
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    assertRewardsOnce();
  }

  @Test
  void shouldCloseToOutcomeWithoutCompletingOrRegranting() {
    settle();
    AtomicInteger restoredOutcome = new AtomicInteger();
    assertTrue(acquisitions.show(view, session::complete, restoredOutcome::incrementAndGet));
    view.onClose.run();
    view.onClose.run();
    view.onContinue.run();
    assertEquals(1, restoredOutcome.get());
    assertEquals(0, completed.get());
    assertFalse(session.isCompleted());
    assertFalse(acquisitions.show(view, session::complete, restoredOutcome::incrementAndGet));
    assertTrue(session.complete());
    assertEquals(1, completed.get());
    assertRewardsOnce();
  }

  @Test
  void shouldRetryFailedPresentationUsingSameResultWithoutSettlement() {
    settle();
    AcquisitionResult result = acquisitions.getResult().orElseThrow();
    view.failShow = true;
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    Runnable stale = view.onContinue;
    assertFalse(session.isCompleted());
    assertRewardsOnce();
    view.failShow = false;
    assertTrue(acquisitions.show(view, session::complete, () -> {}));
    assertSame(result, view.result);
    stale.run();
    assertEquals(0, completed.get());
    view.onContinue.run();
    assertEquals(1, completed.get());
    assertRewardsOnce();
  }

  @Test
  void shouldKeepLegacyContinueAvailableWithoutView() {
    settle();
    assertFalse(acquisitions.show(null, session::complete, () -> {}));
    assertTrue(session.complete());
    assertEquals(1, completed.get());
    assertRewardsOnce();
  }

  @Test
  void shouldIgnoreLateCallbacksAfterDisposalWithoutCompleting() {
    settle();
    assertTrue(acquisitions.show(view, session::complete, () -> fail("Disposed")));
    acquisitions.close();
    acquisitions.close();
    view.onContinue.run();
    view.onClose.run();
    assertEquals(0, completed.get());
    assertFalse(session.isCompleted());
    assertEquals(1, view.hidden);
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    assertRewardsOnce();
  }

  @Test
  void shouldReuseAdapterAndRefuseReplacingBusyView() {
    settle();
    assertSame(acquisitions, session.getAcquisitionSession(mock(CardService.class)));
    assertTrue(acquisitions.show(view, session::complete, () -> {}));
    assertFalse(acquisitions.show(view, () -> fail("Replaced callback"), () -> {}));
    TestView replacement = new TestView();
    assertFalse(acquisitions.show(replacement, () -> fail("Replaced view"), () -> {}));
    assertEquals(0, replacement.shown);
    view.onContinue.run();
    assertEquals(1, completed.get());
    assertRewardsOnce();
  }

  @Test
  void shouldInvalidateOverlayWhenOldContinueCompletesSession() {
    settle();
    assertTrue(acquisitions.show(view, session::complete, () -> fail("Stale")));
    assertTrue(session.complete());
    view.onContinue.run();
    view.onClose.run();
    assertEquals(1, completed.get());
    assertRewardsOnce();
  }

  @Test
  void shouldNotShowPendingFailedOrLeaveResults() {
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    player.failNextCurrencyUpdate();
    assertFalse(session.resolveChoice("accept").isSuccess());
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    assertTrue(session.resolveChoice("leave").isSuccess());
    assertFalse(acquisitions.show(view, session::complete, () -> {}));
    assertEquals(0, view.shown);
    assertTrue(state.getOwnedItems().isEmpty());
    assertTrue(deck.getCardIds().isEmpty());
    assertTrue(session.complete());
  }

  @Test
  void shouldKeepSettlementAfterMetadataFailureAndAllowResultRetry() {
    CardService cards = mock(CardService.class);
    ChanceEncounterSession active =
        new ChanceEncounterSession(
            2,
            session.getEncounter(),
            new ChanceOutcomeApplier(
                player,
                new MockCardCatalogGateway("bandage"),
                deck,
                new PlayerRunStateItemAdapter(state)),
            (node, success) -> completed.incrementAndGet());
    ChanceAcquisitionSession activeAcquisitions = active.getAcquisitionSession(cards);
    assertTrue(active.resolveChoice("accept").isSuccess());
    when(cards.getCard("bandage")).thenThrow(new IllegalStateException("Metadata unavailable"));
    assertFalse(activeAcquisitions.show(view, active::complete, () -> {}));
    assertEquals(0, view.shown);
    assertFalse(active.isCompleted());
    assertRewardsOnce();
    doReturn(Optional.empty()).when(cards).getCard("bandage");
    assertTrue(activeAcquisitions.show(view, active::complete, () -> {}));
    assertSame(view.result, activeAcquisitions.getResult().orElseThrow());
    view.onContinue.run();
    assertEquals(1, completed.get());
    assertRewardsOnce();
  }

  private static final class TestView implements AcquisitionView {
    private Runnable onContinue;
    private Runnable onClose;
    private AcquisitionResult result;
    private int shown;
    private int hidden;
    private boolean failShow;

    @Override
    public void show(AcquisitionResult result, Runnable onContinue, Runnable onClose) {
      this.result = result;
      this.onContinue = onContinue;
      this.onClose = onClose;
      shown++;
      if (failShow) throw new IllegalStateException("Display unavailable");
    }

    @Override
    public void hide() {
      hidden++;
    }
  }
}
