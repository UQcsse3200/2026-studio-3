package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.csse3200.game.extensions.GameExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  /** Builds a MapNodeActor without loading textures (test constructor). */
  private MapNodeActor actor(int id) {
    return new MapNodeActor(mapGraph.getNode(id), true);
  }

  /**
   * Fires a touchDown on the given actor's first listener, same as scene2d would when the actor is
   * clicked on stage.
   *
   * @param actor actor to click
   */
  private void click(MapNodeActor actor) {
    InputListener listener = (InputListener) actor.getListeners().get(0);
    listener.touchDown(new InputEvent(), 0, 0, 0, 0);
  }

  @BeforeEach
  void setUp() {
    //   0 (start) -- 1 (AVAILABLE)
    //            \-- 2 (LOCKED, not connected to start's unlock path directly)
    Map<Integer, MapNode> pool = new HashMap<>();
    pool.put(0, new MapNode(0, RoomType.COMBAT));
    pool.put(1, new MapNode(1, RoomType.COMBAT));
    pool.put(2, new MapNode(2, RoomType.SHOP));
    pool.put(3, new MapNode(3, RoomType.COMBAT));

    mapGraph = new MapGraph(pool, false);

    mapGraph.connectNodes(0, 1);
    mapGraph.connectNodes(1, 3); // node 3 stays LOCKED (not adjacent to start)

    // Seed start: node 0 CURRENT, node 1 AVAILABLE. Node 3 stays LOCKED.
    mapGraph.startRun(0);

    controller = new MapSelectionController(mapGraph);
    inputHandler = new MapInputHandler(controller);

    selected = new AtomicReference<>();
    locked = new AtomicReference<>();
    controller.getEvents().addListener("nodeSelected", (Integer id) -> selected.set(id));
    controller.getEvents().addListener("nodeLocked", (Integer id) -> locked.set(id));
  }

  @Test
  void clickingAvailableNodeActorFiresNodeSelected() {
    MapNodeActor available = actor(1); // AVAILABLE after startRun
    inputHandler.attach(available);

    click(available);

    assertEquals(1, selected.get());
    assertNull(locked.get());
  }

  @Test
  void clickingLockedNodeActorFiresNodeLocked() {
    MapNodeActor lockedActor = actor(3); // LOCKED
    inputHandler.attach(lockedActor);

    click(lockedActor);

    assertEquals(3, locked.get());
    assertNull(selected.get());
  }

  @Test
  void attachAllWiresMultipleActors() {
    MapNodeActor available = actor(1);
    MapNodeActor lockedActor = actor(3);
    inputHandler.attachAll(List.of(available, lockedActor));

    click(lockedActor);
    assertEquals(3, locked.get());

    click(available);
    assertEquals(1, selected.get());
  }

  @Test
  void hoveringActorFiresNodeHovered() {
    AtomicReference<Integer> hovered = new AtomicReference<>();
    controller.getEvents().addListener("nodeHovered", (Integer id) -> hovered.set(id));

    MapNodeActor available = actor(1);
    inputHandler.attach(available);

    InputListener listener = (InputListener) available.getListeners().get(0);
    listener.enter(new InputEvent(), 0, 0, 0, null);

    assertEquals(1, hovered.get());
  }
}
