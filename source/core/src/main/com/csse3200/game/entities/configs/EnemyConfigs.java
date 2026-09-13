package com.csse3200.game.entities.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Roster of all enemies, loaded from {@code configs/enemies.json}. */
public class EnemyConfigs {
  private static final Logger logger = LoggerFactory.getLogger(EnemyConfigs.class);
  public EnemyConfig[] enemies = new EnemyConfig[0];
  private transient Map<String, EnemyConfig> index;

  /**
   * Gets an enemy configuration by id.
   *
   * @param id enemy id
   * @return the enemy configuration, or null if not found
   */
  public EnemyConfig get(String id) {
    return index().get(id);
  }

  /**
   * Checks whether an enemy id exists in the roster.
   *
   * @param id enemy id
   * @return true if the enemy exists
   */
  public boolean contains(String id) {
    return index().containsKey(id);
  }

  /**
   * Gets all valid enemy ids in the roster.
   *
   * @return list of enemy ids
   */
  public List<String> ids() {
    return new ArrayList<>(index().keySet());
  }

  private Map<String, EnemyConfig> index() {
    if (index == null) {
      index = new HashMap<>();

      for (EnemyConfig config : enemies) {
        if (config == null) {
          logger.warn("Skipping null enemy config");
          continue;
        }

        if (config.id == null || config.id.isBlank() || config.id.equals("unknown")) {
          logger.warn("Skipping enemy config with missing id");
          continue;
        }

        if (config.health <= 0) {
          logger.warn(
              "Skipping enemy config '{}' with invalid health: {}", config.id, config.health);
          continue;
        }

        index.put(config.id, config);
      }
    }

    return index;
  }
}
