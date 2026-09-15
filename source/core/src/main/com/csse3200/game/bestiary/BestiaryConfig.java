package com.csse3200.game.bestiary;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bestiary-owned presentation data loaded separately from enemy combat configuration. */
public class BestiaryConfig {
  public DescriptionEntry[] entries = new DescriptionEntry[0];

  /**
   * Returns valid descriptions keyed by stable enemy ID.
   *
   * @return immutable description lookup
   */
  public Map<String, String> descriptionsByEnemyId() {
    Map<String, String> descriptions = new LinkedHashMap<>();
    if (entries == null) {
      return Collections.unmodifiableMap(descriptions);
    }

    for (DescriptionEntry entry : entries) {
      if (entry != null
          && entry.enemyId != null
          && !entry.enemyId.isBlank()
          && entry.description != null
          && !entry.description.isBlank()) {
        descriptions.put(entry.enemyId, entry.description);
      }
    }
    return Collections.unmodifiableMap(descriptions);
  }

  /** One Bestiary description associated with an Enemy System ID. */
  public static class DescriptionEntry {
    public String enemyId = "";
    public String description = "";
  }
}
