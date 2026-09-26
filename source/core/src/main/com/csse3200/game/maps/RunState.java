package com.csse3200.game.maps;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.rewards.RewardOption;
import java.util.Objects;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private int playerHealth;
  private int playerMaxHealth;
  private int playerMaxEnergy;
  private boolean playerStatsInitialised;
  private PlayerRunState playerState;
  private Long encounterSeed;

  /**
   * Returns the durable player values for this run, initialising them from the player config on
   * first access.
   *
   * @return the player's persistent health and gold state
   */
  public PlayerRunState getOrCreatePlayerState() {
    if (playerState == null) {
      playerState = PlayerFactory.createInitialRunState();
    }
    return playerState;
  }

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
   * Initialises the player's health
   *
   * @param startingHealth the health the player starts with
   */
  public void initialisePlayerStats(
      int startingHealth, int startingMaxHealth, int startingMaxEnergy) {
    if (!playerStatsInitialised) {
      playerHealth = startingHealth;
      playerMaxHealth = startingMaxHealth;
      playerMaxEnergy = startingMaxEnergy;
      playerStatsInitialised = true;
    }
  }

  /**
   * Returns the player's current health
   *
   * @return the player's health
   */
  public int getPlayerHealth() {
    return playerHealth;
  }

  /**
   * Returns the player's max health
   *
   * @return the player's max health
   */
  public int getPlayerMaxHealth() {
    return playerMaxHealth;
  }

  /**
   * Returns the player's max energy
   *
   * @return the player's max energy
   */
  public int getPlayerMaxEnergy() {
    return playerMaxEnergy;
  }

  /**
   * Sets the player's health
   *
   * @param health the player's new health
   */
  public void setPlayerHealth(int health) {
    playerHealth = Math.max(0, health);
  }

  /**
   * Sets the player's max health
   *
   * @param maxHealth the player's new max health
   */
  public void setPlayerMaxHealth(int maxHealth) {
    playerMaxHealth = Math.max(1, maxHealth);
  }

  /**
   * Sets the player's max energy
   *
   * @param maxEnergy the player's new max energy
   */
  public void setPlayerMaxEnergy(int maxEnergy) {
    playerMaxEnergy = Math.max(1, maxEnergy);
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
    this.encounterSeed = new Random().nextLong();
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

  /**
   * Gets the seed that decides which encounter each node of this run holds.
   *
   * @return the run's encounter seed, or null if no run is in progress
   */
  public Long getEncounterSeed() {
    return encounterSeed;
  }

  /**
   * Puts back the encounter seed from a save, so every node keeps the encounter it had before.
   *
   * @param savedSeed seed read from the save, or null for saves made before it was recorded
   */
  public void restoreEncounterSeed(Long savedSeed) {
    if (savedSeed != null) {
      encounterSeed = savedSeed;
    }
  }

  public MapGraph getMapGraph() {
    return mapGraph;
  }

  /**
   * Restores a saved run without replaying movement or encounter transitions.
   *
   * @param mapGraph restored map graph
   * @param activeNodeId saved in-progress encounter node, or null if the player is between rooms
   * @return true if the run state was restored
   */
  public boolean restoreRun(MapGraph mapGraph, Integer activeNodeId) {
    if (mapGraph == null) {
      logger.warn("Could not restore run without a map");
      return false;
    }
    if (activeNodeId != null && mapGraph.getNode(activeNodeId) == null) {
      logger.warn("Could not restore active encounter at unknown node {}", activeNodeId);
      return false;
    }

    this.mapGraph = mapGraph;
    this.activeNodeId = activeNodeId;
    // A fresh seed covers saves made before the seed was recorded; restoreEncounterSeed puts
    // back the saved one when there is one.
    this.encounterSeed = new Random().nextLong();
    return true;
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
  public Integer getMapProgression() {
    Integer mapNodeId = this.getActiveNodeId();
    MapNode activeNode = mapGraph.getNode(mapNodeId);

    if (mapNodeId == null || !Objects.nonNull(activeNode) || activeNode.getHeight() <= 0) {
      return 0;
    }

    return mapGraph.getNode(this.getActiveNodeId()).getHeight();
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

  /**
   * Abandons the in-progress encounter without recording a result, reverting the map to the
   * player's position before they entered it.
   *
   * @return true if an encounter was actually abandoned
   */
  public boolean abandonEncounter() {
    if (mapGraph == null || activeNodeId == null) {
      logger.warn("Abandon requested but no encounter was active");
      return false;
    }

    boolean reverted = mapGraph.abandonCurrentNode();
    activeNodeId = null;
    return reverted;
  }

  public void endRun() {
    mapGraph = null;
    activeNodeId = null;
    encounterSeed = null;
    playerDeck = null;
    playerHealth = 0;
    playerMaxHealth = 0;
    playerMaxEnergy = 0;
    playerStatsInitialised = false;
    playerState = null;
  }

  private RewardOption pendingReward;

  public void setPendingReward(RewardOption reward) {
    this.pendingReward = reward;
  }

  public RewardOption getPendingReward() {
    return pendingReward;
  }

  public void clearPendingReward() {
    this.pendingReward = null;
  }
}
