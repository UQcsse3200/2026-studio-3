package com.csse3200.game.maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Represents the map graph containing all map nodes. */
public class MapGraph implements EncounterCallback {

  private Map<Integer, MapNode> nodes;
  private MapNode currentNode;

  public static final int MAP_WIDTH = 7;
  public static final int MAP_HEIGHT = 10;
  public static final int MAX_NODE_COUNT = MAP_WIDTH * MAP_HEIGHT;
  public static final int BRANCH_CHANCE = 10;
  private final Random random = new Random();

  /**
   * Creates a graph containing an existing node pool and runs procedural path generation over it.
   *
   * @param nodes nodes keyed by their unique identifiers
   */
  public MapGraph(Map<Integer, MapNode> nodes) {
    this(nodes, true);
  }

  /**
   * Creates a graph containing an existing node pool, optionally running procedural path
   * generation. Passing {@code false} builds the graph exactly as given, which lets callers set up
   * controlled maps (for example in tests) without triggering full generation.
   *
   * @param nodes nodes keyed by their unique identifiers
   * @param generate whether to run procedural path generation
   */
  public MapGraph(Map<Integer, MapNode> nodes, boolean generate) {
    this.nodes = new HashMap<>(nodes);

    if (generate) {
      while (generatePathing() != 0) {
        clearConnections();
      }
    }
  }

  /** Clears the connections of nodes on the graph. */
  private void clearConnections() {
    for (MapNode node : nodes.values()) {
      node.getConnections().clear();
    }
  }

  /**
   * Primary map generation function. The player is able to start from any of the nodes at height =
   * 1. Distinct paths are generated and can have a chance to create random branches if the option
   * is available.
   */
  private int generatePathing() {

    List<MapNode> row = getNodesByHeight(MAP_HEIGHT - 1);
    MapNode finalNode = getNode(MAX_NODE_COUNT);

    int pathCount = random.nextInt(3, row.size() - 1);
    pruneRandomNodes(row, pathCount);

    Set<MapNode> visited = new HashSet<>();
    List<MapNode> pathNodes = new ArrayList<>();

    // generates initial no. of paths from final node
    for (MapNode child : row) {

      if (child == null) {
        return -1;
      }

      connectNodes(finalNode, child);
      visited.add(child);
      pathNodes.add(child);
    }
    // begin to loop the bulk of connections down the tree
    for (int i = 2; i < MAP_HEIGHT; i++) {

      row = getNodesByHeight(MAP_HEIGHT - i);
      List<MapNode> newNodes = new ArrayList<>();

      for (MapNode parentNode : pathNodes) {
        MapNode child = chooseNextNode(parentNode, row, visited);
        if (child == null) {
          return -1;
        }

        connectNodes(parentNode, child);
        visited.add(child);
        newNodes.add(child);

        // creates random additional branches off of the paths for variety
        if (newNodes.size() < 6 && random.nextInt(100) < BRANCH_CHANCE) {

          MapNode branch = chooseNextNode(parentNode, row, visited);

          if (branch != null) {
            connectNodes(parentNode, branch);

            visited.add(branch);
            newNodes.add(branch);
          }
        }
      }
      pathNodes = newNodes;
    }
    pruneUnconnectedMapGraphNodes();
    return 0;
  }

  /**
   * Heuristic helper function for map generation. Finds a random node in range that hasn't already
   * been visited.
   *
   * @param parentNode Chosen node where heuristic will be calculated from
   * @param row Chosen row the node must be connected to (can be above or below)
   * @param visited The set of nodes that have already been visited by the branches
   */
  private MapNode chooseNextNode(MapNode parentNode, List<MapNode> row, Set<MapNode> visited) {

    int currentPos = parentNode.getNodeId() % MAP_WIDTH;

    List<MapNode> inRange = getNodesInRange(currentPos, row, 1);

    inRange.removeIf(visited::contains);

    if (inRange.isEmpty()) {
      return null;
    }

    return inRange.get(random.nextInt(inRange.size()));
  }

  /**
   * Returns a list of nodes that are within the given x coordinate range of a provided node on a
   * neighboring row.
   *
   * @param nodePos The x-coordinate of the node that is being ranged from.
   * @param row The row nodes should be trying to reach.
   * @param range The desired range of the nodes to be returned.
   */
  private List<MapNode> getNodesInRange(int nodePos, List<MapNode> row, int range) {

    List<MapNode> inRange = new ArrayList<>();

    for (MapNode node : row) {

      int childPos = node.getNodeId() % MAP_WIDTH;

      if (Math.abs(nodePos - childPos) <= range) {
        inRange.add(node);
      }
    }
    return inRange;
  }

  /**
   * Removes all unconnected nodes from the MapGraph. Only called as the final step of generation.
   */
  private void pruneUnconnectedMapGraphNodes() {
    if (!this.nodes.isEmpty()) {
      this.nodes.values().removeIf(node -> node.getConnections().isEmpty());
    }
  }

  /**
   * Removes random nodes from a list. Does not remove from MapGraph state.
   *
   * @param nodelist List of nodes to be pruned
   * @param count Number of nodes to be pruned
   */
  private void pruneRandomNodes(List<MapNode> nodelist, int count) {

    for (int i = 0; i < count; i++) {
      nodelist.remove(random.nextInt(0, nodelist.size() - 1));
    }
  }

  /**
   * Adds a node to the graph.
   *
   * @param node node to add
   */
  public void addNode(MapNode node) {
    nodes.put(node.getNodeId(), node);
  }

  /**
   * Adds a list of nodes to the graph. TO BE DELETED, JUST FOR TESTING
   *
   * @param nodeList nodes to add to the graph
   */
  public void addNodes(Map<Integer, MapNode> nodeList) {
    for (MapNode node : nodeList.values()) {
      addNode(node);
    }
  }

  /**
   * Gets a node by its id.
   *
   * @param nodeId node identifier
   * @return matching node
   */
  public MapNode getNode(Integer nodeId) {
    return nodes.get(nodeId);
  }

  /**
   * Gets the current node.
   *
   * @return current node or null if no current node is set
   */
  public MapNode getCurrentNode() {
    return currentNode;
  }

  /**
   * Restores the current node from trusted save/load code without applying normal movement rules.
   *
   * @param nodeId saved current node id, or null when no node is current
   * @return true when the node was restored, or false if the id is unknown
   */
  public boolean restoreCurrentNode(Integer nodeId) {
    if (nodeId == null) {
      currentNode = null;
      return true;
    }

    MapNode restoredCurrentNode = nodes.get(nodeId);
    if (restoredCurrentNode == null) {
      return false;
    }
    currentNode = restoredCurrentNode;
    return true;
  }

  /**
   * Gets all nodes in the graph.
   *
   * @return all map nodes
   */
  public Map<Integer, MapNode> getNodes() {
    return nodes;
  }

  /**
   * Gets all nodes in the given row of the MapGraph.
   *
   * @param height The chosen height/layer from which nodes are retrieved
   * @return all map nodes in the given row
   */
  public List<MapNode> getNodesByHeight(int height) {

    List<MapNode> result = new ArrayList<>();
    for (MapNode node : nodes.values()) {
      if (node.getHeight() == height) {
        result.add(node);
      }
    }
    return result;
  }

  /** Connects two nodes. */
  public void connectNodes(MapNode first, MapNode second) {

    if (first != null && second != null) {
      first.addConnection(second);
      second.addConnection(first);
    }
  }

  /**
   * Connects two nodes by id. Kept so callers outside this package can connect nodes without
   * looking them up first, which the encounter integration relies on.
   *
   * @param firstId id of the first node
   * @param secondId id of the second node
   */
  public void connectNodes(Integer firstId, Integer secondId) {
    connectNodes(nodes.get(firstId), nodes.get(secondId));
  }

  /**
   * Called after an encounter finishes.
   *
   * @param nodeId completed node id
   * @param success whether encounter completed successfully
   */
  public void completeNode(Integer nodeId, boolean success) {

    MapNode node = nodes.get(nodeId);

    if (node == null) {
      return;
    }

    if (success) {

      node.setState(NodeState.COMPLETED);

      for (MapNode connected : node.getConnections()) {
        if (connected.getState() == NodeState.LOCKED) {
          connected.setState(NodeState.AVAILABLE);
        }
      }
    }
  }

  /**
   * Get all nodes with the specified state
   *
   * @param state state to match
   * @return nodes with the specified state
   */
  public List<MapNode> getNodesByState(NodeState state) {
    List<MapNode> result = new ArrayList<>();

    for (MapNode node : nodes.values()) {
      if (node.getState() == state) {
        result.add(node);
      }
    }

    return result;
  }

  /**
   * Starts a run at the given node, unlocking its connections so there is somewhere to move.
   *
   * @param nodeId id of the node the player starts on
   * @return true if the node exists
   */
  public boolean startRun(Integer nodeId) {
    MapNode startNode = nodes.get(nodeId);

    if (startNode == null) {
      return false;
    }

    currentNode = startNode;
    startNode.setState(NodeState.CURRENT);

    for (MapNode connected : startNode.getConnections()) {
      if (connected.getState() == NodeState.LOCKED) {
        connected.setState(NodeState.AVAILABLE);
      }
    }

    return true;
  }

  /**
   * Checks if a move is valid and updates currentNode accordingly.
   *
   * @param nodeId id of the node to move to
   * @return true if the move is valid, false otherwise
   */
  public boolean moveToNode(Integer nodeId) {
    MapNode targetNode = nodes.get(nodeId);

    // targetNode must be connected to currentNode and must be available
    if (currentNode == null
        || targetNode == null
        || targetNode.getState() != NodeState.AVAILABLE
        || !currentNode.getConnections().contains(targetNode)) {
      return false;
    }

    // state of previous currentNode should be updated when its encounter completes.
    currentNode = targetNode;
    targetNode.setState(NodeState.CURRENT);

    return true;
  }

  @Override
  public void onEncounterComplete(Integer nodeId, boolean success) {
    completeNode(nodeId, success);
  }
}
