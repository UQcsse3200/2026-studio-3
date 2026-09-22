package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Walks a whole run through the map rather than testing each class on its own.
 *
 * <p>Selecting a node touches {@link MapSelectionController}, {@link MapGraph} and {@link RunState}
 * together, and the bugs that have actually bitten us sat between them rather than inside any one
 * of them. None of this needs a graphics context, so the loop can be covered without the screens.
 *
 * <p>The map here is built by hand rather than generated, so the assertions do not depend on what
 * the procedural generator happens to produce.
 */
@ExtendWith(GameExtension.class)
public class MapProgressionEndToEndTest {

  private static final Integer START = 0;
  private static final Integer COMBAT = 1;
  private static final Integer SHOP = 2;
  private static final Integer BOSS = 3;

  private MapGraph graph;
  private RunState runState;
  private MapSelectionController controller;
  private List<String> firedEvents;

  /** start -> combat and shop, both -> boss. */
  @BeforeEach
  void setUp() {
    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(START, new MapNode(START, RoomType.START));
    nodes.put(COMBAT, new MapNode(COMBAT, RoomType.COMBAT));
    nodes.put(SHOP, new MapNode(SHOP, RoomType.SHOP));
    nodes.put(BOSS, new MapNode(BOSS, RoomType.FINAL));

    graph = new MapGraph(nodes, false);
    graph.getNode(START).addConnection(graph.getNode(COMBAT));
    graph.getNode(START).addConnection(graph.getNode(SHOP));
    graph.getNode(COMBAT).addConnection(graph.getNode(BOSS));
    graph.getNode(SHOP).addConnection(graph.getNode(BOSS));

    EventHandler events = new EventHandler();
    firedEvents = new ArrayList<>();
    for (String name :
        List.of("nodeSelected", "nodeLocked", "nodeCompleted", "nodeSelectionRejected")) {
      events.addListener(name, (Integer nodeId) -> firedEvents.add(name + ":" + nodeId));
    }

    controller = new MapSelectionController(graph, events);
    runState = new RunState();
    runState.startRun(graph, START);
  }

  @Test
  void startingARunOpensOnlyTheFirstChoices() {
    assertEquals(NodeState.CURRENT, graph.getNode(START).getState());
    assertEquals(List.of(COMBAT, SHOP), sortedSelectableIds());
    assertEquals(NodeState.LOCKED, graph.getNode(BOSS).getState());
  }

  @Test
  void aLockedNodeCannotBeSelected() {
    assertFalse(controller.onNodeClicked(BOSS));

    assertTrue(firedEvents.contains("nodeLocked:" + BOSS));
    assertNull(runState.getActiveNodeId());
    assertEquals(NodeState.CURRENT, graph.getNode(START).getState());
  }

  @Test
  void selectingAnAvailableNodeMovesThePlayerAndReportsIt() {
    assertTrue(controller.onNodeClicked(COMBAT));

    assertTrue(firedEvents.contains("nodeSelected:" + COMBAT));
    assertEquals(COMBAT, graph.getCurrentNode().getNodeId());
  }

  /** The loop the whole feature exists for: pick a room, resolve it, come back to the same map. */
  @Test
  void completingAnEncounterUnlocksWhatComesNext() {
    controller.onNodeClicked(COMBAT);
    runState.enterEncounter(COMBAT);
    runState.completeEncounter(true);

    assertSame(graph, runState.getMapGraph());
    assertEquals(NodeState.COMPLETED, graph.getNode(COMBAT).getState());
    assertEquals(NodeState.AVAILABLE, graph.getNode(BOSS).getState());
    assertNull(runState.getActiveNodeId());
  }

  @Test
  void failingAnEncounterLeavesProgressWhereItWas() {
    controller.onNodeClicked(COMBAT);
    runState.enterEncounter(COMBAT);
    runState.completeEncounter(false);

    assertNotEquals(NodeState.COMPLETED, graph.getNode(COMBAT).getState());
    assertEquals(NodeState.LOCKED, graph.getNode(BOSS).getState());
  }

  @Test
  void aCompletedNodeCannotBeEnteredAgain() {
    controller.onNodeClicked(COMBAT);
    runState.enterEncounter(COMBAT);
    runState.completeEncounter(true);
    firedEvents.clear();

    assertFalse(controller.onNodeClicked(COMBAT));

    assertTrue(firedEvents.contains("nodeCompleted:" + COMBAT));
    assertNull(runState.getActiveNodeId());
  }

  /** Taking one branch should not leave the other one open to walk into later. */
  @Test
  void theBranchNotTakenIsNotStillWaiting() {
    controller.onNodeClicked(COMBAT);
    runState.enterEncounter(COMBAT);
    runState.completeEncounter(true);

    assertFalse(controller.onNodeClicked(SHOP));
    assertEquals(COMBAT, graph.getCurrentNode().getNodeId());
  }

  @Test
  void aRunCanBePlayedFromTheStartThroughToTheBoss() {
    controller.onNodeClicked(COMBAT);
    runState.enterEncounter(COMBAT);
    runState.completeEncounter(true);

    assertTrue(controller.onNodeClicked(BOSS));
    runState.enterEncounter(BOSS);
    runState.completeEncounter(true);

    assertEquals(NodeState.COMPLETED, graph.getNode(BOSS).getState());
    assertEquals(RoomType.FINAL, graph.getNode(BOSS).getRoomType());
  }

  @Test
  void endingTheRunClearsTheMap() {
    controller.onNodeClicked(COMBAT);
    runState.enterEncounter(COMBAT);

    runState.endRun();

    assertFalse(runState.isRunActive());
    assertNull(runState.getMapGraph());
    assertNull(runState.getActiveNodeId());
  }

  private List<Integer> sortedSelectableIds() {
    List<Integer> ids = new ArrayList<>(controller.getSelectableNodeIds());
    ids.sort(Integer::compareTo);
    return ids;
  }
}
