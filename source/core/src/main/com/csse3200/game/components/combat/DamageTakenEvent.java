package com.csse3200.game.components.combat;

import java.util.Objects;

/**
 * Describes the actual health lost from one instance of damage.
 *
 * <p>The amount is the health difference after block and armour have been applied, rather than the
 * incoming raw damage. Listeners can therefore display feedback that always matches the health bar.
 *
 * @param amount actual health lost, never negative
 * @param type category of damage that caused the loss
 */
public record DamageTakenEvent(int amount, DamageType type) {

  public DamageTakenEvent {
    if (amount < 0) {
      throw new IllegalArgumentException("Damage amount cannot be negative");
    }

    Objects.requireNonNull(type, "Damage type cannot be null");
  }
}
