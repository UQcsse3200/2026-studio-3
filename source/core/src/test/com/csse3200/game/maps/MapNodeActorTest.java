package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.extensions.GameExtension;
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

    assertEquals(1.25f * actor.size, actor.getNodeSize());
  }

  @Test
  void setHoveredFalseResetsScale() {
    MapNodeActor actor = new MapNodeActor(node, true);

    actor.setHovered(true);
    actor.setHovered(false);

    assertEquals(actor.size, actor.getNodeSize());
  }

  @Test
  void nodeHasCorrectSize() {
    MapNode node = new MapNode(0, RoomType.COMBAT);
    MapNodeActor actor = new MapNodeActor(node, true);

    assertEquals(Gdx.graphics.getWidth() / 13f, actor.getNodeSize());
  }
}
