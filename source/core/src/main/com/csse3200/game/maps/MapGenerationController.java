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
   * // TODO: need to implement starting node
   * // TODO: would like to have configurable constraints (enemy first, no back to
   * back shops/events )
   * // TODO: would like weights of room types on a single path to be considered
   * in some way
   */
  private int generatePathing() {

    List<MapNode> row = map.getNodesByHeight(MapGenerationConfig.MAP_HEIGHT - 1);
    MapNode finalNode = map.getNode(MapGenerationConfig.MAX_NODE_COUNT);

    int pathCount = rand.nextInt(3, row.size() - 1);
    pruneRandomNodes(row, pathCount);

    Set<MapNode> visited = new HashSet<>();
    List<MapNode> oldNodes = new ArrayList<>();

    // generates initial no. of paths from final node
    for (MapNode child : row) {

      if (child == null) {
        return -1;
      }

      map.connectNodes(finalNode, child);
      visited.add(child);
      oldNodes.add(child);
    }

    // begin to loop the bulk of connections down the tree
    for (int i = 2; i < MapGenerationConfig.MAP_HEIGHT; i++) {

      row = map.getNodesByHeight(MapGenerationConfig.MAP_HEIGHT - i);
      List<MapNode> newNodes = new ArrayList<>();

      for (MapNode parentNode : oldNodes) {
        MapNode child = chooseNextNode(parentNode, row, visited);
        if (child == null) {
          return -1;
        }

        map.connectNodes(parentNode, child);
        visited.add(child);
        newNodes.add(child);

        // creates random additional branches off of the paths for variety
        if (rand.nextInt(100) < MapGenerationConfig.BRANCH_CHANCE) {

          MapNode branch = chooseNextNode(parentNode, row, visited);

          if (branch != null) {
            map.connectNodes(parentNode, branch);

            visited.add(branch);
            newNodes.add(branch);
          }
        }
      }
      oldNodes = newNodes;
    }
    pruneUnconnectedMapGraphNodes();
    return 0;
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
   * Heuristic helper function for map generation. Finds a random node in range
   * that hasn't already been visited.
   *
   * @param parentNode Chosen node where heuristic will be calculated from
   *
   * @param row        Chosen row the node must be connected to (can be above or
   *                   below)
   *
   * @param visited    The set of nodes that have already been visited by the
   *                   branches
   */
  private MapNode chooseNextNode(MapNode parentNode, List<MapNode> row, Set<MapNode> visited) {

    int currentPos = parentNode.getNodeId() % MapGenerationConfig.MAP_WIDTH;

    List<MapNode> inRange = getNodesInRange(currentPos, row);

    inRange.removeIf(visited::contains);

    if (inRange.isEmpty()) {
      return null;
    }

    return inRange.get(rand.nextInt(inRange.size()));
  }

  /**
   * Returns a list of nodes that are within the given x coordinate range of a
   * provided node on a neighboring row.
   *
   *
   * @param nodePos The x-coordinate of the node that is being ranged from.
   *
   * @param row     The row nodes should be trying to reach.
   */
  private List<MapNode> getNodesInRange(int nodePos, List<MapNode> row) {

    List<MapNode> inRange = new ArrayList<>();

    for (MapNode node : row) {

      int childPos = node.getNodeId() % MapGenerationConfig.MAP_WIDTH;

      if (Math.abs(nodePos - childPos) <= 1) {
        inRange.add(node);
      }
    }
    return inRange;
  }

  /**
   * Removes random nodes from a list. Does not remove from MapGraph state.
   *
   * @param nodelist List of nodes to be pruned
   * @param count    Number of nodes to be pruned
   */
  private void pruneRandomNodes(List<MapNode> nodelist, int count) {

    for (int i = 0; i < count; i++) {
      nodelist.remove(rand.nextInt(0, nodelist.size() - 1));
    }
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
