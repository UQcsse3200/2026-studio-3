package com.csse3200.game.entities.configs;

/** Configuration for a single enemy, loaded from {@code configs/enemies.json}. */
public class EnemyConfig extends BaseEntityConfig {
  /** Unique key used to look this enemy up. */
  public String id = "unknown";

  public String name = "unknown Enemy";
  public int armour = 0;
  public EnemyTier tier = EnemyTier.NORMAL;

  /** Identifier of the behaviour pattern used each round. */
  public String behaviour = "cycle_attack_defend";

  /**
   * Optional texture atlas path override. When left blank, the factory renders the enemy with
   * {@code images/enemies/<id>.atlas} by convention, falling back to a shared default atlas.
   */
  public String sprite = "";
}
