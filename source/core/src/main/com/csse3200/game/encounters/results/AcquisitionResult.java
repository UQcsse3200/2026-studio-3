package com.csse3200.game.encounters.results;

import java.util.List;
import java.util.Objects;

/**
 * Describes already-applied changes for presentation. It never grants a reward or applies an
 * effect.
 */
public record AcquisitionResult(String resultId, List<Entry> entries) {
  public AcquisitionResult {
    requireText(resultId, "resultId");
    entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("Result must contain at least one entry");
    }
  }

  public enum Kind {
    CARD,
    ITEM,
    POSITIVE_EFFECT,
    NEGATIVE_EFFECT
  }

  /** Quantity is a count for goods or an absolute magnitude for effects; kind carries the sign. */
  public record Entry(
      Kind kind, String id, int quantity, String name, String description, Integer durationTurns) {
    public Entry {
      Objects.requireNonNull(kind, "kind");
      requireText(id, "id");
      requireText(name, "name");
      description = Objects.requireNonNull(description, "description");
      if (quantity <= 0 || (durationTurns != null && durationTurns <= 0)) {
        throw new IllegalArgumentException("Quantity and explicit duration must be positive");
      }
      if ((kind == Kind.CARD || kind == Kind.ITEM) && durationTurns != null) {
        throw new IllegalArgumentException("Goods do not have an effect duration");
      }
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
