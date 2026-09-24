package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GameStateSnapshotProviderTest {

  @Test
  void capturesPlayerHealthAndGold() {
    PlayerRunState playerState = new PlayerRunState(80, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    SaveGameData data =
        new GameStateSnapshotProvider(
                playerState,
                deck,
                runState,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault())
            .capture();

    assertEquals(80, data.player.currentHealth);
    assertEquals(100, data.player.maxHealth);
    assertEquals(50, data.player.gold);
  }

  @Test
  void capturesMapNodesWithConnectionIdsNotObjectReferences() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    SaveGameData data =
        new GameStateSnapshotProvider(
                playerState,
                deck,
                runState,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault())
            .capture();

    assertEquals(1, data.map.nodes.size());
    assertEquals(0, data.map.nodes.get(0).nodeId);
  }

  @Test
  void capturesOnlyDiscoveredBestiaryProgress() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();
    BestiaryService bestiary = BestiaryService.loadDefault();
    bestiary.recordEncountered("lesser_shade");
    bestiary.recordDefeated("boss_knight");

    SaveGameData data =
        new GameStateSnapshotProvider(
                playerState, deck, runState, bestiary, CardDiscoveryService.loadDefault())
            .capture();

    assertEquals(2, data.progress.bestiary.size());
    assertEquals("lesser_shade", data.progress.bestiary.get(0).enemyId);
    assertEquals(BestiaryUnlockState.ENCOUNTERED.name(), data.progress.bestiary.get(0).unlockState);
    assertEquals("boss_knight", data.progress.bestiary.get(1).enemyId);
    assertEquals(BestiaryUnlockState.DEFEATED.name(), data.progress.bestiary.get(1).unlockState);
  }

  @Test
  void capturesPietyAsCurrentNodeHeightForSchemaCompleteness() {
    // Regression test for PR #303 review: confirms a non-zero current-node height is actually
    // captured into PlayerSaveData.piety. Note this field is snapshot-only (see the doc comment
    // on capturePlayer()) -- MapGraph itself, not this field, is what SaveGameRestoreService
    // restores and what PlayerStatsTopDisplay reads live, so this test only verifies the capture
    // side, not a restore round-trip.
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();

    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(21, new MapNode(21, RoomType.COMBAT)); // height = 21 / MAP_WIDTH(7) = 3
    MapGraph mapGraph = new MapGraph(nodes, false);
    mapGraph.restoreCurrentNode(21);
    RunState runState = new RunState();
    runState.setMapGraph(mapGraph);

    SaveGameData data =
        new GameStateSnapshotProvider(
                playerState,
                deck,
                runState,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault())
            .capture();

    assertEquals(3, data.player.piety);
  }

  @Test
  void rejectsMissingBestiaryService() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GameStateSnapshotProvider(
                playerState, deck, runState, null, CardDiscoveryService.loadDefault()));
  }

  @Test
  void capturesOnlySeenCardProgress() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();
    runState.restoreEncounterSeed(42L);
    CardDiscoveryService cards = CardDiscoveryService.loadDefault();
    cards.recordSeen("strike");

    SaveGameData data =
        new GameStateSnapshotProvider(
                playerState, deck, runState, BestiaryService.loadDefault(), cards)
            .capture();

    assertEquals(1, data.progress.cards.size());
    assertEquals("strike", data.progress.cards.get(0).cardId);
    assertEquals(CardUnlockState.SEEN.name(), data.progress.cards.get(0).unlockState);
    assertEquals(42L, data.progress.encounterSeed);
  }

  @Test
  void rejectsMissingCardDiscoveryService() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GameStateSnapshotProvider(
                playerState, deck, runState, BestiaryService.loadDefault(), null));
  }

  private RunState buildRunStateWithSingleNode() {
    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(0, new MapNode(0, RoomType.COMBAT));
    MapGraph mapGraph = new MapGraph(nodes, false);
    RunState runState = new RunState();
    runState.setMapGraph(mapGraph);
    return runState;
  }
}
