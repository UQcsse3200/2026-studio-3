package com.csse3200.game.cards.effects;

/**
 * Runtime boundary used by card effects to interact with the player's combat stats.
 *
 * <p>Team 5 resolves what a card is trying to do. The player stats system remains responsible for
 * storing the actual player health, block, energy, and status values behind this gateway.
 */
public interface CharacterEffectGateway {
  /** Current outgoing damage modifier from player-side stats such as strength. */
  int getStrengthModifier();

  /** Grants block to the player. */
  void gainBlock(int amount);

  /** Restores player health. */
  void heal(int amount);

  /** Grants player strength for the remainder of combat. */
  void gainStrength(int amount);
}
