package com.csse3200.game.cards.runtime;

/**
 * One card copy owned by the player at runtime.
 *
 * <p>This is an immutable data object, not a libGDX/Ashley ECS entity. The {@code instanceId}
 * identifies this exact copy while {@code cardId} identifies its shared card definition.
 *
 * @param instanceId stable identifier for this owned copy
 * @param cardId identifier used to retrieve the shared card definition
 * @param upgradeLevel zero for the base card or one for the upgraded card
 */
public record CardInstance(String instanceId, String cardId, int upgradeLevel) {
  public static final int BASE_LEVEL = 0;
  public static final int UPGRADED_LEVEL = 1;

  /** Validates runtime identity whenever an instance is constructed. */
  public CardInstance {
    validateIdentifier(instanceId, "instanceId");
    validateIdentifier(cardId, "cardId");
    if (upgradeLevel != BASE_LEVEL && upgradeLevel != UPGRADED_LEVEL) {
      throw new IllegalArgumentException("upgradeLevel must be 0 or 1, was " + upgradeLevel);
    }
  }

  /**
   * @return true when this copy uses its card definition's upgraded values
   */
  public boolean isUpgraded() {
    return upgradeLevel == UPGRADED_LEVEL;
  }

  /**
   * Returns an upgraded copy while preserving this card's stable identity.
   *
   * @return a new upgraded instance with the same instance and card IDs
   * @throws IllegalStateException if this card is already upgraded
   */
  public CardInstance upgrade() {
    if (isUpgraded()) {
      throw new IllegalStateException("Card instance is already upgraded: " + instanceId);
    }
    return new CardInstance(instanceId, cardId, UPGRADED_LEVEL);
  }

  private static void validateIdentifier(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or blank");
    }
  }
}
