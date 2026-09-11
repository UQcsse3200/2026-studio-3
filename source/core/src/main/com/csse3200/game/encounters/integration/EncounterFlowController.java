package com.csse3200.game.encounters.integration;

import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopService;
import java.util.Objects;

/**
 * Entry point for the Map-to-Encounter-to-Map lifecycle.
 *
 * <p>The controller permits one active encounter, creates Chance and Shop sessions with the real
 * integration gateways, and forwards completion to the Map exactly once for the active node.
 */
public final class EncounterFlowController implements EncounterCallback {
  /** Type of encounter currently controlled. */
  public enum EncounterType {
    /** A choice-based Chance Encounter. */
    CHANCE,
    /** A purchasable-card Shop Encounter. */
    SHOP
  }

  private final ChanceOutcomeApplier chanceOutcomeApplier;
  private final ShopTransactionGateway shopTransactions;
  private final EncounterCallback mapCallback;

  private Integer activeNodeId;
  private EncounterType activeType;

  /**
   * Creates a controller for the shared encounter lifecycle.
   *
   * @param player player state used by Chance outcomes
   * @param shopTransactions Player/Card/Deck boundary used by Shop purchases
   * @param mapCallback Map callback receiving final encounter completion
   */
  public EncounterFlowController(
      PlayerStateGateway player,
      ShopTransactionGateway shopTransactions,
      EncounterCallback mapCallback) {
    this.chanceOutcomeApplier = new ChanceOutcomeApplier(player);
    this.shopTransactions =
        Objects.requireNonNull(shopTransactions, "shopTransactions cannot be null");
    this.mapCallback = Objects.requireNonNull(mapCallback, "mapCallback cannot be null");
  }

  /**
   * Starts a Chance Encounter for a map node.
   *
   * @param nodeId selected map node
   * @param encounter encounter definition to resolve
   * @return session ready for the Chance UI
   * @throws IllegalStateException when another encounter is active
   */
  public ChanceEncounterSession startChance(Integer nodeId, ChanceEncounter encounter) {
    begin(nodeId, EncounterType.CHANCE);
    try {
      return new ChanceEncounterSession(nodeId, encounter, chanceOutcomeApplier, this);
    } catch (RuntimeException exception) {
      clearActiveEncounter();
      throw exception;
    }
  }

  /**
   * Starts a Shop Encounter for a map node.
   *
   * @param nodeId selected map node
   * @param shopService shop inventory and stock service
   * @return session ready for the Shop UI
   * @throws IllegalStateException when another encounter is active
   */
  public ShopEncounter startShop(Integer nodeId, ShopService shopService) {
    begin(nodeId, EncounterType.SHOP);
    return new ShopEncounter(nodeId, shopService, shopTransactions, this);
  }

  @Override
  public void onEncounterComplete(Integer nodeId, boolean success) {
    if (activeNodeId == null || !activeNodeId.equals(nodeId)) {
      return;
    }

    clearActiveEncounter();
    mapCallback.onEncounterComplete(nodeId, success);
  }

  /**
   * Reports whether the controller owns an unfinished encounter.
   *
   * @return true while a Chance or Shop encounter is active
   */
  public boolean isEncounterActive() {
    return activeNodeId != null;
  }

  /**
   * Returns the node for the current encounter.
   *
   * @return active map node ID, or null when no encounter is active
   */
  public Integer getActiveNodeId() {
    return activeNodeId;
  }

  /**
   * Returns the current encounter category.
   *
   * @return active encounter type, or null when no encounter is active
   */
  public EncounterType getActiveType() {
    return activeType;
  }

  private void begin(Integer nodeId, EncounterType type) {
    if (isEncounterActive()) {
      throw new IllegalStateException(
          String.format("Encounter for node %s is already active", activeNodeId));
    }
    if (nodeId == null) {
      throw new IllegalArgumentException("nodeId cannot be null");
    }
    activeNodeId = nodeId;
    activeType = type;
  }

  private void clearActiveEncounter() {
    activeNodeId = null;
    activeType = null;
  }
}
