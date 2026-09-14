package com.csse3200.game.bestiary;

import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyTier;

/**
 * Immutable Bestiary definition derived from an enemy configuration.
 *
 * <p>Combat-owned values are copied when the Bestiary is initialised. Player-specific discovery
 * progress is deliberately stored by {@link BestiaryService}, not in this definition.
 *
 * @param enemyId stable identifier shared with the enemy roster
 * @param name enemy display name
 * @param tier enemy difficulty tier
 * @param health base health before run scaling
 * @param baseAttack base attack before run scaling
 * @param armour base armour before run scaling
 * @param behaviour enemy behaviour identifier
 * @param sprite optional explicit sprite atlas path
 * @param description optional Bestiary-specific description
 */
public record BestiaryEntry(
    String enemyId,
    String name,
    EnemyTier tier,
    int health,
    int baseAttack,
    int armour,
    String behaviour,
    String sprite,
    String description) {
  private static final String ENEMY_SPRITE_DIR = "images/enemies/";

  /** Creates a validated and normalised Bestiary definition. */
  public BestiaryEntry {
    enemyId = enemyId == null ? null : enemyId.trim();
    if (enemyId == null || enemyId.isBlank() || "unknown".equals(enemyId)) {
      throw new IllegalArgumentException("enemyId must identify a registered enemy");
    }
    if (health <= 0) {
      throw new IllegalArgumentException("health must be positive");
    }
    if (baseAttack < 0 || armour < 0) {
      throw new IllegalArgumentException("combat values must not be negative");
    }

    name = name == null || name.isBlank() ? "Unknown Enemy" : name.trim();
    tier = tier == null ? EnemyTier.NORMAL : tier;
    behaviour = normaliseOptionalText(behaviour);
    sprite = normaliseOptionalText(sprite);
    if (sprite.isEmpty()) {
      sprite = ENEMY_SPRITE_DIR + enemyId + ".atlas";
    }
    description = normaliseOptionalText(description);
  }

  /**
   * Creates a Bestiary definition using an enemy's base configuration.
   *
   * @param config source enemy configuration
   * @return immutable Bestiary definition
   */
  public static BestiaryEntry fromEnemyConfig(EnemyConfig config) {
    return fromEnemyConfig(config, "");
  }

  /**
   * Creates a Bestiary definition with additional Bestiary-specific descriptive text.
   *
   * @param config source enemy configuration
   * @param description optional Bestiary description
   * @return immutable Bestiary definition
   */
  public static BestiaryEntry fromEnemyConfig(EnemyConfig config, String description) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    return new BestiaryEntry(
        config.id,
        config.name,
        config.tier,
        config.health,
        config.baseAttack,
        config.armour,
        config.behaviour,
        config.sprite,
        description);
  }

  private static String normaliseOptionalText(String value) {
    return value == null ? "" : value.trim();
  }
}
