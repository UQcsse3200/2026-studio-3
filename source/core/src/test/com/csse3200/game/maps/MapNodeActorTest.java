package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MapNodeActorTest {

  private MapNode node;

  @BeforeEach
  void setUp() {
    node = new MapNode(5, RoomType.COMBAT);
    node.setState(NodeState.LOCKED);
  }

  @Test
  void getNodeReturnsCorrectNode() {
    MapNodeActor actor = new MapNodeActor(node, true);

    assertEquals(node, actor.getNode());
  }

  @Test
  void getNodeIdReturnsCorrectId() {
    MapNodeActor actor = new MapNodeActor(node, true);

    assertEquals(5, actor.getNodeId());
  }

  @Test
  void setHoveredScalesNodeUp() {
    MapNodeActor actor = new MapNodeActor(node, true);

    actor.setHovered(true);

    assertEquals(1.25f, actor.getNodeScale());
  }

  @Test
  void setHoveredFalseResetsScale() {
    MapNodeActor actor = new MapNodeActor(node, true);

    actor.setHovered(true);
    actor.setHovered(false);

    assertEquals(1f, actor.getNodeScale());
  }

  @Test
  void nodeHasCorrectSize() {
    MapNode node = new MapNode(0, RoomType.COMBAT);
    MapNodeActor actor = new MapNodeActor(node, true);

    float mapWidth = 1280f;
    float size = (mapWidth - 512f) / 13f;

    assertEquals(size, actor.getNodeSize());
  }
}
