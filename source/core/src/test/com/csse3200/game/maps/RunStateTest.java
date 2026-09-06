package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class RunStateTest {

  /** Start node 0, connected forwards to 1 and 2. */
  private MapGraph createGraph() {
    RoomDistributionConfig config = new RoomDistributionConfig(MapGraph.MAX_NODE_COUNT, 60, 30, 10);
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));
    graph.addNode(new MapNode(0, RoomType.COMBAT));
    graph.addNode(new MapNode(1, RoomType.EVENT));
    graph.addNode(new MapNode(2, RoomType.SHOP));

    graph.getNode(0).addConnection(graph.getNode(1));
    graph.getNode(0).addConnection(graph.getNode(2));

    return graph;
  }

  @Test
  void noRunActiveBeforeStarting() {
    RunState runState = new RunState();

    assertFalse(runState.isRunActive());
    assertNull(runState.getMapGraph());
  }

  @Test
  void startRunOpensTheFirstChoices() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();

    assertTrue(runState.startRun(graph, 0));
    assertTrue(runState.isRunActive());
    assertEquals(NodeState.CURRENT, graph.getNode(0).getState());
    assertEquals(NodeState.AVAILABLE, graph.getNode(1).getState());
    assertEquals(NodeState.AVAILABLE, graph.getNode(2).getState());
  }

  @Test
  void startRunRejectsUnknownStartNode() {
    RunState runState = new RunState();

    assertFalse(runState.startRun(createGraph(), 99));
    assertFalse(runState.isRunActive());
  }

  @Test
  void startRunRejectsNullGraph() {
    RunState runState = new RunState();

    assertFalse(runState.startRun(null, 0));
  }

  @Test
  void completingAnEncounterUnlocksConnectedNodes() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    graph.getNode(1).addConnection(graph.getNode(2));

    runState.startRun(graph, 0);
    graph.moveToNode(1);
    runState.enterEncounter(1);
    runState.completeEncounter(true);

    assertEquals(NodeState.COMPLETED, graph.getNode(1).getState());
    assertEquals(NodeState.AVAILABLE, graph.getNode(2).getState());
  }

  @Test
  void failedEncounterDoesNotAdvanceProgress() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();

    MapNode locked = new MapNode(3, RoomType.COMBAT);
    graph.addNode(locked);
    graph.getNode(1).addConnection(locked);

    runState.startRun(graph, 0);
    graph.moveToNode(1);
    runState.enterEncounter(1);
    runState.completeEncounter(false);

    assertNotEquals(NodeState.COMPLETED, graph.getNode(1).getState());
    assertEquals(NodeState.LOCKED, locked.getState());
  }

  @Test
  void enteringAnEncounterClosesOffTheStartNode() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();

    runState.startRun(graph, 0);
    graph.moveToNode(1);
    runState.enterEncounter(1);

    assertEquals(NodeState.COMPLETED, graph.getNode(0).getState());
    assertEquals(1, graph.getNodesByState(NodeState.CURRENT).size());
  }

  /**
   * The point of the whole class: leaving the map for an encounter and coming back has to give the
   * same map, not a freshly generated one, because screens are disposed on every change.
   */
  @Test
  void mapAndProgressSurviveAnEncounter() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();

    runState.startRun(graph, 0);
    graph.moveToNode(1);
    runState.enterEncounter(1);
    runState.completeEncounter(true);

    assertSame(graph, runState.getMapGraph());
    assertEquals(NodeState.COMPLETED, graph.getNode(1).getState());
    // Same map instance, same nodes — it was not regenerated while the encounter ran.
    assertEquals(graph.getNodes().size(), runState.getMapGraph().getNodes().size());
  }

  @Test
  void activeNodeIsClearedOnceTheEncounterReports() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();

    runState.startRun(graph, 0);
    graph.moveToNode(1);
    runState.enterEncounter(1);

    assertEquals(1, runState.getActiveNodeId());

    runState.completeEncounter(true);

    assertNull(runState.getActiveNodeId());
  }

  @Test
  void completingWithNoActiveEncounterIsIgnored() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    runState.startRun(graph, 0);

    runState.completeEncounter(true);

    assertEquals(NodeState.CURRENT, graph.getNode(0).getState());
  }

  @Test
  void endRunDiscardsTheMap() {
    RunState runState = new RunState();
    runState.startRun(createGraph(), 0);

    runState.endRun();

    assertFalse(runState.isRunActive());
    assertNull(runState.getMapGraph());
    assertNull(runState.getActiveNodeId());
  }
}
