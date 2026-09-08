package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class MapGraphTest {
  private MapNode createNode(int id, NodeState state) {
    MapNode node =
        new MapNode(
            id,
            RoomType.COMBAT); // Add tests for other room types, hardcoded for simplicity for now
    node.setState(state);
    return node;
  }

  @Test
  void testMapGeneration() {
    MapGenerationController mapGen = new MapGenerationController();
    MapGraph map = mapGen.getMap();

    assertTrue(map.getNodes().size() < MapGenerationConfig.MAX_NODE_COUNT);
    // for (int i = 0; i < MapGraph.MAP_HEIGHT; i++) {

    // assertTrue(map.moveToNode());
    // }

  }

  @Test
  void generatedMapCanStartWithReachableChoices() {
    MapGenerationController mapGen = new MapGenerationController();
    MapGraph map = mapGen.getMap();
    MapNode startNode =
        map.getNodesByHeight(1).stream()
            .min((first, second) -> Integer.compare(first.getNodeId(), second.getNodeId()))
            .orElseThrow();

    assertTrue(map.startRun(startNode.getNodeId()));
    assertEquals(NodeState.CURRENT, startNode.getState());
    assertFalse(startNode.getConnections().isEmpty());
    assertTrue(
        startNode.getConnections().stream()
            .allMatch(node -> node.getState() == NodeState.AVAILABLE));
  }

  @Test
  void getCurrentNodeNull() {
    MapGenerationController mapGen = new MapGenerationController();
    MapGraph map = mapGen.getMap();

    assertNull(map.getCurrentNode());
  }

  @Test
  void createsGraphFromGeneratedNodeMap() {
    Map<Integer, MapNode> nodes =
        NodePoolGenerator.generate(new MapGenerationConfig());

    MapGraph graph = new MapGraph(nodes);
    graph.addNodes(nodes);

    assertEquals(nodes.keySet(), graph.getNodes().keySet());
    assertTrue(
        nodes.entrySet().stream()
            .allMatch(entry -> graph.getNode(entry.getKey()) == entry.getValue()));
  }

  @Test
  void getNodesByState() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph map = new MapGraph(NodePoolGenerator.generate(config));

    MapNode node1 = createNode(1, NodeState.AVAILABLE);
    MapNode node2 = createNode(2, NodeState.LOCKED);
    MapNode node3 = createNode(3, NodeState.AVAILABLE);
    MapNode node4 = createNode(4, NodeState.COMPLETED);

    map.addNode(node1);
    map.addNode(node2);
    map.addNode(node3);
    map.addNode(node4);

    List<MapNode> result = map.getNodesByState(NodeState.AVAILABLE);

    assertEquals(2, result.size());
    assertTrue(result.contains(node1));
    assertTrue(result.contains(node3));
    assertFalse(result.contains(node2));
    assertFalse(result.contains(node4));
  }

  @Test
  void getNodesByStateEmpty() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph map = new MapGraph(NodePoolGenerator.generate(config));

    MapNode node1 = createNode(1, NodeState.AVAILABLE);
    MapNode node2 = createNode(2, NodeState.LOCKED);

    map.addNode(node1);
    map.addNode(node2);

    List<MapNode> result = map.getNodesByState(NodeState.COMPLETED);

    assertTrue(result.isEmpty());
  }

  @Test
  void completeNodeSuccess() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));

    MapNode node = createNode(1, NodeState.CURRENT);
    graph.addNode(node);

    graph.completeNode(1, true);

    assertEquals(NodeState.COMPLETED, node.getState());
  }

  @Test
  void completeNodeUnlocksConnected() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));

    MapNode current = createNode(1, NodeState.CURRENT);
    MapNode connected1 = createNode(2, NodeState.LOCKED);
    MapNode connected2 = createNode(3, NodeState.LOCKED);

    graph.addNode(current);
    graph.addNode(connected1);
    graph.addNode(connected2);

    graph.connectNodes(current, connected1);
    graph.connectNodes(current, connected2);

    graph.completeNode(1, true);

    assertEquals(NodeState.COMPLETED, current.getState());
    assertEquals(NodeState.AVAILABLE, connected1.getState());
    assertEquals(NodeState.AVAILABLE, connected2.getState());
  }

  @Test
  void completeNodePreservesNonLocked() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));

    MapNode current = createNode(1, NodeState.CURRENT);
    MapNode completed = createNode(2, NodeState.COMPLETED);
    MapNode available = createNode(3, NodeState.AVAILABLE);

    graph.addNode(current);
    graph.addNode(completed);
    graph.addNode(available);

    graph.connectNodes(current, completed);
    graph.connectNodes(current, available);

    graph.completeNode(1, true);

    assertEquals(NodeState.COMPLETED, completed.getState());
    assertEquals(NodeState.AVAILABLE, available.getState());
  }

  @Test
  void completeNodeFailure() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));

    MapNode current = createNode(1, NodeState.CURRENT);
    MapNode connected = createNode(2, NodeState.LOCKED);

    graph.addNode(current);
    graph.addNode(connected);
    graph.connectNodes(current, connected);

    graph.completeNode(1, false);

    assertEquals(NodeState.CURRENT, current.getState());
    assertEquals(NodeState.LOCKED, connected.getState());
  }

  @Test
  void completeNodeInvalidId() {
    MapGenerationConfig config = new MapGenerationConfig();
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));

    MapNode node = createNode(1, NodeState.CURRENT);
    graph.addNode(node);

    assertDoesNotThrow(() -> graph.completeNode(999, true));

    assertEquals(NodeState.CURRENT, node.getState());
  }

  // TODO: Add tests for moveToNode
}
