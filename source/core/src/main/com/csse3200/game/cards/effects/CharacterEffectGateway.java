package com.csse3200.game.cards.effects;

/**
 * Runtime boundary used by card effects to modify a character.
 *
 * <p>The resolver deliberately depends on this small interface rather than a concrete player or
 * enemy implementation. Team 7 character and status components can be connected through an adapter
 * without coupling the card system to their internal representation.
 */
public interface CharacterEffectGateway {
  /** Applies immediate damage. */
  void damage(int amount);

  /** Grants block. */
  void gainBlock(int amount);

  /** Restores health. */
  void heal(int amount);

  /** Applies poison stacks for a fixed number of turns. */
  void applyPoison(int stacks, int duration);

  /** Applies vulnerable stacks for a fixed number of turns. */
  void applyVulnerable(int stacks, int duration);

  /** Grants strength for the remainder of combat. */
  void gainStrength(int amount);
}
