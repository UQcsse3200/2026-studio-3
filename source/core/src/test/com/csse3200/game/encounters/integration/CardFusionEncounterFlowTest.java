package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.fusion.CardFusionService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.chance.ChanceBehaviourResult;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CardFusionEncounterFlowTest {
  @Test
  void shouldFuseAndCompleteDelegatedEvent() {
    CardService cardService = cardService();
    RunState runState = runStateWithCommonCards(cardService);
    RecordingCallback callback = new RecordingCallback();
    CardFusionEncounterFlow flow =
        new CardFusionEncounterFlow(
            delegatedSession(callback),
            runState,
            new CardFusionService(cardService, new Random(1L)));

    List<String> selectedIds =
        flow.getEligibleCards().stream().map(CardInstance::instanceId).toList();

    assertTrue(flow.fuse(selectedIds).successful());
    assertTrue(runState.hasUsedCardFusion());
    assertEquals(1, runState.getOrCreatePlayerDeck(cardService).getCards().size());
    assertEquals(1, callback.count);
    assertTrue(callback.success);
  }

  @Test
  void shouldLeaveAndCompleteEventWithoutMutatingDeck() {
    CardService cardService = cardService();
    RunState runState = runStateWithCommonCards(cardService);
    RecordingCallback callback = new RecordingCallback();
    CardFusionEncounterFlow flow =
        new CardFusionEncounterFlow(
            delegatedSession(callback),
            runState,
            new CardFusionService(cardService, new Random(1L)));

    assertTrue(flow.leave());
    assertFalse(runState.hasUsedCardFusion());
    assertEquals(3, runState.getOrCreatePlayerDeck(cardService).getCards().size());
    assertEquals(1, callback.count);
    assertTrue(callback.success);
  }

  private static ChanceEncounterSession delegatedSession(RecordingCallback callback) {
    ChanceEncounter encounter =
        new ChanceEncounter(
            "card-fusion",
            "Fuse cards.",
            List.of(new ChanceChoice("fuse", "Fuse.", new ChanceOutcome(0, 0))));
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            7,
            encounter,
            choice -> ChanceBehaviourResult.delegated(),
            new ChanceOutcomeApplier(new MockPlayerStateGateway(100, 50)),
            callback);
    session.resolveChoice("fuse");
    return session;
  }

  private static RunState runStateWithCommonCards(CardService cardService) {
    RunState runState = new RunState();
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    deck.clear();
    deck.addCard("strike");
    deck.addCard("strike");
    deck.addCard("strike");
    return runState;
  }

  private static CardService cardService() {
    return new CardLibrary(CardConfigLoader.loadCards());
  }

  private static final class RecordingCallback implements com.csse3200.game.maps.EncounterCallback {
    private int count;
    private boolean success;

    @Override
    public void onEncounterComplete(Integer nodeId, boolean success) {
      count++;
      this.success = success;
    }
  }
}
