package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class MapNodeTest {

  @Test
  void newNodeKeepsItsIdAndRoomType() {
    MapNode node = new MapNode(3, RoomType.SHOP);

    assertEquals(3, node.getNodeId());
    assertEquals(RoomType.SHOP, node.getRoomType());
  }

  /** Nodes start locked so nothing is selectable until a run opens the first choices. */
  @Test
  void newNodeStartsLocked() {
    assertEquals(NodeState.LOCKED, new MapNode(0, RoomType.COMBAT).getState());
  }

  @Test
  void newNodeHasNoConnections() {
    assertTrue(new MapNode(0, RoomType.COMBAT).getConnections().isEmpty());
  }

  @Test
  void stateCanBeChanged() {
    MapNode node = new MapNode(0, RoomType.COMBAT);

    node.setState(NodeState.AVAILABLE);
    assertEquals(NodeState.AVAILABLE, node.getState());

    node.setState(NodeState.COMPLETED);
    assertEquals(NodeState.COMPLETED, node.getState());
  }

  @Test
  void roomTypeCanBeChanged() {
    MapNode node = new MapNode(0, RoomType.COMBAT);

    node.setRoomType(RoomType.ELITE);

    assertEquals(RoomType.ELITE, node.getRoomType());
  }

  /** Height is the row the node sits on, derived from its id and the map width. */
  @Test
  void heightIsDerivedFromTheNodeId() {
    assertEquals(0, new MapNode(0, RoomType.START).getHeight());
    assertEquals(0, new MapNode(MapGenerationConfig.MAP_WIDTH - 1, RoomType.COMBAT).getHeight());
    assertEquals(1, new MapNode(MapGenerationConfig.MAP_WIDTH, RoomType.COMBAT).getHeight());
    assertEquals(2, new MapNode(MapGenerationConfig.MAP_WIDTH * 2, RoomType.COMBAT).getHeight());
  }

  @Test
  void nodesOnTheSameRowShareAHeight() {
    MapNode first = new MapNode(MapGenerationConfig.MAP_WIDTH, RoomType.COMBAT);
    MapNode last = new MapNode(MapGenerationConfig.MAP_WIDTH * 2 - 1, RoomType.EVENT);

    assertEquals(first.getHeight(), last.getHeight());
  }

  @Test
  void addingAConnectionMakesItVisible() {
    MapNode from = new MapNode(0, RoomType.COMBAT);
    MapNode to = new MapNode(1, RoomType.EVENT);

    from.addConnection(to);

    assertTrue(from.getConnections().contains(to));
    assertEquals(1, from.getConnections().size());
  }

  /** Connections point one way, so the player cannot walk back into a room they skipped. */
  @Test
  void connectionsAreNotAddedBothWays() {
    MapNode from = new MapNode(0, RoomType.COMBAT);
    MapNode to = new MapNode(1, RoomType.EVENT);

    from.addConnection(to);

    assertFalse(to.getConnections().contains(from));
  }

  @Test
  void addingTheSameConnectionTwiceKeepsOneCopy() {
    MapNode from = new MapNode(0, RoomType.COMBAT);
    MapNode to = new MapNode(1, RoomType.EVENT);

    from.addConnection(to);
    from.addConnection(to);

    assertEquals(1, from.getConnections().size());
  }

  @Test
  void aNodeCanHaveSeveralConnections() {
    MapNode from = new MapNode(0, RoomType.START);
    MapNode first = new MapNode(1, RoomType.COMBAT);
    MapNode second = new MapNode(2, RoomType.SHOP);

    from.addConnection(first);
    from.addConnection(second);

    assertEquals(2, from.getConnections().size());
    assertTrue(from.getConnections().containsAll(java.util.List.of(first, second)));
  }
}
