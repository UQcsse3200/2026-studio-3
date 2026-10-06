package com.csse3200.game.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Serializable player values that persist for the current run.
 *
 * <p>Legacy schema-version-1 saves may use {@code piety} instead of {@code level}. Reading accepts
 * that alias only when {@code level} is absent; writing always uses {@code level}.
 */
public class PlayerSaveData implements Json.Serializable {
  private static final String LEVEL_KEY = "level";

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

  @Override
  public void write(Json json) {
    json.writeValue("currentHealth", currentHealth);
    json.writeValue("maxHealth", maxHealth);
    json.writeValue("gold", gold);
    json.writeValue(LEVEL_KEY, level);
  }

  @Override
  public void read(Json json, JsonValue jsonData) {
    currentHealth = jsonData.getInt("currentHealth", 0);
    maxHealth = jsonData.getInt("maxHealth", 0);
    gold = jsonData.getInt("gold", 0);
    level = jsonData.has(LEVEL_KEY) ? jsonData.getInt(LEVEL_KEY) : jsonData.getInt("piety", 0);
  }
}
