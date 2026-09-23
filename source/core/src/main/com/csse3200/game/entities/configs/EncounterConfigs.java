package com.csse3200.game.entities.configs;

import com.csse3200.game.maps.RoomType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Table of all hand-authored encounters, loaded from {@code configs/encounters.json}.
 *
 * <p>Invalid entries are skipped with a warning rather than failing the whole table, consistent
 * with {@link EnemyConfigs}.
 */
public class EncounterConfigs {
  private static final Logger logger = LoggerFactory.getLogger(EncounterConfigs.class);
  public EncounterConfig[] encounters = new EncounterConfig[0];
  private transient Map<String, EncounterConfig> index;

  /**
   * Gets an encounter by id.
   *
   * @param id encounter id
   * @return the encounter, or null if it does not exist or was invalid
   */
  public EncounterConfig get(String id) {
    return index().get(id);
  }

  /**
   * Gets every valid encounter, in file order.
   *
   * @return list of valid encounters
   */
  public List<EncounterConfig> all() {
    return new ArrayList<>(index().values());
  }

  /**
   * Gets every valid encounter that can appear in the given room at the given progression.
   *
   * @param type room type of the node being entered
   * @param progression map progression of the node being entered
   * @return matching encounters in file order, possibly empty
   */
  public List<EncounterConfig> matching(RoomType type, int progression) {
    List<EncounterConfig> result = new ArrayList<>();
    for (EncounterConfig config : index().values()) {
      if (config.matches(type, progression)) {
        result.add(config);
      }
    }
    return result;
  }

  private Map<String, EncounterConfig> index() {
    if (index == null) {
      index = new LinkedHashMap<>();
      for (EncounterConfig config : encounters) {
        if (isValid(config)) {
          index.put(config.id, config);
        }
      }
    }
    return index;
  }

  private boolean isValid(EncounterConfig config) {
    if (config == null) {
      logger.warn("Skipping null encounter config");
      return false;
    }
    if (config.id == null || config.id.isBlank() || config.id.equals("unknown")) {
      logger.warn("Skipping encounter config with missing id");
      return false;
    }
    if (index.containsKey(config.id)) {
      logger.warn("Skipping duplicate encounter id '{}'", config.id);
      return false;
    }
    if (config.roomType == null) {
      logger.warn("Skipping encounter '{}' with missing room type", config.id);
      return false;
    }
    if (config.weight <= 0) {
      logger.warn("Skipping encounter '{}' with invalid weight: {}", config.id, config.weight);
      return false;
    }
    if (config.minProgression < 0 || config.minProgression > config.maxProgression) {
      logger.warn(
          "Skipping encounter '{}' with invalid progression range: {}..{}",
          config.id,
          config.minProgression,
          config.maxProgression);
      return false;
    }
    return hasValidEnemies(config);
  }

  private boolean hasValidEnemies(EncounterConfig config) {
    if (config.enemies == null || config.enemies.length == 0) {
      logger.warn("Skipping encounter '{}' with no enemies", config.id);
      return false;
    }
    for (String enemyId : config.enemies) {
      if (enemyId == null || enemyId.isBlank()) {
        logger.warn("Skipping encounter '{}' with a blank enemy id", config.id);
        return false;
      }
    }
    return true;
  }
}
