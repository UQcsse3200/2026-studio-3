package com.csse3200.game.components.enemy;

/**
 * Immutable snapshot of the player's recent combat behaviour.
 *
 * @param attackCardsPlayed total number of attack cards observed
 * @param skillCardsPlayed total number of skill cards observed
 * @param consecutiveTurnsWithoutBlock consecutive turns in which the player gained no block
 * @param cardsPlayedLastTurn number of cards played during the previous turn
 */
public record PlayerMemory(
    int attackCardsPlayed,
    int skillCardsPlayed,
    int consecutiveTurnsWithoutBlock,
    int cardsPlayedLastTurn) {

  private static final int ATTACK_FAVOUR_THRESHOLD = 4;
  private static final int ATTACK_TO_SKILL_RATIO = 2;
  private static final int UNDEFENDED_TURN_THRESHOLD = 3;

  private static final PlayerMemory EMPTY = new PlayerMemory(0, 0, 0, 0);

  /** Prevents invalid negative combat counters. */
  public PlayerMemory {
    if (attackCardsPlayed < 0
        || skillCardsPlayed < 0
        || consecutiveTurnsWithoutBlock < 0
        || cardsPlayedLastTurn < 0) {
      throw new IllegalArgumentException("Player memory values cannot be negative");
    }
  }

  /**
   * Whether the player strongly favours attack cards.
   *
   * @return true when at least four attack cards have been played and the attack count is more than
   *     twice the skill count
   */
  public boolean favoursAttack() {
    return attackCardsPlayed >= ATTACK_FAVOUR_THRESHOLD
        && (long) attackCardsPlayed > (long) ATTACK_TO_SKILL_RATIO * skillCardsPlayed;
  }

  /**
   * Whether the player has gone at least three consecutive turns without gaining block.
   *
   * @return true when the player is considered undefended
   */
  public boolean isUndefended() {
    return consecutiveTurnsWithoutBlock >= UNDEFENDED_TURN_THRESHOLD;
  }

  /**
   * Returns an empty memory snapshot instead of using {@code null}.
   *
   * @return the shared empty player-memory instance
   */
  public static PlayerMemory empty() {
    return EMPTY;
  }
}
