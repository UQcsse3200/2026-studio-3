package com.csse3200.game.components.combat;

/**
 * Defines the predetermined states of the Finite State Machine. Represents the current phase of the
 * deterministic game loop.
 */
public enum BattlePhase {
  // Beginning States
  /** Initial battle preparation before turns begin. */
  SETUP,

  /** Enemy intentions are determined and shown to the player. */
  REVEAL_INTENTS,

  // Player States
  /** Start-of-player-turn effects and resources are processed. */
  PLAYER_START,

  /** The controller is waiting for the player to choose an action. */
  PLAYER_TURN,

  /** The player's selected attack is being resolved. */
  PLAYER_ATTACK,

  /** The player's selected defensive action is being resolved. */
  PLAYER_DEFEND,

  /** The player's selected non-attack, non-defence action is being resolved. */
  PLAYER_OTHER,

  /** End-of-player-turn effects and cleanup are being processed. */
  PLAYER_END,

  /** A player action has resolved and its outcome is being checked. */
  PLAYER_RESOLVED,

  // Enemy States
  /** The current enemy is choosing or beginning its action. */
  ENEMY_TURN,

  /** The current enemy's attack is being resolved. */
  ENEMY_ATTACK,

  /** The current enemy's defensive action is being resolved. */
  ENEMY_DEFEND,

  /** The current enemy's non-attack, non-defence action is being resolved. */
  ENEMY_OTHER,

  /** An enemy action has resolved and its outcome is being checked. */
  ENEMY_RESOLVED,

  // Terminal States
  /** Terminal state reached when every enemy has been defeated. */
  VICTORY,

  /** Terminal state reached when the player has been defeated. */
  DEFEAT;
}
