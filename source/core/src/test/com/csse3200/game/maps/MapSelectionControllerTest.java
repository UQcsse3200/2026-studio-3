package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests for MapSelectionController using a small controlled MapGraph. */
@ExtendWith(GameExtension.class)
class MapSelectionControllerTest {

  private MapGraph mapGraph;
  private MapSelectionController controller;
  private AtomicReference<Integer> selected;
  private AtomicReference<Integer> locked;
  private AtomicReference<Integer> completed;

  /**
   * Creates a node with the given id and room type.
   *
   * @param id node identifier
   * @param roomType room type
   * @return the created node
   */
  private MapNode node(int id, RoomType roomType) {
    return new MapNode(id, roomType);
  }

  @BeforeEach
  void setUp() {
    // Build a small controlled graph WITHOUT procedural generation.
    //   0 (start) -- 1 -- 3 -- 4
    //            \-- 2 --/
    Map<Integer, MapNode> pool = new HashMap<>();
    MapNode node1 = new MapNode(0, RoomType.COMBAT);
    MapNode node2 = new MapNode(1, RoomType.COMBAT);
    MapNode node3 = new MapNode(2, RoomType.SHOP);
    MapNode node4 = new MapNode(3, RoomType.COMBAT);
    MapNode node5 = new MapNode(4, RoomType.FINAL);

    node1.setHeight(0);
    node2.setHeight(1);
    node3.setHeight(1);
    node4.setHeight(2);
    node5.setHeight(3);

    pool.put(0, node1);
    pool.put(1, node2);
    pool.put(2, node3);
    pool.put(3, node4);
    pool.put(4, node5);

    mapGraph = new MapGraph(pool, false);

    mapGraph.connectNodes(0, 1);
    mapGraph.connectNodes(0, 2);
    mapGraph.connectNodes(1, 3);
    mapGraph.connectNodes(2, 3);
    mapGraph.connectNodes(3, 4);

    // Seed the start node: node 0 becomes CURRENT, its connections (1, 2) become AVAILABLE.
    mapGraph.startRun(0);

    controller = new MapSelectionController(mapGraph);

    selected = new AtomicReference<>();
    locked = new AtomicReference<>();
    completed = new AtomicReference<>();
    controller.getEvents().addListener("nodeSelected", (Integer id) -> selected.set(id));
    controller.getEvents().addListener("nodeLocked", (Integer id) -> locked.set(id));
    controller.getEvents().addListener("nodeCompleted", (Integer id) -> completed.set(id));
  }

  @Test
  void selectableNodeCommitsMoveAndFiresSelected() {
    boolean accepted = controller.onNodeClicked(1); // AVAILABLE from start

    assertTrue(accepted);
    assertEquals(1, selected.get());
    assertNull(locked.get());
    assertEquals(NodeState.CURRENT, mapGraph.getNode(1).getState());
    assertEquals(1, mapGraph.getCurrentNode().getNodeId());
  }

  @Test
  void lockedNodeSendsNoMoveAndFiresLocked() {
    boolean accepted = controller.onNodeClicked(3); // LOCKED at start

    assertFalse(accepted);
    assertEquals(3, locked.get());
    assertNull(selected.get());
    assertEquals(NodeState.LOCKED, mapGraph.getNode(3).getState());
  }

  @Test
  void unknownNodeFiresLockedAndDoesNotMove() {
    boolean accepted = controller.onNodeClicked(999);

    assertFalse(accepted);
    assertEquals(999, locked.get());
    assertNull(selected.get());
  }

  @Test
  void nullClickIsIgnored() {
    assertFalse(controller.onNodeClicked(null));
    assertNull(selected.get());
    assertNull(locked.get());
  }

  @Test
  void completedNodeFiresCompletedNotLocked() {
    mapGraph.getNode(1).setState(NodeState.COMPLETED);

    boolean accepted = controller.onNodeClicked(1);

    assertFalse(accepted);
    assertEquals(1, completed.get());
    assertNull(locked.get());
    assertNull(selected.get());
  }

  @Test
  void lockedNodeBecomesSelectableAfterEncounterCompletes() {
    controller.onNodeClicked(1); // move 0 -> 1
    mapGraph.completeNode(1, true); // finishing node 1 unlocks its connections (node 3)

    assertEquals(NodeState.AVAILABLE, mapGraph.getNode(3).getState());
    assertTrue(controller.onNodeClicked(3));
  }

  @Test
  void roomTypeIsExposedForFinalNode() {
    assertEquals(RoomType.FINAL, mapGraph.getNode(4).getRoomType());
  }

  @Test
  void getSelectableNodeIdsReturnsOnlyAvailable() {
    List<Integer> selectable = controller.getSelectableNodeIds();

    assertEquals(2, selectable.size());
    assertTrue(selectable.contains(1));
    assertTrue(selectable.contains(2));
    assertFalse(selectable.contains(0));
    assertFalse(selectable.contains(3));
  }

  @Test
  void isSelectableMatchesNodeState() {
    assertTrue(controller.isSelectable(1));
    assertFalse(controller.isSelectable(3));
    assertFalse(controller.isSelectable(999));
    assertFalse(controller.isSelectable(null));
  }
}
