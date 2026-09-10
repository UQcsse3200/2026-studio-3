package com.csse3200.game.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapGenerationController {

  private MapGraph map;
  private final Random rand;
  private final MapGenerationConfig config;

  public MapGenerationController() {

    this.config = new MapGenerationConfig();
    this.map = new MapGraph(NodePoolGenerator.generate(config));
    this.rand = new Random(config.getSeed());

    while (generatePathing() != 0) {
      clearConnections();
    }
  }

  public MapGenerationController(
      int totalNodeCount, int combatWeight, int eventWeight, int shopWeight) {

    this.config = new MapGenerationConfig(totalNodeCount, combatWeight, eventWeight, shopWeight);
    this.map = new MapGraph(NodePoolGenerator.generate(config));
    this.rand = new Random(config.getSeed());

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
   * <p>
   * // TODO: need to rebalance room types based on weights // TODO: would like to
   * have
   * configurable constraints (enemy first, no back to back shops/events )
   */
  private int generatePathing() {
    List<List<MapNode>> generatedPaths = initializePaths();

    if (generatedPaths.isEmpty()) {
      return -1;
    }

    for (List<MapNode> path : generatedPaths) {
      if (buildPath(path) == -1) {
        return -1;
      }
    }

    pruneUnconnectedMapGraphNodes();
    rebalanceRoomTypes();
    return 0;
  }

  // Builds out an individual path from start to final. Paths must first be
  // initialized from initializePaths().
  private int buildPath(List<MapNode> path) {

    for (int i = 2; i < MapGenerationConfig.MAP_HEIGHT + 1; i++) {

      MapNode prevNode = path.getLast();
      MapNode nextNode = chooseNextNode(prevNode);

      if (nextNode == null) {
        return -1;
      }

      map.connectNodes(prevNode, nextNode);

      // random chance to create branches to other paths
      if (rand.nextInt(100) < MapGenerationConfig.BRANCH_CHANCE) {

        MapNode branch = chooseNextNode(prevNode);

        if (branch != null && !branch.getConnections().isEmpty()) {

          map.connectNodes(prevNode, branch);
          path.add(branch);
        }
      }

      path.add(nextNode);
    }

    return 0;
  }

  /**
   * Heuristic function for map generation. Finds a random node in range that
   * hasn't already been
   * visited.
   *
   * @param parentNode Chosen node from which heuristic will be evaluated
   */
  private MapNode chooseNextNode(MapNode parentNode) {

    List<MapNode> validNodes = getNodesInRange(parentNode);

    for (MapNode node : getNodesInRange(parentNode)) {

      int distance = map.getRelativeNodePos(parentNode, node);
      if (distance == 0 && !node.getConnections().isEmpty()) {

        for (MapNode connected : node.getConnections()) { // prevent crossover X-like connections

          int connectionDistance = map.getRelativeNodePos(parentNode, connected);
          if (connectionDistance == -1) {

            validNodes.removeIf(neighbour -> map.getRelativeNodePos(parentNode, neighbour) == -1);
          }
          if (connectionDistance == 1) {

            validNodes.removeIf(neighbour -> map.getRelativeNodePos(parentNode, neighbour) == 1);
          }
        }
      }
    }

    if (validNodes.isEmpty()) {
      return null;
    }

    return validNodes.get(rand.nextInt(validNodes.size()));
  }

  private List<List<MapNode>> initializePaths() {

    List<List<MapNode>> generatedPaths = new ArrayList<>();

    int pathCount = rand.nextInt(3, 5);
    MapNode startNode = map.getNode(0);
    List<MapNode> firstRow = map.getNodesByHeight(1);

    for (int i = 0; i < pathCount; i++) {

      firstRow.remove(rand.nextInt(1, firstRow.size() - 1));
    }

    for (MapNode node : firstRow) {

      map.connectNodes(startNode, node);

      List<MapNode> path = new ArrayList<>();
      path.add(node);
      generatedPaths.add(path);
    }

    return generatedPaths;
  }

  private void rebalanceRoomTypes() {

    // for (int i = 0; i < combatCount; i++) {
    //
    // MapNode node = map.getNode(rand.nextInt(1, MapGenerationConfig.MAX_NODE_COUNT
    // - 1));
    // node.setRoomType(RoomType.COMBAT);
    // }
    // for (int i = 0; i < eventCount; i++) {
    //
    // MapNode node = map.getNode(rand.nextInt(1, MapGenerationConfig.MAX_NODE_COUNT
    // - 1));
    // node.setRoomType(RoomType.EVENT);
    // }
    // for (int i = 0; i < shopCount; i++) {
    //
    // MapNode node = map.getNode(rand.nextInt(1, MapGenerationConfig.MAX_NODE_COUNT
    // - 1));
    // node.setRoomType(RoomType.SHOP);
    // }
  }

  public MapGraph getMap() {
    return map;
  }

  /** Clears the connections of nodes on the graph. */
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
   *
   * <p>
   * TODO: consider moving this to MapGraph? just unsure about logistics
   */
  private void pruneUnconnectedMapGraphNodes() {
    if (!map.getNodes().isEmpty()) {
      map.getNodes().values().removeIf(node -> node.getConnections().isEmpty());
    }
  }
}
