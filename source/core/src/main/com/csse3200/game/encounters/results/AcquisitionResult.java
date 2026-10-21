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
    NEGATIVE_EFFECT,
    GOLD,
    HP
  }

  /**
   * Quantity is a positive count for goods or absolute magnitude for effects. For GOLD and HP,
   * quantity is a signed display amount, not a final balance. Positive Event HP displays the
   * configured healing amount; actual health is independently capped by settlement. Other resource
   * entries display the committed change.
   */
  public record Entry(
      Kind kind, String id, int quantity, String name, String description, Integer durationTurns) {
    public Entry {
      Objects.requireNonNull(kind, "kind");
      requireText(id, "id");
      requireText(name, "name");
      Objects.requireNonNull(description, "description");
      if (kind == Kind.GOLD || kind == Kind.HP) {
        if (quantity == 0 || durationTurns != null) {
          throw new IllegalArgumentException(
              "Resource changes must be nonzero and have no duration");
        }
      } else {
        if (quantity <= 0 || (durationTurns != null && durationTurns <= 0)) {
          throw new IllegalArgumentException("Quantity and explicit duration must be positive");
        }
        if ((kind == Kind.CARD || kind == Kind.ITEM) && durationTurns != null) {
          throw new IllegalArgumentException("Goods do not have an effect duration");
        }
      }
    }

    public static Entry gold(int delta) {
      return new Entry(Kind.GOLD, "gold", delta, "Gold", "Change in gold", null);
    }

    public static Entry hp(int delta) {
      return new Entry(Kind.HP, "hp", delta, "HP", "Change in health", null);
    }

    public boolean isResourceChange() {
      return kind == Kind.GOLD || kind == Kind.HP;
    }

    /**
     * Returns the actual signed resource change; goods and effects do not have a resource delta.
     */
    public int delta() {
      if (!isResourceChange()) {
        throw new IllegalStateException("Only GOLD and HP entries have a resource delta");
      }
      return quantity;
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
