package com.csse3200.game.components.enemy;

/**
 * A status effect an enemy intent can inflict on its target.
 *
 * <p>These constants are converted to strings at the boundary with {@link
 * com.csse3200.game.components.CombatStatsComponent#applyStatusEffect(String, int, int)}, which
 * stores active effects in a string-keyed map. Most effects use their {@code name()} as the key;
 * TAUNT appends the source enemy ID so multiple taunters can coexist.
 */
public enum IntentEffectType {
  /** Prevents the target from playing cards while active. */
  SILENCE,

  /** Damages the target each time it plays a card. */
  DAMAGE_ON_CARD_PLAY,

  /** Forces single-enemy cards to target an active taunting enemy. */
  TAUNT
}
