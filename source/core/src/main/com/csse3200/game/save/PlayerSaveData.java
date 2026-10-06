package com.csse3200.game.save;

import java.util.ArrayList;
import java.util.List;

/** Serializable player values that persist for the current run. */
public class PlayerSaveData {
  public int currentHealth;
  public int maxHealth;
  public int gold;
  public int piety;
  public List<String> ownedItems = new ArrayList<>();

  /** Required for JSON deserialisation. */
  public PlayerSaveData() {}

  public PlayerSaveData(int currentHealth, int maxHealth, int gold, int piety) {
    this(currentHealth, maxHealth, gold, piety, List.of());
  }

  public PlayerSaveData(
      int currentHealth, int maxHealth, int gold, int piety, List<String> ownedItems) {
    this.currentHealth = currentHealth;
    this.maxHealth = maxHealth;
    this.gold = gold;
    this.piety = piety;
    if (ownedItems != null) {
      this.ownedItems.addAll(ownedItems);
    }
  }
}
