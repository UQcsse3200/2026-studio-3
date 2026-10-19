package com.csse3200.game.chance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable chance and weighted item pool for a single optional Curse reward. */
public record ChanceCurseRisk(double triggerChance, List<WeightedItem> items) {
  public ChanceCurseRisk {
    if (!Double.isFinite(triggerChance) || triggerChance < 0 || triggerChance > 1) {
      throw new IllegalArgumentException("Curse trigger chance must be between 0 and 1");
    }
    items = List.copyOf(items);
    if (items.isEmpty()) throw new IllegalArgumentException("Curse item pool must not be empty");
    Set<String> ids = new HashSet<>();
    long total = 0;
    for (WeightedItem item : items) {
      if (!ids.add(item.id()))
        throw new IllegalArgumentException("Duplicate Curse item ID: " + item.id());
      total += item.weight();
    }
    if (total > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Curse weight total exceeds integer range");
  }

  /** One authoritative item identifier with a positive relative selection weight. */
  public record WeightedItem(String id, int weight) {
    public WeightedItem {
      if (id == null || !id.matches("[A-Z][A-Z0-9_]*") || weight <= 0) {
        throw new IllegalArgumentException(
            "Curse item requires an enum-name ID and positive weight");
      }
    }
  }
}
