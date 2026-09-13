package com.csse3200.game.cards.effects;

/**
 * Immutable combat-state snapshot used while resolving one card.
 *
 * <p>The values are read from other teams' state through adapters before Team 5 calculates card
 * effects. Team 5 uses them to return final pre-mitigation damage, while Team 1/Team 7 still own
 * block, armor, HP, statuses and death checks.
 *
 * @param strength flat outgoing damage bonus
 * @param outgoingFeeble active Feeble value on the attacker; any positive value applies the fixed
 *     Feeble damage multiplier
 * @param targetVulnerable active Vulnerable value on the target; any positive value applies the
 *     fixed Vulnerable damage multiplier
 */
public record CardEffectResolutionContext(int strength, int outgoingFeeble, int targetVulnerable) {
  private static final double FEEBLE_DAMAGE_MULTIPLIER = 0.75;
  private static final double VULNERABLE_DAMAGE_MULTIPLIER = 1.5;

  public CardEffectResolutionContext {
    outgoingFeeble = Math.max(0, outgoingFeeble);
    targetVulnerable = Math.max(0, targetVulnerable);
  }

  /** Creates a context using only Team 5's backwards-compatible calculation state. */
  public static CardEffectResolutionContext from(PlayerEffectState playerState) {
    if (playerState == null) {
      throw new IllegalArgumentException("Player effect state cannot be null");
    }
    return new CardEffectResolutionContext(playerState.getStrength(), 0, 0);
  }

  /** Applies Strength, Feeble and Vulnerable once, then rounds final damage down. */
  public int resolveDamage(int baseDamage) {
    int damage = Math.max(0, baseDamage + strength);
    double multiplier = 1.0;

    if (outgoingFeeble > 0) {
      multiplier *= FEEBLE_DAMAGE_MULTIPLIER;
    }
    if (targetVulnerable > 0) {
      multiplier *= VULNERABLE_DAMAGE_MULTIPLIER;
    }

    return Math.max(0, (int) Math.floor(damage * multiplier));
  }
}
