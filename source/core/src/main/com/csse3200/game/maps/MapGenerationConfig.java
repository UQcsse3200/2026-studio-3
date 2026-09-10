package com.csse3200.game.maps;

/**
 * Immutable configuration for generating and assigning room types to a map node
 * pool.
 */
public final class MapGenerationConfig {

  public static final int MAP_WIDTH = 7;
  public static final int MAP_HEIGHT = 10;
  public static final int MAX_NODE_COUNT = MAP_WIDTH * MAP_HEIGHT;
  public static final int BRANCH_CHANCE = 5;

  private int normalNodeCount = 70; // TODO: overlapping state, should weights also be constants? should this class
                                    // exist?

  // Weights are used as a fixed point decimal with a scaling factor of 100
  private int combatWeight = 70;
  private int eventWeight = 20;
  private int shopWeight = 10;
  private final Long seed;

  /**
   * Creates and validates a room distribution configuration with default values
   * and no seed.
   */
  public MapGenerationConfig() {
    checkValid(normalNodeCount, combatWeight, eventWeight, shopWeight);
    this.seed = System.nanoTime();
  }

  /**
   * Creates and validates a room distribution configuration without seed.
   *
   * @param normalNodeCount number of normal nodes
   * @param combatWeight    relative combat-room weight
   * @param eventWeight     relative event-room weight
   * @param shopWeight      relative shop-room weight
   */
  public MapGenerationConfig(
      int normalNodeCount, int combatWeight, int eventWeight, int shopWeight) {
    this(normalNodeCount, combatWeight, eventWeight, shopWeight, System.nanoTime());
  }

  /**
   * Creates and validates a room distribution configuration.
   *
   * @param normalNodeCount number of normal nodes
   * @param combatWeight    relative combat-room weight
   * @param eventWeight     relative event-room weight
   * @param shopWeight      relative shop-room weight
   * @param seed            seed used for repeatable random generation
   */
  public MapGenerationConfig(
      int normalNodeCount, int combatWeight, int eventWeight, int shopWeight, Long seed) {

    checkValid(normalNodeCount, combatWeight, eventWeight, shopWeight);

    this.normalNodeCount = normalNodeCount;
    this.combatWeight = combatWeight;
    this.eventWeight = eventWeight;
    this.shopWeight = shopWeight;
    this.seed = seed;
  }

  /** Checks that the node count and weights are valid. */
  private static void checkValid(
      int normalNodeCount, int combatWeight, int eventWeight, int shopWeight) {
    if (normalNodeCount < 1) {
      throw new IllegalArgumentException("Normal node count must be at least one!");
    }
    if (combatWeight < 0 || eventWeight < 0 || shopWeight < 0) {
      throw new IllegalArgumentException("Room weights cannot be negative!");
    }
    if (combatWeight + eventWeight + shopWeight == 0) {
      throw new IllegalArgumentException("At least one room weight must be positive!");
    }
  }

  /** Returns the number of nodes. */
  public int getNormalNodeCount() {
    return normalNodeCount;
  }

  /** Returns the combat-room weight. */
  public int getCombatWeight() {
    return combatWeight;
  }

  /** Returns the event-room weight. */
  public int getEventWeight() {
    return eventWeight;
  }

  /** Returns the shop-room weight. */
  public int getShopWeight() {
    return shopWeight;
  }

  /** Returns the seed. */
  public Long getSeed() {
    return seed;
  }

  /** Returns the combined room weight. */
  public int getTotalWeight() {
    return combatWeight + eventWeight + shopWeight;
  }
}
