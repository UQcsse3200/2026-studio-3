package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.chance.ChanceBehaviourResult;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChanceEncounterSessionTest {
  @Test
  void shouldRejectInvalidChoiceAndKeepSessionOpen() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    RecordingCallback callback = new RecordingCallback();
    ChanceEncounterSession session = createSession(player, callback);

    ChanceResolution result = session.resolveChoice("missing");

    assertEquals(ChanceResolution.Status.INVALID_CHOICE, result.getStatus());
    assertFalse(session.isResolved());
    assertFalse(session.isCompleted());
    assertEquals(100, player.getHealth());
    assertEquals(50, player.getCurrency());
    assertEquals(0, callback.count);
  }

  @Test
  void shouldRequireSuccessfulResolutionBeforeCompletion() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    RecordingCallback callback = new RecordingCallback();
    ChanceEncounterSession session = createSession(player, callback);

    assertFalse(session.complete());
    assertEquals(0, callback.count);
  }

  @Test
  void shouldApplyAndReportCompletionOnlyOnce() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    RecordingCallback callback = new RecordingCallback();
    ChanceEncounterSession session = createSession(player, callback);

    ChanceResolution result = session.resolveChoice("accept");

    assertTrue(result.isSuccess());
    assertEquals(-5, result.getOutcome().getHealthDelta());
    assertEquals(10, result.getOutcome().getCurrencyDelta());
    assertEquals(
        ChanceResolution.Status.ALREADY_RESOLVED, session.resolveChoice("accept").getStatus());
    assertTrue(session.complete());
    assertFalse(session.complete());

    assertEquals(95, player.getHealth());
    assertEquals(60, player.getCurrency());
    assertEquals(1, callback.count);
    assertEquals(1, callback.nodeId);
    assertTrue(callback.success);
  }

  @Test
  void shouldRetryRejectedCardOutcomeWithoutAccumulatingReward() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    player.rejectNextHealthUpdate();
    MockDeckGateway deck = new MockDeckGateway();
    ChanceEncounter encounter =
        new ChanceEncounter(
            "reward-event",
            "Reward event",
            List.of(new ChanceChoice("accept", "Accept", new ChanceOutcome(20, 0, "bandage"))));
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), deck),
            (nodeId, success) -> {});

    ChanceResolution firstAttempt = session.resolveChoice("accept");
    ChanceResolution retry = session.resolveChoice("accept");

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, firstAttempt.getStatus());
    assertTrue(retry.isSuccess());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(90, player.getHealth());
  }

  @Test
  void shouldApplyInjectedBehaviourOutcomeThroughExistingApplier() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    ChanceEncounter encounter =
        new ChanceEncounter(
            "runtime-event",
            "Runtime event",
            List.of(new ChanceChoice("roll", "Roll", new ChanceOutcome(0, 0))));
    ChanceOutcome runtimeOutcome = new ChanceOutcome(-15, 25);
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            choiceId -> {
              assertEquals("roll", choiceId);
              return ChanceBehaviourResult.outcome(runtimeOutcome);
            },
            new ChanceOutcomeApplier(player),
            (nodeId, success) -> {});

    ChanceResolution result = session.resolveChoice("roll");

    assertTrue(result.isSuccess());
    assertSame(runtimeOutcome, result.getOutcome());
    assertEquals(85, player.getHealth());
    assertEquals(75, player.getCurrency());
  }

  @Test
  void shouldKeepSessionOpenWhileDelegatedBehaviourIsPending() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    AtomicInteger resolutionCount = new AtomicInteger();
    ChanceEncounter encounter =
        new ChanceEncounter(
            "delegated-event",
            "Delegated event",
            List.of(new ChanceChoice("continue", "Continue", new ChanceOutcome(0, 0))));
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            choiceId -> {
              resolutionCount.incrementAndGet();
              return ChanceBehaviourResult.delegated();
            },
            new ChanceOutcomeApplier(player),
            (nodeId, success) -> {});

    ChanceResolution result = session.resolveChoice("continue");
    ChanceResolution repeatedAttempt = session.resolveChoice("continue");

    assertEquals(ChanceResolution.Status.DELEGATED, result.getStatus());
    assertEquals(ChanceResolution.Status.DELEGATED, repeatedAttempt.getStatus());
    assertEquals(1, resolutionCount.get());
    assertTrue(session.isAwaitingDelegatedCompletion());
    assertFalse(session.isResolved());
    assertFalse(session.complete());
    assertFalse(session.isCompleted());
    assertEquals(100, player.getHealth());
    assertEquals(50, player.getCurrency());
  }

  @Test
  void shouldCompleteDelegatedFlowAsASuccessfulMapEncounter() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    RecordingCallback callback = new RecordingCallback();
    ChanceEncounter encounter =
        new ChanceEncounter(
            "delegated-event",
            "Delegated event",
            List.of(new ChanceChoice("continue", "Continue", new ChanceOutcome(0, 0))));
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            choiceId -> ChanceBehaviourResult.delegated(),
            new ChanceOutcomeApplier(player),
            callback);

    session.resolveChoice("continue");

    assertTrue(session.completeDelegated());
    assertFalse(session.completeDelegated());
    assertTrue(session.isCompleted());
    assertEquals(1, callback.count);
    assertEquals(1, callback.nodeId);
    assertTrue(callback.success);
  }

  @Test
  void shouldKeepSessionOpenWithoutApplyingStateWhileStagedChoiceIsPending() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    AtomicInteger resolutionCount = new AtomicInteger();
    ChanceEncounter encounter =
        new ChanceEncounter(
            "staged-event",
            "Staged event",
            List.of(new ChanceChoice("continue", "Continue", new ChanceOutcome(0, 0))));
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            choiceId -> {
              resolutionCount.incrementAndGet();
              return ChanceBehaviourResult.awaitingChoice();
            },
            new ChanceOutcomeApplier(player),
            (nodeId, success) -> {});

    ChanceResolution first = session.resolveChoice("continue");
    ChanceResolution second = session.resolveChoice("continue");

    assertEquals(ChanceResolution.Status.AWAITING_CHOICE, first.getStatus());
    assertEquals(ChanceResolution.Status.AWAITING_CHOICE, second.getStatus());
    assertEquals(2, resolutionCount.get());
    assertFalse(session.isResolved());
    assertFalse(session.complete());
    assertEquals(100, player.getHealth());
    assertEquals(50, player.getCurrency());
  }

  private ChanceEncounterSession createSession(
      MockPlayerStateGateway player, RecordingCallback callback) {
    ChanceEncounter encounter =
        new ChanceEncounter(
            "test-event",
            "Test event",
            List.of(
                new ChanceChoice("accept", "Accept", new ChanceOutcome(-5, 10)),
                new ChanceChoice("leave", "Leave", new ChanceOutcome(0, 0))));
    return new ChanceEncounterSession(1, encounter, new ChanceOutcomeApplier(player), callback);
  }

  private static final class RecordingCallback implements com.csse3200.game.maps.EncounterCallback {
    private int count;
    private Integer nodeId;
    private boolean success;

    @Override
    public void onEncounterComplete(Integer nodeId, boolean success) {
      count++;
      this.nodeId = nodeId;
      this.success = success;
    }
  }
}
