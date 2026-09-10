package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NodePoolGeneratorTest {

  @Test
  void generatesNormalNodesAndOneFinalNode() {
    RoomDistributionConfig config = new RoomDistributionConfig(10, 60, 30, 10, 12345L);

    Map<Integer, MapNode> nodes = NodePoolGenerator.generate(config);

    assertEquals(11, nodes.size());
    assertEquals(10, nodes.get(10).getNodeId());
    assertEquals(RoomType.FINAL, nodes.get(10).getRoomType());
    assertEquals(
        1, nodes.values().stream().filter(node -> node.getRoomType() == RoomType.FINAL).count());
    assertEquals(
        11, new HashSet<>(nodes.values().stream().map(MapNode::getNodeId).toList()).size());
    assertTrue(nodes.values().stream().allMatch(node -> node.getState() == NodeState.LOCKED));
    assertTrue(
        nodes.entrySet().stream()
            .allMatch(entry -> entry.getKey().equals(entry.getValue().getNodeId())));
  }

  @Test
  void followsConfiguredDistribution() {
    RoomDistributionConfig config = new RoomDistributionConfig(10, 60, 30, 10, 12345L);

    Map<Integer, MapNode> nodes = NodePoolGenerator.generate(config);

    assertEquals(6, countRooms(nodes, RoomType.COMBAT));
    assertEquals(3, countRooms(nodes, RoomType.EVENT));
    assertEquals(1, countRooms(nodes, RoomType.SHOP));
  }

  @Test
  void allocatesRemainderNodesToClosestWeightedDistribution() {
    RoomDistributionConfig config = new RoomDistributionConfig(7, 60, 30, 10, 12345L);

    Map<Integer, MapNode> nodes = NodePoolGenerator.generate(config);

    assertEquals(4, countRooms(nodes, RoomType.COMBAT));
    assertEquals(2, countRooms(nodes, RoomType.EVENT));
    assertEquals(1, countRooms(nodes, RoomType.SHOP));
  }

  @Test
  void usesLongArithmeticForLargeWeights() {
    RoomDistributionConfig config =
        new RoomDistributionConfig(
            7, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 12345L);

    Map<Integer, MapNode> nodes = NodePoolGenerator.generate(config);

    assertEquals(3, countRooms(nodes, RoomType.COMBAT));
    assertEquals(2, countRooms(nodes, RoomType.EVENT));
    assertEquals(2, countRooms(nodes, RoomType.SHOP));
  }

  @Test
  void sameSeedProducesSameRoomAssignments() {
    RoomDistributionConfig firstConfig = new RoomDistributionConfig(20, 60, 30, 10, 98765L);
    RoomDistributionConfig secondConfig = new RoomDistributionConfig(20, 60, 30, 10, 98765L);

    List<RoomType> firstTypes = roomTypesById(NodePoolGenerator.generate(firstConfig));
    List<RoomType> secondTypes = roomTypesById(NodePoolGenerator.generate(secondConfig));

    assertEquals(firstTypes, secondTypes);
  }

  @Test
  void excludesZeroWeightRoomTypes() {
    RoomDistributionConfig config = new RoomDistributionConfig(8, 1, 0, 0, 1L);

    Map<Integer, MapNode> nodes = NodePoolGenerator.generate(config);

    assertEquals(8, countRooms(nodes, RoomType.COMBAT));
    assertEquals(0, countRooms(nodes, RoomType.EVENT));
    assertEquals(0, countRooms(nodes, RoomType.SHOP));
  }

  @Test
  void returnsImmutableNodePool() {
    Map<Integer, MapNode> nodes =
        NodePoolGenerator.generate(new RoomDistributionConfig(3, 1, 1, 1, 1L));

    assertThrows(UnsupportedOperationException.class, () -> nodes.put(99, nodes.get(0)));
  }

  @Test
  void rejectsNullConfiguration() {
    assertThrows(NullPointerException.class, () -> NodePoolGenerator.generate(null));
  }

  @Test
  void doesNotCreateGraphConnections() {
    Map<Integer, MapNode> nodes =
        NodePoolGenerator.generate(new RoomDistributionConfig(5, 3, 2, 1, 1L));

    assertFalse(nodes.isEmpty());
    assertTrue(nodes.values().stream().allMatch(node -> node.getConnections().isEmpty()));
  }

  private List<RoomType> roomTypesById(Map<Integer, MapNode> nodes) {
    return nodes.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getValue().getRoomType())
        .toList();
  }

  private long countRooms(Map<Integer, MapNode> nodes, RoomType roomType) {
    return nodes.values().stream().filter(node -> node.getRoomType() == roomType).count();
  }
}
