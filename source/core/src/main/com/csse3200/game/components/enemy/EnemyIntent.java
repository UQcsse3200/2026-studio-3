package com.csse3200.game.components.enemy;

import java.util.Objects;

/** An enemy's telegraphed action for the coming round. */
public class EnemyIntent {
  private final IntentType type;
  private final int value;
  private final IntentEffectType effectType;
  private final int duration;

  /**
   * Creates an intent that inflicts no status effect.
   *
   * @param type category of the telegraphed action
   * @param value magnitude of the action, such as damage dealt or armor gained
   */
  public EnemyIntent(IntentType type, int value) {
    this(type, value, null, 0);
  }

  /**
   * Creates an intent, optionally carrying a status effect to inflict.
   *
   * @param type category of the telegraphed action
   * @param value magnitude of the action, or the magnitude of the status effect
   * @param effectType status effect to inflict, or null if the action inflicts none
   * @param duration turns the status effect lasts; 0 or fewer means it does not expire
   */
  public EnemyIntent(IntentType type, int value, IntentEffectType effectType, int duration) {
    this.type = type;
    this.value = value;
    this.effectType = effectType;
    this.duration = duration;
  }

  public static EnemyIntent attack(int damage) {
    return new EnemyIntent(IntentType.ATTACK, damage);
  }

  public static EnemyIntent defend(int armor) {
    return new EnemyIntent(IntentType.DEFEND, armor);
  }

  /**
   * Creates an intent that inflicts a status effect on the target.
   *
   * @param effectType status effect to inflict
   * @param value magnitude of the effect
   * @param duration turns the effect lasts; 0 or fewer means it does not expire
   * @return a DEBUFF intent carrying the given effect
   */
  public static EnemyIntent debuff(IntentEffectType effectType, int value, int duration) {
    return new EnemyIntent(IntentType.DEBUFF, value, effectType, duration);
  }

  public static EnemyIntent unknown() {
    return new EnemyIntent(IntentType.UNKNOWN, 0);
  }

  public IntentType getType() {
    return type;
  }

  public int getValue() {
    return value;
  }

  /**
   * @return the status effect this intent inflicts, or null if it inflicts none
   */
  public IntentEffectType getEffectType() {
    return effectType;
  }

  /**
   * @return turns the inflicted status effect lasts; 0 or fewer means it does not expire
   */
  public int getDuration() {
    return duration;
  }

  /** Two intents are equal when their type, value, effect type and duration all match. */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EnemyIntent)) {
      return false;
    }
    EnemyIntent that = (EnemyIntent) other;
    return value == that.value
        && duration == that.duration
        && type == that.type
        && effectType == that.effectType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, value, effectType, duration);
  }

  @Override
  public String toString() {
    if (effectType == null) {
      return type + "(" + value + ")";
    }
    return type + "(" + effectType + "," + value + "," + duration + ")";
  }
}
