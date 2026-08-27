package com.csse3200.game.cards;

/** The category of a card, describing its role in the deck rather than what it does. */
public enum CardType {
  /** Deals damage to a target. */
  ATTACK,

  /** Provides utility, such as block, buffs or card draw, without dealing direct damage. */
  SKILL,

  /** Applies a persistent effect that lasts for the remainder of combat. */
  POWER,

  /** A negative, usually unplayable card added as a temporary burden. */
  STATUS,

  /** A negative, permanent burden that is harder to remove than a status card. */
  CURSE
}
