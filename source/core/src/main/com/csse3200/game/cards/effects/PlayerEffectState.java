package com.csse3200.game.cards.effects;

/** Player-side effect state that Team 5 needs when resolving later card effects. */
public class PlayerEffectState {
  private int strength;

  /** Creates player effect state with no active modifiers. */
  public PlayerEffectState() {}

  /**
   * Creates player effect state with an initial strength modifier.
   *
   * @param strength current player strength
   */
  public PlayerEffectState(int strength) {
    this.strength = strength;
  }

  /**
   * @return current outgoing damage modifier from strength
   */
  public int getStrength() {
    return strength;
  }

  /**
   * Adds strength to the Team 5 calculation state.
   *
   * @param amount strength amount to add
   */
  public void addStrength(int amount) {
    strength += amount;
  }
}
