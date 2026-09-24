package com.csse3200.game.save;

/** Serializable player values that persist for the current run. */
public class PlayerSaveData {
  public int currentHealth;
  public int maxHealth;
  public int gold;
  public int level;

  /** Required for JSON deserialisation. */
  public PlayerSaveData() {}

  public PlayerSaveData(int currentHealth, int maxHealth, int gold, int level) {
    this.currentHealth = currentHealth;
    this.maxHealth = maxHealth;
    this.gold = gold;
    this.level = level;
  }
}
