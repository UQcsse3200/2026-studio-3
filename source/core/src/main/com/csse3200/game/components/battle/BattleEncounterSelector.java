package com.csse3200.game.components.battle;

import com.csse3200.game.entities.configs.EncounterConfigs;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.function.Predicate;

/** Works out which enemies the current map node should fight, using the state of the run. */
public final class BattleEncounterSelector {
  private BattleEncounterSelector() {}

  /**
   * Chooses the enemies for the node the player is entering.
   *
   * @param runState state of the current run
   * @return enemy ids in spawn order, never empty
   */
  public static List<String> enemiesFor(RunState runState) {
    EncounterConfigs table =
        FileLoader.readClass(EncounterConfigs.class, "configs/encounters.json");
    EnemyConfigs roster = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");
    Predicate<String> isKnownEnemy = roster == null ? id -> false : roster::contains;
    EncounterComposer composer = new EncounterComposer(table, isKnownEnemy);
    return composer.compose(
        currentRoomType(runState), runState.getMapProgression(), seedFor(runState));
  }

  /**
   * Gets the room type of the node the player is entering.
   *
   * @param runState state of the current run
   * @return the active node's room type, or {@link RoomType#COMBAT} if there is no active node
   */
  static RoomType currentRoomType(RunState runState) {
    MapNode node = activeNode(runState);
    return node == null ? RoomType.COMBAT : node.getRoomType();
  }

  /**
   * Gets the seed that fixes which encounter the active node gets.
   *
   * <p>Combines the run's seed with the node id, so a node keeps its encounter for the whole run,
   * including after a reload, while a new run places different encounters on the same node.
   *
   * @param runState state of the current run
   * @return a seed for the active node
   */
  static long seedFor(RunState runState) {
    Integer nodeId = runState.getActiveNodeId();
    Long runSeed = runState.getEncounterSeed();
    long node = nodeId == null ? 0L : nodeId;
    return runSeed == null ? node : runSeed * 31 + node;
  }

  private static MapNode activeNode(RunState runState) {
    Integer nodeId = runState.getActiveNodeId();
    MapGraph graph = runState.getMapGraph();
    if (nodeId == null || graph == null) {
      return null;
    }
    return graph.getNode(nodeId);
  }
}
