package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import java.util.List;
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

    assertTrue(session.resolveChoice("accept").isSuccess());
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
