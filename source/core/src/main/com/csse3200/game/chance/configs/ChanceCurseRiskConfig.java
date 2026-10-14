package com.csse3200.game.chance.configs;

/** Optional configuration for granting one weighted Curse item after a probability check. */
public class ChanceCurseRiskConfig {
  /** Required probability between zero and one, inclusive. */
  public Double triggerChance;

  /** Required nonempty pool, with unique IDs and positive integer weights. */
  public Item[] items;

  /** One item ID and its relative selection weight. */
  public static class Item {
    /** Required authoritative ItemType name; actual existence is checked before settlement. */
    public String id;

    /** Required positive relative selection weight. */
    public Integer weight;
  }
}
