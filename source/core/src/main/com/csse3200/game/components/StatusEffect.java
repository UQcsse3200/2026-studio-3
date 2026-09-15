package com.csse3200.game.components;

/**
 * Represents a single active status effect on a combat entity, such as Vulnerable or Weak. A status
 * effect has a type identifier, a value used to modify combat calculations, and a duration measured
 * in remaining turns.
 *
 * <p>NOTE: field names (type, value, duration) are aligned with Team 6's EffectConfig for
 * consistency. type is a String here (e.g. "VULNERABLE") rather than Team 6's EffectType enum,
 * since this class does not depend on Team 6's code - callers should pass EffectType.name() or an
 * equivalent String when constructing a StatusEffect from Team 6/5 data.
 *
 * <p>NOTE: value is an int (aligned with Team 6's EffectConfig.value, which represents a number of
 * stacks). This class does not interpret what the value/stacks mean for any specific effect type -
 * that calculation is owned elsewhere (see CombatStatsComponent javadoc).
 *
 * <p>NOTE: value is mutable (not final) so that a status effect's stack count can be adjusted at
 * runtime - e.g. stacking additional Poison onto an already-active effect, or a card that removes
 * some stacks. How/when to call setValue()/addValue() (e.g. whether repeated application should
 * stack or overwrite) is a design decision owned by whichever teammate implements that specific
 * effect's behaviour, not by this class.
 *
 * <p>NOTE: a duration of 0 or less represents a permanent effect that does not expire from ticking
 * (e.g. Strength, which Team 6 defines with duration=0 and lasts for the rest of combat). Only a
 * positive duration counts down and eventually expires.
 */
public class StatusEffect {

  private final String type;
  private int value;
  private int duration;

  /**
   * Creates a new status effect.
   *
   * @param type identifier for the effect, e.g. "VULNERABLE"
   * @param value magnitude of the effect (e.g. number of stacks), interpretation is defined
   *     elsewhere per effect type
   * @param duration number of turns the effect remains active for; 0 or less means permanent (does
   *     not expire from ticking)
   */
  public StatusEffect(String type, int value, int duration) {
    this.type = type;
    this.value = value;
    this.duration = duration;
  }

  /**
   * Returns the type identifier of this effect.
   *
   * @return effect type
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the magnitude/stack value of this effect.
   *
   * @return effect value
   */
  public int getValue() {
    return value;
  }

  /**
   * Overwrites the value directly, replacing whatever it was before.
   *
   * @param value new value
   */
  public void setValue(int value) {
    this.value = value;
  }

  /**
   * Adjusts the value by the given amount. Pass a positive amount to stack more of the effect on,
   * or a negative amount to reduce it (e.g. a card that removes some stacks).
   *
   * @param amount amount to add to the current value; can be negative
   */
  public void addValue(int amount) {
    this.value += amount;
  }

  /**
   * Returns the number of turns remaining before this effect expires. 0 or less means the effect is
   * permanent and will not expire from ticking.
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
    if (duration <= 0) {
      return false;
    }
    duration--;
    return duration <= 0;
  }
}
