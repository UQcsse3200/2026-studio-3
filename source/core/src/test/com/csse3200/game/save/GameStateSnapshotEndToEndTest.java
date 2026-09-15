package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGenerationController;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves the full save pipeline works against real game objects, not just isolated units: a live
 * player/deck/map is captured through {@link GameStateSnapshotProvider}, written to an actual JSON
 * file via {@link SaveGameService}, then read back and verified to match — including a full restore
 * into fresh objects, as though the game relaunched.
 */
@ExtendWith(GameExtension.class)
class GameStateSnapshotEndToEndTest {

  @TempDir Path temporaryDirectory;

  private SaveGameService saveGameService;
  private BestiaryService bestiary;

  @BeforeEach
  void setUp() {
    PlayerRunState playerState = new PlayerRunState(65, 100, 120);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithConnectedNodes();
    bestiary = BestiaryService.loadDefault();
    bestiary.recordDefeated("lesser_shade");
    bestiary.recordEncountered("boss_knight");

    GameStateSnapshotProvider provider =
        new GameStateSnapshotProvider(playerState, deck, runState, bestiary);
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

  @Test
  void savesAndLoadsBestiaryUnlockProgress() {
    saveGameService.saveGame(1);
    LoadResult loadResult = saveGameService.loadGame(1);

    assertEquals(2, loadResult.data().progress.bestiary.size());
    assertEquals("lesser_shade", loadResult.data().progress.bestiary.get(0).enemyId);
    assertEquals("DEFEATED", loadResult.data().progress.bestiary.get(0).unlockState);
    assertEquals("boss_knight", loadResult.data().progress.bestiary.get(1).enemyId);
    assertEquals("ENCOUNTERED", loadResult.data().progress.bestiary.get(1).unlockState);
  }

  @Test
  void saveCloseRelaunchAndRestoreRecoversTheWholeRun() {
    assertTrue(saveGameService.saveGame(1).success());
    assertTrue(temporaryDirectory.resolve("slot-1.json").toFile().isFile());

    // Fresh objects simulate closing the game and starting a new process before loading.
    PlayerRunState restoredPlayerState = new PlayerRunState(1, 10, 0);
    PlayerDeck restoredDeck = PlayerDeckFactory.createStarterDeck();
    restoredDeck.clear();
    RunState restoredRunState = new RunState();
    BestiaryService restoredBestiary = BestiaryService.loadDefault();
    SaveGameService relaunchedSaveGameService =
        new SaveGameService(
            new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile())));

    LoadResult loadResult = relaunchedSaveGameService.loadGame(1);
    assertTrue(loadResult.success());

    RestoreResult restoreResult =
        new SaveGameRestoreService(
                restoredPlayerState, restoredDeck, restoredRunState, restoredBestiary)
            .restore(loadResult.data());

    assertTrue(restoreResult.success());
    assertEquals("MAP", restoreResult.resumeScreen());
    assertEquals(65, restoredPlayerState.getCurrentHealth());
    assertEquals(100, restoredPlayerState.getMaxHealth());
    assertEquals(120, restoredPlayerState.getGold());
    assertEquals(PlayerDeckFactory.getStarterDeckCardIds(), restoredDeck.getCardIds());
    assertEquals(0, restoredRunState.getMapGraph().getCurrentNode().getNodeId());
    assertEquals(NodeState.AVAILABLE, restoredRunState.getMapGraph().getNode(1).getState());
    assertEquals(
        BestiaryUnlockState.DEFEATED, restoredBestiary.getProgressSnapshot().get("lesser_shade"));
    assertEquals(
        BestiaryUnlockState.ENCOUNTERED, restoredBestiary.getProgressSnapshot().get("boss_knight"));
  }

  @Test
  void saveRestorePreservesGeneratedMapTopologyAndNodeStates() {
    PlayerRunState playerState = new PlayerRunState(44, 90, 33);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = new RunState();
    BestiaryService bestiaryService = BestiaryService.loadDefault();
    MapGraph generatedMap = new MapGenerationController().getMap();
    MapNode startNode =
        generatedMap.getNodesByHeight(1).stream()
            .min((first, second) -> Integer.compare(first.getNodeId(), second.getNodeId()))
            .orElseThrow();
    assertTrue(runState.startRun(generatedMap, startNode.getNodeId()));

    SaveGameService generatedSaveGameService =
        new SaveGameService(
            new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile())),
            new GameStateSnapshotProvider(playerState, deck, runState, bestiaryService));
    assertTrue(generatedSaveGameService.saveGame(2).success());

    LoadResult loadResult =
        new SaveGameService(new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile())))
            .loadGame(2);
    assertTrue(loadResult.success());

    RunState restoredRunState = new RunState();
    RestoreResult restoreResult =
        new SaveGameRestoreService(
                new PlayerRunState(1, 10, 0),
                PlayerDeckFactory.createStarterDeck(),
                restoredRunState,
                BestiaryService.loadDefault())
            .restore(loadResult.data());

    assertTrue(restoreResult.success());
    MapGraph restoredMap = restoredRunState.getMapGraph();
    assertEquals(generatedMap.getNodes().keySet(), restoredMap.getNodes().keySet());
    assertEquals(
        generatedMap.getCurrentNode().getNodeId(), restoredMap.getCurrentNode().getNodeId());
    assertEquals(nodeStatesById(generatedMap), nodeStatesById(restoredMap));
    assertEquals(connectionIdsByNodeId(generatedMap), connectionIdsByNodeId(restoredMap));
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

  private Map<Integer, NodeState> nodeStatesById(MapGraph graph) {
    return graph.getNodes().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getState()));
  }

  private Map<Integer, Set<Integer>> connectionIdsByNodeId(MapGraph graph) {
    return graph.getNodes().entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    entry.getValue().getConnections().stream()
                        .map(MapNode::getNodeId)
                        .collect(Collectors.toCollection(TreeSet::new))));
  }
}
