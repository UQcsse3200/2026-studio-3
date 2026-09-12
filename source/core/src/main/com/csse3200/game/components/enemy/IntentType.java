package com.csse3200.game.components.enemy;

/** Category of action an enemy has telegraphed for the coming round. */
public enum IntentType {
  ATTACK,
  DEFEND,
  BUFF,
  /** Weakens the player. */
  DEBUFF,
  /** Intent is hidden or not yet decided. */
  UNKNOWN,
}
