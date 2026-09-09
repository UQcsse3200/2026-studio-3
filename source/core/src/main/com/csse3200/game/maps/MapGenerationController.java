package com.csse3200.game.maps;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class MapGenerationController {

  private MapGraph map;
  private final Random rand = new Random();

  private final MapGenerationConfig config;

  // TODO: should use seed, either from config or self
  public MapGenerationController() {

    this.config = new MapGenerationConfig();
    this.map = new MapGraph(NodePoolGenerator.generate(config));

    while (generatePathing() != 0) {
      clearConnections();
    }
  }

  public MapGenerationController(int totalNodeCount, int combatWeight, int eventWeight, int shopWeight) {

    this.config = new MapGenerationConfig(totalNodeCount, combatWeight, eventWeight, shopWeight);
    this.map = new MapGraph(NodePoolGenerator.generate(config));

    while (generatePathing() != 0) {
      clearConnections();
    }
  }

  /**
   * Primary map generation function. The player is able to start from any of the
   * nodes at height =
   * 1. Distinct paths are generated and can have a chance to create random
   * branches if the option
   * is available.
   *
   * // TODO: need to resolve connections crossing over each other
   * // TODO: there should be less variance in the generation, too many extreme
   * cases
   * // TODO: need to rebalance room types based on weights
   * // TODO: would like to have configurable constraints (enemy first, no back to
   * back shops/events )
   * // TODO: would like weights of room types on a single path to be considered
   * in some way
   */
  private int generatePathing() {
    List<List<MapNode>> generatedPaths = initializePaths();

    if (generatedPaths.isEmpty()) {
      return -1;
    }

    return 0;
  }

  private void buildPath(List<MapNode> path) {

  }

  private List<List<MapNode>> initializePaths() {

    List<List<MapNode>> generatedPaths = new ArrayList<>();

    int pathCount = rand.nextInt(3, 5);
    MapNode startNode = map.getNode(0);
    List<MapNode> firstRow = map.getNodesByHeight(1);

    for (int i = 0; i < pathCount; i++) {

      firstRow.remove(rand.nextInt(0, firstRow.size()));
    }

    for (MapNode node : firstRow) {

      map.connectNodes(startNode, node);

      List<MapNode> path = new ArrayList<>();
      path.add(node);
      generatedPaths.add(path);
    }

    return generatedPaths;
  }

  public MapGraph getMap() {
    return map;
  }

  /**
   * Clears the connections of nodes on the graph.
   *
   */
  private void clearConnections() {
    for (MapNode node : map.getNodes().values()) {
      node.getConnections().clear();
    }
  }

  /**
   * Returns a the list of nodes that a given node is within range to connect
   * with.
   *
   * @param node Targeted node for getting nodes in range
   */
  private List<MapNode> getNodesInRange(MapNode node) {

    List<MapNode> inRange = new ArrayList<>();
    List<MapNode> row = map.getNodesByHeight(node.getHeight() + 1);

    for (MapNode child : row) {

      int pos = Math.abs(map.getRelativeNodePos(node, child));

      if (pos <= 1) {
        inRange.add(child);
      }
    }
    return inRange;
  }

  /**
   * Removes all unconnected nodes from the MapGraph. Only called as the final
   * step of generation.
   */
  private void pruneUnconnectedMapGraphNodes() {
    if (!map.getNodes().isEmpty()) {
      map.getNodes().values().removeIf(node -> node.getConnections().isEmpty());
    }
  }
}
