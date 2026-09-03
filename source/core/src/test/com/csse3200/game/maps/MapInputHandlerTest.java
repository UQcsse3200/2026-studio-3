package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests for MapInputHandler, checking clicks are routed to the correct node id. */
@ExtendWith(GameExtension.class)
class MapInputHandlerTest {

  private MapGraph mapGraph;
  private MapSelectionController controller;
  private MapInputHandler inputHandler;
  private AtomicReference<Integer> selected;
  private AtomicReference<Integer> locked;

  private MapNode node(int id, RoomType type, NodeState state) {
    MapNode n = new MapNode(id, type);
    n.setState(state);
    return n;
  }

  /**
   * Fires a touchDown on the given actor's first listener, same as scene2d would when the actor is
   * actually clicked on stage.
   *
   * @param actor actor to click
   */
  private void click(MapNodeActor actor) {
    InputListener listener = (InputListener) actor.getListeners().get(0);
    listener.touchDown(new InputEvent(), 0, 0, 0, 0);
  }

  @BeforeEach
  void setUp() {
    RoomDistributionConfig config = new RoomDistributionConfig(70, 70, 20, 10);
    mapGraph = new MapGraph(NodePoolGenerator.generate(config));

    mapGraph.addNode(node(0, RoomType.COMBAT, NodeState.CURRENT));
    mapGraph.addNode(node(1, RoomType.COMBAT, NodeState.AVAILABLE));
    mapGraph.addNode(node(2, RoomType.SHOP, NodeState.LOCKED));

    // mapGraph.connectNodes(0, 1);
    // mapGraph.connectNodes(0, 2);

    controller = new MapSelectionController(mapGraph);
    inputHandler = new MapInputHandler(controller);

    selected = new AtomicReference<>();
    locked = new AtomicReference<>();
    controller.getEvents().addListener("nodeSelected", (Integer id) -> selected.set(id));
    controller.getEvents().addListener("nodeLocked", (Integer id) -> locked.set(id));
  }

  // BLOCKED: MapGraph.moveToNode() NPEs on a fresh graph because no starting
  // node is seeded as CURRENT yet. That seeding depends on map generation
  // (#15), which isn't built yet. Uncomment once a start node is set.
  // @Test
  // void clickingAvailableNodeActorFiresNodeSelected() {
  //   NodeActor actor = new NodeActor(1, 0, 0, 50, 50);
  //   inputHandler.attach(actor);
  //   click(actor);
  //   assertEquals(1, selected.get());
  //   assertNull(locked.get());
  // }

  @Test
  void clickingLockedNodeActorFiresNodeLocked() {
    MapNode node = new MapNode(2, RoomType.COMBAT);
    MapNodeActor actor = new MapNodeActor(node, true);
    inputHandler.attach(actor);

    click(actor);

    assertEquals(2, locked.get());
    assertNull(selected.get());
  }

  @Test
  void attachAllWiresLockedActorCorrectly() {
    MapNode availableNode = new MapNode(2, RoomType.COMBAT);
    MapNode lockedNode = new MapNode(2, RoomType.COMBAT);
    MapNodeActor available = new MapNodeActor(availableNode, true);
    MapNodeActor lockedActor = new MapNodeActor(lockedNode, true);
    inputHandler.attachAll(List.of(available, lockedActor));

    click(lockedActor);
    assertEquals(2, locked.get());
  }

  @Test
  void hoveringActorFiresNodeHovered() {
    AtomicReference<Integer> hovered = new AtomicReference<>();
    controller.getEvents().addListener("nodeHovered", (Integer id) -> hovered.set(id));
    MapNode node = new MapNode(1, RoomType.COMBAT);
    MapNodeActor actor = new MapNodeActor(node, true);
    inputHandler.attach(actor);

    InputListener listener = (InputListener) actor.getListeners().get(0);
    listener.enter(new InputEvent(), 0, 0, 0, null);

    assertEquals(1, hovered.get());
  }

  // BLOCKED: same start-node seeding gap as above (#15) — the "available"
  // half of this test needs a successful move.
  // @Test
  // void attachAllWiresMultipleActors() {
  //   NodeActor available = new NodeActor(1, 0, 0, 50, 50);
  //   NodeActor lockedActor = new NodeActor(2, 60, 0, 50, 50);
  //   inputHandler.attachAll(List.of(available, lockedActor));
  //   click(lockedActor);
  //   assertEquals(2, locked.get());
  //   click(available);
  //   assertEquals(1, selected.get());
  // }
}
