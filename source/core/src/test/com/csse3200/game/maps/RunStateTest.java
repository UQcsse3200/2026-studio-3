package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class RunStateTest {

  /** Start node 0, connected forwards to 1 and 2. */
  private MapGraph createGraph() {
    MapGenerationConfig config = new MapGenerationConfig();
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
  void playerStatePersistsAcrossRunAccesses() {
    RunState runState = new RunState();

    PlayerRunState first = runState.getOrCreatePlayerState();
    first.restore(50, 100, 25);

    PlayerRunState second = runState.getOrCreatePlayerState();

    assertSame(first, second);
    assertEquals(50, second.getCurrentHealth());
    assertEquals(100, second.getMaxHealth());
    assertEquals(25, second.getGold());
  }

  @Test
  void playerDeckPersistsAcrossRunAccesses() {
    RunState runState = new RunState();
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());

    PlayerDeck first = runState.getOrCreatePlayerDeck(cardService);
    PlayerDeck second = runState.getOrCreatePlayerDeck(cardService);

    assertSame(first, second);
  }

  @Test
  void mapHeightReturnsCorrectValue() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    runState.startRun(graph, 0);

    assertEquals(graph.getCurrentNode().getHeight(), runState.getMapProgression());
  }

  @Test
  void returnsZeroWhenActiveNodeIsMissing() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    runState.startRun(graph, 0);

    assertEquals(0, runState.getMapProgression());
  }

  @Test
  void returnsZeroWhenNodeIdDoesNotExist() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    runState.startRun(graph, 0);

    runState.enterEncounter(999);

    assertEquals(0, runState.getMapProgression());
  }

  @Test
  void restoringRunFixesNodeStuckMidBattle() {
    // Regression test for a real bug reported by Jayden (Team 4): saving mid-battle leaves that
    // node CURRENT and MapGraph.currentNode pointing at it. Both must be corrected on restore, or
    // the node is permanently unselectable -- see RunState.restoreRun() for the full explanation.
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    graph.getNode(0).setState(NodeState.COMPLETED); // the real prior position
    graph.getNode(1).setState(NodeState.CURRENT); // interrupted mid-battle when the save was made
    // addConnection() in createGraph() is one-directional; connectNodes() is what real save data
    // uses (see SaveGameRestoreService.buildMapGraph()) and adds the reverse link too, which the
    // fix's neighbour search over node 1's own connections needs.
    graph.connectNodes(1, 0);
    graph.restoreCurrentNode(1); // simulates buildMapGraph(), which runs before restoreRun()

    assertTrue(runState.restoreRun(graph, 1));

    assertEquals(NodeState.AVAILABLE, graph.getNode(1).getState());
    assertSame(graph.getNode(0), graph.getCurrentNode());
  }

  @Test
  void restoringRunWithNoActiveEncounterLeavesNodesUnaffected() {
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    graph.getNode(0).setState(NodeState.CURRENT);
    graph.restoreCurrentNode(0);

    assertTrue(runState.restoreRun(graph, null));

    assertEquals(NodeState.CURRENT, graph.getNode(0).getState());
    assertSame(graph.getNode(0), graph.getCurrentNode());
    assertNull(runState.getActiveNodeId());
  }

  @Test
  void firstBattleOfARunAlwaysLeavesACompletedNeighbourToFallBackOn() {
    // Regression test addressing a review question from Zaidan on PR #315: does the fix's
    // COMPLETED-neighbour search find anything for the very first battle of a run, before
    // anything else has been completed? Confirms the invariant holds: entering any encounter
    // (enterEncounter()) always closes off the node the player just moved from, and that node
    // is guaranteed to be a genuine, bidirectional neighbour of the new CURRENT node, since
    // moveToNode() only succeeds via a real connectNodes()-created connection. So by the time a
    // mid-battle save is even possible (activeNodeId != null), a COMPLETED neighbour always
    // exists. The true "nothing completed yet" case -- the start node itself -- has no battle of
    // its own (RoomType.START), so activeNodeId stays null there and the fix's block never runs.
    RunState runState = new RunState();
    MapGraph graph = createGraph();
    // createGraph()'s addConnection() is one-directional; the real graph-building path
    // (connectNodes(), used by SaveGameRestoreService.buildMapGraph()) adds both directions.
    graph.connectNodes(1, 0);

    runState.startRun(graph, 0); // node 0: CURRENT, nodes 1 & 2: AVAILABLE
    graph.moveToNode(1); // the very first move of the run
    runState.enterEncounter(1); // the very first encounter of the run

    assertEquals(NodeState.COMPLETED, graph.getNode(0).getState());
    assertTrue(graph.getNode(1).getConnections().contains(graph.getNode(0)));

    // Simulate saving mid-battle right here and reloading.
    graph.restoreCurrentNode(1);
    assertTrue(runState.restoreRun(graph, 1));

    assertEquals(NodeState.AVAILABLE, graph.getNode(1).getState());
    assertSame(graph.getNode(0), graph.getCurrentNode());
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
