package com.csse3200.game.maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents the map graph containing all map nodes. */
public class MapGraph implements EncounterCallback {

  private Map<Integer, MapNode> nodes;
  private MapNode currentNode;

  /**
   * Creates a graph containing an existing node pool and runs procedural path
   * generation over it.
   *
   * @param nodes nodes keyed by their unique identifiers
   */
  public MapGraph(Map<Integer, MapNode> nodes) {
    this(nodes, true);
  }

  /**
   * Creates a graph containing an existing node pool, optionally running
   * procedural path
   * generation. Passing {@code false} builds the graph exactly as given, which
   * lets callers set up
   * controlled maps (for example in tests) without triggering full generation.
   *
   * @param nodes    nodes keyed by their unique identifiers
   * @param generate whether to run procedural path generation
   */
  public MapGraph(Map<Integer, MapNode> nodes, boolean generate) {
    this.nodes = new HashMap<>(nodes);
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
   * Gets all nodes in the graph.
   *
   * @return all map nodes
   */
  public Map<Integer, MapNode> getNodes() {
    return this.nodes;
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
   * Connects two nodes by id. Kept so callers outside this package can connect
   * nodes without
   * looking them up first, which the encounter integration relies on.
   *
   * @param firstId  id of the first node
   * @param secondId id of the second node
   */
  public void connectNodes(Integer firstId, Integer secondId) {
    connectNodes(nodes.get(firstId), nodes.get(secondId));
  }

  /**
   * Called after an encounter finishes.
   *
   * @param nodeId  completed node id
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
   * Starts a run at the given node, unlocking its connections so there is
   * somewhere to move.
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
