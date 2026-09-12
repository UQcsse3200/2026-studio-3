package com.csse3200.game.maps;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Holds the map and the player's progress for the length of a run.
 *
 * <p>Screens get disposed when the game switches away from them, so the map can't be owned by the
 * map screen or it would be lost every time the player enters an encounter. GdxGame holds this
 * instead.
 */
public class RunState {
  private static final Logger logger = LoggerFactory.getLogger(RunState.class);

  private MapGraph mapGraph;
  private Integer activeNodeId;
  private PlayerDeck playerDeck;

  /**
   * Returns the run-scoped player deck, creating the starter deck on first access.
   *
   * @param cardService authoritative card lookup service used to validate starter card IDs
   * @return the player's persistent deck for this run
   */
  public PlayerDeck getOrCreatePlayerDeck(CardService cardService) {
    if (cardService == null) {
      throw new IllegalArgumentException("cardService must not be null");
    }
    if (playerDeck == null) {
      playerDeck = PlayerDeckFactory.createStarterDeck(cardService);
    }
    return playerDeck;
  }

  /**
   * Starts a run on a generated map.
   *
   * @param mapGraph map for this run
   * @param startNodeId node the player begins on
   * @return true if the run was started
   */
  public boolean startRun(MapGraph mapGraph, Integer startNodeId) {
    if (mapGraph == null || !mapGraph.startRun(startNodeId)) {
      logger.warn("Could not start run at node {}", startNodeId);
      return false;
    }

    this.mapGraph = mapGraph;
    this.activeNodeId = null;
    return true;
  }

  /**
   * Sets the map for the current run without placing the player on it. Used when the map is
   * generated for display before progression has started.
   *
   * @param mapGraph map for this run
   */
  public void setMapGraph(MapGraph mapGraph) {
    this.mapGraph = mapGraph;
    this.activeNodeId = null;
  }

  public boolean isRunActive() {
    return mapGraph != null;
  }

  public MapGraph getMapGraph() {
    return mapGraph;
  }

  /**
   * Remembers the node the player entered so the encounter can report back against it. Any node
   * still marked current is one the player passed through without an encounter, i.e. the node they
   * started on, so it is closed off here.
   */
  public void enterEncounter(Integer nodeId) {
    for (MapNode node : mapGraph.getNodesByState(NodeState.CURRENT)) {
      if (!node.getNodeId().equals(nodeId)) {
        node.setState(NodeState.COMPLETED);
      }
    }
    activeNodeId = nodeId;
  }

  public Integer getActiveNodeId() {
    return activeNodeId;
  }

  /** Returns the height of the currently active node. */
  public Integer getMapProgression () {
      Integer mapNodeId = this.getActiveNodeId();
      MapNode activeNode = mapGraph.getNode(mapNodeId);

      if (mapNodeId == null || !Objects.nonNull(activeNode)
              || activeNode.getHeight() <= 0) {
          return 0;
      }

      return mapGraph.getNode(
              this.getActiveNodeId()
      ).getHeight();
  }

  /**
   * Reports the encounter result to the map. A failed encounter leaves the map alone so progression
   * doesn't advance.
   *
   * @param success whether the encounter was completed
   */
  public void completeEncounter(boolean success) {
    if (mapGraph == null || activeNodeId == null) {
      logger.warn("Encounter finished but no node was active");
      return;
    }

    mapGraph.onEncounterComplete(activeNodeId, success);
    activeNodeId = null;
  }

  public void endRun() {
    mapGraph = null;
    activeNodeId = null;
    playerDeck = null;
  }
}
