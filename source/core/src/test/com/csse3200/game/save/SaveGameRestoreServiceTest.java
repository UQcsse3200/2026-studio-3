package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SaveGameRestoreServiceTest {
  private static final String STRIKE = "strike";
  private static final String DEFEND = "defend";
  private static final String BANDAGE = "bandage";

  @Test
  void restoresDuplicateCardsPreservingInstanceIdentityAndUpgradeLevel() {
    // Regression test requested in review (Ruitao, PR #236): captureDeck() previously only
    // serialised cardId, so restoring two copies of the same card — one base, one upgraded —
    // silently lost which was which. This proves instanceId and upgradeLevel both round-trip.
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = new PlayerDeck(upgradableTestCardService(), List.of());
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    BestiaryService bestiary = BestiaryService.loadDefault();

    SaveGameData saveData = validSaveData();
    saveData.deck =
        new DeckSaveData(
            List.of(
                new CardInstanceSaveData("copy-base", STRIKE, CardInstance.BASE_LEVEL),
                new CardInstanceSaveData("copy-upgraded", STRIKE, CardInstance.UPGRADED_LEVEL)));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
            .restore(saveData);

    assertTrue(result.success());
    List<CardInstance> restored = deck.getCards();
    assertEquals(2, restored.size());

    CardInstance baseCopy =
        restored.stream().filter(card -> card.instanceId().equals("copy-base")).findFirst().get();
    CardInstance upgradedCopy =
        restored.stream()
            .filter(card -> card.instanceId().equals("copy-upgraded"))
            .findFirst()
            .get();

    assertEquals(STRIKE, baseCopy.cardId());
    assertEquals(CardInstance.BASE_LEVEL, baseCopy.upgradeLevel());
    assertEquals(STRIKE, upgradedCopy.cardId());
    assertEquals(CardInstance.UPGRADED_LEVEL, upgradedCopy.upgradeLevel());
    assertNotEquals(baseCopy.instanceId(), upgradedCopy.instanceId());
  }

  @Test
  void restoresPlayerDeckAndMapState() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    BestiaryService bestiary = BestiaryService.loadDefault();
    bestiary.recordDefeated("lesser_shade");

    SaveGameData saveData = validSaveData();
    saveData.player = new PlayerSaveData(80, 100, 42, 0);
    saveData.deck = DeckSaveData.ofCardIds(List.of(DEFEND, BANDAGE));
    saveData.progress.resumeScreen = "MAP";
    saveData.progress.bestiary =
        List.of(
            new BestiaryProgressSaveData("boss_knight", "ENCOUNTERED"),
            new BestiaryProgressSaveData("retired_enemy", "DEFEATED"));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
            .restore(saveData);

    assertTrue(result.success());
    assertEquals("MAP", result.resumeScreen());
    assertEquals(80, playerState.getCurrentHealth());
    assertEquals(100, playerState.getMaxHealth());
    assertEquals(42, playerState.getGold());
    assertEquals(List.of(DEFEND, BANDAGE), cardIds(deck));
    assertNotNull(runState.getMapGraph());
    assertEquals(1, runState.getMapGraph().getCurrentNode().getNodeId());
    assertEquals(2, runState.getActiveNodeId());
    assertEquals(NodeState.COMPLETED, runState.getMapGraph().getNode(0).getState());
    assertEquals(NodeState.CURRENT, runState.getMapGraph().getNode(1).getState());
    assertEquals(BestiaryUnlockState.LOCKED, bestiary.getProgressSnapshot().get("lesser_shade"));
    assertEquals(
        BestiaryUnlockState.ENCOUNTERED, bestiary.getProgressSnapshot().get("boss_knight"));
    assertFalse(bestiary.getProgressSnapshot().containsKey("retired_enemy"));
  }

  @Test
  void rejectsInvalidDeckWithoutMutatingLiveState() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    BestiaryService bestiary = BestiaryService.loadDefault();

    SaveGameData saveData = validSaveData();
    saveData.player = new PlayerSaveData(80, 100, 42, 0);
    saveData.deck = DeckSaveData.ofCardIds(List.of("unknown_card"));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
            .restore(saveData);

    assertFalse(result.success());
    assertEquals(RestoreError.INVALID_DECK_STATE, result.error());
    assertEquals(12, playerState.getCurrentHealth());
    assertEquals(3, playerState.getGold());
    assertEquals(List.of(STRIKE), cardIds(deck));
    assertEquals(0, runState.getMapGraph().getCurrentNode().getNodeId());
  }

  @Test
  void rejectsInvalidMapWithoutMutatingLiveState() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    BestiaryService bestiary = BestiaryService.loadDefault();

    SaveGameData saveData = validSaveData();
    saveData.map.nodes.get(0).connectionIds.add(99);

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
            .restore(saveData);

    assertFalse(result.success());
    assertEquals(RestoreError.INVALID_MAP_STATE, result.error());
    assertEquals(12, playerState.getCurrentHealth());
    assertEquals(List.of(STRIKE), cardIds(deck));
    assertEquals(0, runState.getMapGraph().getCurrentNode().getNodeId());
  }

  @Test
  void rejectsInvalidBestiaryProgressWithoutMutatingLiveState() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    BestiaryService bestiary = BestiaryService.loadDefault();
    bestiary.recordDefeated("lesser_shade");

    SaveGameData saveData = validSaveData();
    saveData.player = new PlayerSaveData(80, 100, 42, 0);
    saveData.deck = DeckSaveData.ofCardIds(List.of(DEFEND));
    saveData.progress.bestiary =
        List.of(new BestiaryProgressSaveData("boss_knight", "NOT_A_STATE"));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
            .restore(saveData);

    assertFalse(result.success());
    assertEquals(RestoreError.INVALID_PROGRESS_STATE, result.error());
    assertEquals(12, playerState.getCurrentHealth());
    assertEquals(3, playerState.getGold());
    assertEquals(List.of(STRIKE), cardIds(deck));
    assertEquals(0, runState.getMapGraph().getCurrentNode().getNodeId());
    assertEquals(BestiaryUnlockState.DEFEATED, bestiary.getProgressSnapshot().get("lesser_shade"));
    assertEquals(BestiaryUnlockState.LOCKED, bestiary.getProgressSnapshot().get("boss_knight"));
  }

  @Test
  void rejectsBlankAndDuplicateBestiaryIdsWithoutMutatingLiveState() {
    List<List<BestiaryProgressSaveData>> invalidProgressCases =
        List.of(
            List.of(new BestiaryProgressSaveData(" ", "ENCOUNTERED")),
            List.of(
                new BestiaryProgressSaveData("boss_knight", "ENCOUNTERED"),
                new BestiaryProgressSaveData("boss_knight", "DEFEATED")));

    for (List<BestiaryProgressSaveData> invalidProgress : invalidProgressCases) {
      PlayerRunState playerState = new PlayerRunState(12, 50, 3);
      PlayerDeck deck = testDeck(List.of(STRIKE));
      RunState runState = new RunState();
      runState.startRun(existingMap(), 0);
      BestiaryService bestiary = BestiaryService.loadDefault();
      bestiary.recordDefeated("lesser_shade");

      SaveGameData saveData = validSaveData();
      saveData.player = new PlayerSaveData(80, 100, 42, 0);
      saveData.progress.bestiary = invalidProgress;

      RestoreResult result =
          new SaveGameRestoreService(
                  playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
              .restore(saveData);

      assertFalse(result.success());
      assertEquals(RestoreError.INVALID_PROGRESS_STATE, result.error());
      assertEquals(12, playerState.getCurrentHealth());
      assertEquals(3, playerState.getGold());
      assertEquals(
          BestiaryUnlockState.DEFEATED, bestiary.getProgressSnapshot().get("lesser_shade"));
      assertEquals(BestiaryUnlockState.LOCKED, bestiary.getProgressSnapshot().get("boss_knight"));
    }
  }

  @Test
  void restoresCardProgressAndSkipsUnknownIds() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    CardDiscoveryService cards = CardDiscoveryService.loadDefault();
    cards.recordSeen(DEFEND);

    SaveGameData saveData = validSaveData();
    saveData.progress.cards =
        List.of(
            new CardProgressSaveData(STRIKE, CardUnlockState.SEEN.name()),
            new CardProgressSaveData("retired_card", CardUnlockState.SEEN.name()));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, BestiaryService.loadDefault(), cards)
            .restore(saveData);

    assertTrue(result.success());
    assertEquals(CardUnlockState.SEEN, cards.getProgressSnapshot().get(STRIKE));
    assertEquals(CardUnlockState.LOCKED, cards.getProgressSnapshot().get(DEFEND));
    assertFalse(cards.getProgressSnapshot().containsKey("retired_card"));
  }

  @Test
  void restoringSaveDoesNotKeepStarterCardsSeenBeyondSavedProgress() {
    CardDiscoveryService cards = CardDiscoveryService.loadDefault();
    ServiceLocator.registerCardDiscoveryService(cards);
    CardLibrary cardLibrary = new CardLibrary(CardConfigLoader.loadCards());
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    RunState runState = new RunState();
    PlayerDeck deck = runState.createStarterDeckForNewRun(cardLibrary);
    runState.startRun(existingMap(), 0);
    assertTrue(
        PlayerDeckFactory.getStarterDeckCardIds().stream()
            .allMatch(id -> cards.getProgressSnapshot().get(id) == CardUnlockState.SEEN));

    SaveGameData saveData = validSaveData();
    saveData.progress.cards =
        List.of(new CardProgressSaveData(STRIKE, CardUnlockState.SEEN.name()));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, BestiaryService.loadDefault(), cards)
            .restore(saveData);

    assertTrue(result.success());
    assertEquals(CardUnlockState.SEEN, cards.getProgressSnapshot().get(STRIKE));
    assertEquals(CardUnlockState.LOCKED, cards.getProgressSnapshot().get(DEFEND));
    assertEquals(CardUnlockState.LOCKED, cards.getProgressSnapshot().get(BANDAGE));
    assertEquals(
        1,
        cards.getProgressSnapshot().values().stream()
            .filter(state -> state == CardUnlockState.SEEN)
            .count());
  }

  @Test
  void rejectsInvalidCardProgressWithoutMutatingLiveState() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);
    CardDiscoveryService cards = CardDiscoveryService.loadDefault();
    cards.recordSeen(STRIKE);

    SaveGameData saveData = validSaveData();
    saveData.progress.cards = List.of(new CardProgressSaveData(DEFEND, "NOT_A_STATE"));

    RestoreResult result =
        new SaveGameRestoreService(
                playerState, deck, runState, BestiaryService.loadDefault(), cards)
            .restore(saveData);

    assertFalse(result.success());
    assertEquals(RestoreError.INVALID_PROGRESS_STATE, result.error());
    assertEquals(CardUnlockState.SEEN, cards.getProgressSnapshot().get(STRIKE));
    assertEquals(CardUnlockState.LOCKED, cards.getProgressSnapshot().get(DEFEND));
  }

  @Test
  void validatesConstructorArguments() {
    PlayerRunState playerState = new PlayerRunState(12, 50, 3);
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    BestiaryService bestiary = BestiaryService.loadDefault();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SaveGameRestoreService(
                null, deck, runState, bestiary, CardDiscoveryService.loadDefault()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SaveGameRestoreService(
                playerState, null, runState, bestiary, CardDiscoveryService.loadDefault()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SaveGameRestoreService(
                playerState, deck, null, bestiary, CardDiscoveryService.loadDefault()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SaveGameRestoreService(
                playerState, deck, runState, null, CardDiscoveryService.loadDefault()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SaveGameRestoreService(playerState, deck, runState, bestiary, null));
  }

  private SaveGameData validSaveData() {
    SaveGameData data = new SaveGameData();
    data.player = new PlayerSaveData(30, 60, 10, 0);
    data.deck = DeckSaveData.ofCardIds(List.of(STRIKE));
    data.map =
        new MapSaveData(
            List.of(
                new MapNodeSaveData(
                    0, RoomType.COMBAT.name(), NodeState.COMPLETED.name(), List.of(1)),
                new MapNodeSaveData(
                    1, RoomType.SHOP.name(), NodeState.CURRENT.name(), List.of(0, 2)),
                new MapNodeSaveData(
                    2, RoomType.EVENT.name(), NodeState.AVAILABLE.name(), List.of(1))),
            1,
            2);
    data.progress = new ProgressSaveData("", "");
    return data;
  }

  private MapGraph existingMap() {
    MapNode node = new MapNode(0, RoomType.COMBAT);
    node.setState(NodeState.CURRENT);
    return new MapGraph(Map.of(0, node), false);
  }

  private PlayerDeck testDeck(List<String> cardIds) {
    return new PlayerDeck(TestCardService.withCards(STRIKE, DEFEND, BANDAGE), cardIds);
  }

  /**
   * A card catalogue where STRIKE has a real upgrade definition, for tests that need to restore an
   * upgraded copy — TestCardService.withCards() leaves every card's upgrade config null.
   */
  private CardService upgradableTestCardService() {
    CardConfig strike = new CardConfig();
    strike.id = STRIKE;
    strike.upgrade = new CardUpgradeConfig();
    Map<String, CardConfig> cards = new LinkedHashMap<>();
    cards.put(STRIKE, strike);
    return new CardService() {
      @Override
      public Optional<CardConfig> getCard(String cardId) {
        return Optional.ofNullable(cards.get(cardId));
      }

      @Override
      public List<CardConfig> getAllCards() {
        return List.copyOf(cards.values());
      }
    };
  }

  private static List<String> cardIds(PlayerDeck deck) {
    return deck.getCards().stream().map(CardInstance::cardId).toList();
  }
}
