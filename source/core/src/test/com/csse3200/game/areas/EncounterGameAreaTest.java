package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.fusion.CardFusionFailureReason;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.chance.CardFusionEncounterBehaviour;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceEncounterBehaviourFactory;
import com.csse3200.game.chance.ChanceEncounterFactory;
import com.csse3200.game.encounters.integration.CardFusionEncounterFlow;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceResolution;
import com.csse3200.game.encounters.integration.EncounterFlowController;
import com.csse3200.game.encounters.integration.ShopTransactionGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.shop.ShopInventoryGenerator;
import com.csse3200.game.shop.ShopService;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EncounterGameAreaTest {
  @Test
  void shouldRouteEventNodeToChanceEncounter() {
    assertEquals(
        EncounterFlowController.EncounterType.CHANCE,
        EncounterGameArea.encounterTypeFor(RoomType.EVENT));
  }

  @Test
  void shouldRouteShopNodeToShopEncounter() {
    assertEquals(
        EncounterFlowController.EncounterType.SHOP,
        EncounterGameArea.encounterTypeFor(RoomType.SHOP));
  }

  @Test
  void shouldRejectCombatAndMissingRoomTypes() {
    assertThrows(
        IllegalArgumentException.class, () -> EncounterGameArea.encounterTypeFor(RoomType.COMBAT));
    assertThrows(IllegalArgumentException.class, () -> EncounterGameArea.encounterTypeFor(null));
  }

  @Test
  void shouldBuildMapShopFromTheCardLibrary() {
    CardService cardLibrary = new CardLibrary(CardConfigLoader.loadCards());

    ShopService shop = EncounterGameArea.createMapShop(cardLibrary);

    assertEquals(ShopInventoryGenerator.DEFAULT_OFFER_COUNT, shop.getItems().size());
    Set<String> cardIds = new HashSet<>();
    shop.getItems()
        .forEach(
            item -> {
              assertTrue(cardIds.add(item.cardId));
              assertTrue(cardLibrary.getCard(item.cardId).isPresent());
            });
  }

  @Test
  void shouldUseTheActiveRunDeckAndCompleteAfterSuccessfulFusion() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    RunState runState = runWithThreeCommonCards(cardService);
    PlayerDeck sharedDeck = runState.getOrCreatePlayerDeck(cardService);
    AtomicInteger completions = new AtomicInteger();
    EncounterFlowController controller = controller(completions);
    ChanceEncounter encounter = cardFusionEncounter();
    ChanceEncounterSession session = startSession(controller, encounter, cardService);
    CardFusionEncounterFlow fusionFlow =
        EncounterGameArea.createCardFusionEncounterFlow(encounter, session, runState, cardService);

    assertEquals(ChanceResolution.Status.DELEGATED, session.resolveChoice("fuse").getStatus());
    List<String> selectedIds =
        fusionFlow.getEligibleCards().stream().map(CardInstance::instanceId).toList();
    assertTrue(fusionFlow.fuse(selectedIds).successful());

    assertSame(sharedDeck, runState.getOrCreatePlayerDeck(cardService));
    assertEquals(1, sharedDeck.size());
    assertTrue(runState.hasUsedCardFusion());
    assertEquals(1, completions.get());
    assertFalse(controller.isEncounterActive());
    assertThrows(IllegalStateException.class, () -> fusionFlow.fuse(selectedIds));
    assertEquals(1, completions.get());
  }

  @Test
  void shouldCompleteInitialLeaveWithoutUsingFusion() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    RunState runState = runWithThreeCommonCards(cardService);
    AtomicInteger completions = new AtomicInteger();
    EncounterFlowController controller = controller(completions);
    ChanceEncounter encounter = cardFusionEncounter();
    ChanceEncounterSession session = startSession(controller, encounter, cardService);
    EncounterGameArea.createCardFusionEncounterFlow(encounter, session, runState, cardService);

    assertTrue(session.resolveChoice("leave").isSuccess());
    assertTrue(session.complete());
    assertFalse(session.complete());
    assertEquals(3, runState.getOrCreatePlayerDeck(cardService).size());
    assertFalse(runState.hasUsedCardFusion());
    assertEquals(1, completions.get());
  }

  @Test
  void shouldKeepDelegatedFusionOpenAfterRetryableFailuresUntilLeave() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    RunState runState = runWithThreeCommonCards(cardService);
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    deck.addCard("poison_dagger");
    List<String> originalCards = deck.getCardIds();
    List<String> ids = deck.getCards().stream().map(CardInstance::instanceId).toList();
    AtomicInteger completions = new AtomicInteger();
    ChanceEncounter encounter = cardFusionEncounter();
    ChanceEncounterSession session = startSession(controller(completions), encounter, cardService);
    CardFusionEncounterFlow fusionFlow =
        EncounterGameArea.createCardFusionEncounterFlow(encounter, session, runState, cardService);

    session.resolveChoice("fuse");
    assertEquals(
        CardFusionFailureReason.INVALID_SELECTION,
        fusionFlow.fuse(List.of(ids.get(0), ids.get(0), ids.get(1))).failureReason());
    assertEquals(
        CardFusionFailureReason.CARD_NOT_IN_DECK,
        fusionFlow.fuse(List.of(ids.get(0), ids.get(1), "missing-instance")).failureReason());
    assertEquals(
        CardFusionFailureReason.CARD_NOT_COMMON,
        fusionFlow.fuse(List.of(ids.get(0), ids.get(1), ids.get(3))).failureReason());
    assertEquals(originalCards, deck.getCardIds());
    assertFalse(runState.hasUsedCardFusion());
    assertEquals(0, completions.get());
    assertTrue(session.isAwaitingDelegatedCompletion());

    assertTrue(fusionFlow.leave());
    assertEquals(1, completions.get());
    assertFalse(runState.hasUsedCardFusion());
    assertEquals(originalCards, deck.getCardIds());
  }

  @Test
  void shouldAllowDelegatedLeaveWhenFusionIsUnavailable() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    RunState runState = runWithThreeCommonCards(cardService);
    List<String> ids =
        runState.getOrCreatePlayerDeck(cardService).getCards().stream()
            .map(CardInstance::instanceId)
            .toList();
    runState.markCardFusionUsed();
    AtomicInteger completions = new AtomicInteger();
    ChanceEncounter encounter = cardFusionEncounter();
    ChanceEncounterSession session = startSession(controller(completions), encounter, cardService);
    CardFusionEncounterFlow fusionFlow =
        EncounterGameArea.createCardFusionEncounterFlow(encounter, session, runState, cardService);

    session.resolveChoice("fuse");
    assertEquals(
        CardFusionFailureReason.FUSION_ALREADY_USED, fusionFlow.fuse(ids).failureReason());
    assertEquals(0, completions.get());
    assertTrue(fusionFlow.leave());
    assertEquals(1, completions.get());
    assertEquals(3, runState.getOrCreatePlayerDeck(cardService).size());
  }

  @Test
  void shouldLeaveAfterNoRareRewardIsAvailable() {
    CardService fullCatalog = new CardLibrary(CardConfigLoader.loadCards());
    RunState runState = runWithThreeCommonCards(fullCatalog);
    CardService noRareCatalog = new CardLibrary(List.of(fullCatalog.getCard("strike").orElseThrow()));
    List<String> ids =
        runState.getOrCreatePlayerDeck(fullCatalog).getCards().stream()
            .map(CardInstance::instanceId)
            .toList();
    AtomicInteger completions = new AtomicInteger();
    ChanceEncounter encounter = cardFusionEncounter();
    ChanceEncounterSession session = startSession(controller(completions), encounter, fullCatalog);
    CardFusionEncounterFlow fusionFlow =
        EncounterGameArea.createCardFusionEncounterFlow(encounter, session, runState, noRareCatalog);

    session.resolveChoice("fuse");
    assertEquals(
        CardFusionFailureReason.NO_RARE_CARD_AVAILABLE, fusionFlow.fuse(ids).failureReason());
    assertEquals(0, completions.get());
    assertFalse(runState.hasUsedCardFusion());
    assertTrue(fusionFlow.leave());
    assertEquals(1, completions.get());
    assertEquals(3, runState.getOrCreatePlayerDeck(fullCatalog).size());
  }

  @Test
  void shouldNotCreateFusionFlowForAnOrdinaryEvent() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(0);
    ChanceEncounterSession session =
        startSession(controller(new AtomicInteger()), encounter, cardService);

    assertNull(
        EncounterGameArea.createCardFusionEncounterFlow(
            encounter, session, new RunState(), cardService));
  }

  private static ChanceEncounter cardFusionEncounter() {
    return ChanceEncounterFactory.createInitialEncounters().stream()
        .filter(encounter -> CardFusionEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId()))
        .findFirst()
        .orElseThrow();
  }

  private static RunState runWithThreeCommonCards(CardService cardService) {
    RunState runState = new RunState();
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    deck.clear();
    deck.addCards(List.of("strike", "strike", "strike"));
    return runState;
  }

  private static EncounterFlowController controller(AtomicInteger completions) {
    return new EncounterFlowController(
        new MockPlayerStateGateway(100, 50),
        mock(ShopTransactionGateway.class),
        (nodeId, success) -> completions.incrementAndGet());
  }

  private static ChanceEncounterSession startSession(
      EncounterFlowController controller, ChanceEncounter encounter, CardService cardService) {
    return controller.startChance(
        7,
        encounter,
        ChanceEncounterBehaviourFactory.create(encounter, new Random(266L), cardService));
  }
}
