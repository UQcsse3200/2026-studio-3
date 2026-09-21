package com.csse3200.game.cards.play;

/** Immutable request sent by Team 3/battle flow when a player attempts to play one card. */
public record CardPlayRequest(String instanceId, CardPlayTarget target) {
  public CardPlayRequest {
    if (instanceId == null || instanceId.isBlank()) {
      throw new IllegalArgumentException("Instance ID cannot be null or blank");
    }
    if (!instanceId.equals(instanceId.trim())) {
      throw new IllegalArgumentException("Instance ID cannot have leading or trailing whitespace");
    }
    if (target == null) {
      throw new IllegalArgumentException("Card play target cannot be null");
    }
  }

  /** Converts a UI event using the selected card's resolved target type. */
  public static CardPlayRequest fromUi(
          String instanceId, com.csse3200.game.cards.TargetType type, String targetId) {
    return switch (type) {
      case SELF -> "player".equals(targetId) ? self(instanceId) : singleEnemy(instanceId, targetId);
      case SINGLE_ENEMY -> singleEnemy(instanceId, targetId);
      case ALL_ENEMIES -> allEnemies(instanceId);
    };
  }

  /** Creates a request for a self-targeting card. */
  public static CardPlayRequest self(String instanceId) {
    return new CardPlayRequest(instanceId, CardPlayTarget.self());
  }

  /** Creates a request for a card targeting one selected enemy. */
  public static CardPlayRequest singleEnemy(String instanceId, String targetId) {
    return new CardPlayRequest(instanceId, CardPlayTarget.singleEnemy(targetId));
  }

  /** Creates a request for a card targeting every enemy. */
  public static CardPlayRequest allEnemies(String instanceId) {
    return new CardPlayRequest(instanceId, CardPlayTarget.allEnemies());
  }
}
