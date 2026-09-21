package com.csse3200.game.entities.configs;

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
    return true;
  }
}
