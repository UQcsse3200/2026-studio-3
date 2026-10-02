package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BattleEncounterSelectorTest {

  private static final long RUN_SEED = 1234L;

  private static RunState runAt(int nodeId, RoomType type) {
    return runAt(nodeId, type, RUN_SEED);
  }

  private static RunState runAt(int nodeId, RoomType type, long runSeed) {
    MapGraph graph = new MapGraph(Map.of(nodeId, new MapNode(nodeId, type)), false);
    RunState runState = new RunState();
    runState.restoreRun(graph, nodeId);
    runState.restoreEncounterSeed(runSeed);
    return runState;
  }

  @Test
  void shouldTreatMissingNodeAsCombat() {
    RunState runState = new RunState();

    assertEquals(RoomType.COMBAT, BattleEncounterSelector.currentRoomType(runState));
    assertEquals(0L, BattleEncounterSelector.seedFor(runState));
  }

  @Test
  void shouldReadActiveNodeRoomType() {
    RunState runState = runAt(12, RoomType.ELITE);

    assertEquals(RoomType.ELITE, BattleEncounterSelector.currentRoomType(runState));
  }

  @Test
  void shouldGiveEachNodeItsOwnSeed() {
    long first = BattleEncounterSelector.seedFor(runAt(12, RoomType.COMBAT));
    long second = BattleEncounterSelector.seedFor(runAt(13, RoomType.COMBAT));

    assertNotEquals(first, second);
  }

  @Test
  void shouldGiveSameNodeDifferentSeedsInDifferentRuns() {
    long firstRun = BattleEncounterSelector.seedFor(runAt(12, RoomType.COMBAT, 1L));
    long secondRun = BattleEncounterSelector.seedFor(runAt(12, RoomType.COMBAT, 2L));

    assertNotEquals(firstRun, secondRun);
  }

  @Test
  void shouldKeepEncounterAfterReload() {
    List<String> beforeQuitting =
        BattleEncounterSelector.enemiesFor(runAt(12, RoomType.ELITE, 99L));
    List<String> afterReloading =
        BattleEncounterSelector.enemiesFor(runAt(12, RoomType.ELITE, 99L));

    assertEquals(beforeQuitting, afterReloading);
  }

  @Test
  void shouldFallBackToNodeIdWithoutRunSeed() {
    RunState runState = new RunState();
    runState.setMapGraph(new MapGraph(Map.of(12, new MapNode(12, RoomType.COMBAT)), false));
    runState.enterEncounter(12);

    assertEquals(12L, BattleEncounterSelector.seedFor(runState));
  }

  @Test
  void shouldTreatUnknownActiveNodeAsCombat() {
    RunState runState = runAt(12, RoomType.ELITE);
    runState.enterEncounter(99);

    assertEquals(RoomType.COMBAT, BattleEncounterSelector.currentRoomType(runState));
  }

  @Test
  void shouldPickBossForFinalRoom() {
    List<String> enemies = BattleEncounterSelector.enemiesFor(runAt(70, RoomType.FINAL));

    assertEquals(List.of("boss_knight"), enemies);
  }

  @Test
  void shouldPickEliteLineUpForEliteRoom() {
    List<String> enemies = BattleEncounterSelector.enemiesFor(runAt(12, RoomType.ELITE));

    assertTrue(
        Set.of("tomb_guardian", "void_knight").contains(enemies.get(0)),
        "Elite room gave " + enemies);
  }

  @Test
  void shouldFallBackToDefaultLineUpForStartRoom() {
    List<String> enemies = BattleEncounterSelector.enemiesFor(runAt(0, RoomType.START));

    assertEquals(EncounterComposer.DEFAULT_ENEMIES, enemies);
  }

  @Test
  void shouldNeverPutBossInOrdinaryRoom() {
    for (int nodeId = 7; nodeId < 70; nodeId++) {
      List<String> enemies = BattleEncounterSelector.enemiesFor(runAt(nodeId, RoomType.COMBAT));

      assertFalse(enemies.contains("boss_knight"), "Node " + nodeId + " gave " + enemies);
    }
  }
}
