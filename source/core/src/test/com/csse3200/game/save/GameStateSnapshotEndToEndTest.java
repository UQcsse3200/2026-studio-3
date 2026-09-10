package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves the full save pipeline works against real game objects, not just isolated units:
 * a live player/deck/map is captured through {@link GameStateSnapshotProvider}, written to an
 * actual JSON file via {@link SaveGameService}, then read back and verified to match.
 */
@ExtendWith(GameExtension.class)
class GameStateSnapshotEndToEndTest {

  @TempDir Path temporaryDirectory;

  private SaveGameService saveGameService;

  @BeforeEach
  void setUp() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(65, 5, 100))
            .addComponent(new InventoryComponent(120));
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithConnectedNodes();

    GameStateSnapshotProvider provider = new GameStateSnapshotProvider(player, deck, runState);
    JsonSaveGameRepository repository =
        new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile()));
    saveGameService = new SaveGameService(repository, provider);
  }

  @Test
  void savesAndLoadsRealPlayerStateCorrectly() {
    SaveResult saveResult = saveGameService.saveGame(1);
    assertTrue(saveResult.success(), "Save should succeed for a real captured run");

    LoadResult loadResult = saveGameService.loadGame(1);
    assertTrue(loadResult.success(), "Load should succeed after a successful save");

    SaveGameData loaded = loadResult.data();
    assertEquals(65, loaded.player.currentHealth);
    assertEquals(100, loaded.player.maxHealth);
    assertEquals(120, loaded.player.gold);
  }

  @Test
  void savesAndLoadsDeckCardIdsCorrectly() {
    saveGameService.saveGame(1);
    LoadResult loadResult = saveGameService.loadGame(1);

    assertEquals(PlayerDeckFactory.getStarterDeckCardIds(), loadResult.data().deck.cardIds);
  }

  @Test
  void savesAndLoadsMapConnectionsAsIdsNotObjectReferences() {
    saveGameService.saveGame(1);
    LoadResult loadResult = saveGameService.loadGame(1);

    MapSaveData map = loadResult.data().map;
    assertEquals(2, map.nodes.size());
    assertEquals(0, (int) map.currentNodeId);

    MapNodeSaveData nodeZero =
        map.nodes.stream().filter(n -> n.nodeId == 0).findFirst().orElseThrow();
    assertEquals(1, nodeZero.connectionIds.size());
    assertEquals(1, (int) nodeZero.connectionIds.get(0));
  }

  private RunState buildRunStateWithConnectedNodes() {
    Map<Integer, MapNode> nodes = new HashMap<>();
    MapNode nodeZero = new MapNode(0, RoomType.COMBAT);
    MapNode nodeOne = new MapNode(1, RoomType.SHOP);
    nodeZero.addConnection(nodeOne);
    nodes.put(0, nodeZero);
    nodes.put(1, nodeOne);

    MapGraph mapGraph = new MapGraph(nodes, false);
    RunState runState = new RunState();
    runState.startRun(mapGraph, 0);
    return runState;
  }
}
