package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
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
        new GameStateSnapshotProvider(playerState, deck, runState, BestiaryService.loadDefault())
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
        new GameStateSnapshotProvider(playerState, deck, runState, BestiaryService.loadDefault())
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
        new GameStateSnapshotProvider(playerState, deck, runState, bestiary).capture();

    assertEquals(2, data.progress.bestiary.size());
    assertEquals("lesser_shade", data.progress.bestiary.get(0).enemyId);
    assertEquals(BestiaryUnlockState.ENCOUNTERED.name(), data.progress.bestiary.get(0).unlockState);
    assertEquals("boss_knight", data.progress.bestiary.get(1).enemyId);
    assertEquals(BestiaryUnlockState.DEFEATED.name(), data.progress.bestiary.get(1).unlockState);
  }

  @Test
  void rejectsMissingBestiaryService() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    assertThrows(
        IllegalArgumentException.class,
        () -> new GameStateSnapshotProvider(playerState, deck, runState, null));
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
