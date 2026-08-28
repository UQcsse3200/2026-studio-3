package com.csse3200.game.components;

/**
 * Represents a single active status effect on a combat entity, such as Vulnerable or Weak. A status
 * effect has an identifier, a value used to modify combat calculations, and a duration measured in
 * remaining turns.
 */
public class StatusEffect {

  private final String effectId;
  private final float effectValue;
  private int duration;

  /**
   * Creates a new status effect.
   *
   * @param effectId identifier for the effect, e.g. "vulnerable"
   * @param effectValue value used when applying the effect, e.g. 0.5f for +50% incoming damage
   * @param duration number of turns the effect remains active for
   */
  public StatusEffect(String effectId, float effectValue, int duration) {
    this.effectId = effectId;
    this.effectValue = effectValue;
    this.duration = duration;
  }

  /**
   * Returns the identifier of this effect.
   *
   * @return effect id
   */
  public String getEffectId() {
    return effectId;
  }

  /**
   * Returns the modifier value of this effect.
   *
   * @return effect value
   */
  public float getEffectValue() {
    return effectValue;
  }

  /**
   * Returns the number of turns remaining before this effect expires.
   *
   * @return remaining duration
   */
  public int getDuration() {
    return duration;
  }

  /**
   * Called when a turn ends. Decrements the remaining duration by 1.
   *
   * @return true if the effect has now expired (duration reached 0 or below) and should be removed
   */
  public boolean tickAndCheckExpired() {
    duration--;
    return duration <= 0;
  }
}
