package com.csse3200.game.cards.play;

import com.csse3200.game.cards.TargetType;

/** Immutable target selected by the battle flow for one card play attempt. */
public record CardPlayTarget(TargetType type, String targetId) {
  public CardPlayTarget {
    if (type == null) {
      throw new IllegalArgumentException("Card play target type cannot be null");
    }

    if (targetId != null && !targetId.equals(targetId.trim())) {
      throw new IllegalArgumentException("Target ID cannot have leading or trailing whitespace");
    }
    if (type == TargetType.SINGLE_ENEMY && (targetId == null || targetId.isBlank())) {
      throw new IllegalArgumentException("A single-enemy target must include a target ID");
    }
    if (type != TargetType.SINGLE_ENEMY && targetId != null) {
      throw new IllegalArgumentException("Only a single-enemy target can include a target ID");
    }
  }

  /**
   * @return a player/self target
   */
  public static CardPlayTarget self() {
    return new CardPlayTarget(TargetType.SELF, null);
  }

  /**
   * @return one selected enemy, identified by an opaque battle-flow ID
   */
  public static CardPlayTarget singleEnemy(String targetId) {
    return new CardPlayTarget(TargetType.SINGLE_ENEMY, targetId);
  }

  /**
   * @return every currently valid enemy
   */
  public static CardPlayTarget allEnemies() {
    return new CardPlayTarget(TargetType.ALL_ENEMIES, null);
  }
}
