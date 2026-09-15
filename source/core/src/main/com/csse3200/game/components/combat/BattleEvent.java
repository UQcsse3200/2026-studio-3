package com.csse3200.game.components.combat;

/** Enumerates the permitted inputs to the FSM. These are used to validity check transitions. */
public enum BattleEvent {
  // Battle setup
  /** Battle setup has finished and intents can now be revealed. */
  SETUP_COMPLETE,

  /** All enemy intents have been revealed and the player phase can begin. */
  INTENTS_REVEALED,

  // Player phase
  /** Start-of-turn processing has finished and player input can be accepted. */
  PLAYER_TURN_STARTED,

  /** The player selected an attack action. */
  PLAYER_ATTACK_SELECTED,

  /** The player selected a defensive action. */
  PLAYER_DEFEND_SELECTED,

  /** The player selected an action that is neither attack nor defence. */
  PLAYER_OTHER_SELECTED,

  /** The player requested to finish their turn. */
  PLAYER_END_REQUESTED,

  /** The selected player action has finished resolving. */
  PLAYER_ACTION_RESOLVED,

  /** The player may select another action during the same turn. */
  PLAYER_CONTINUES,

  /** End-of-player-turn processing has finished. */
  PLAYER_TURN_ENDED,

  // Enemy phase
  /** The current enemy selected an attack intent. */
  ENEMY_ATTACK_SELECTED,

  /** The current enemy selected a defensive intent. */
  ENEMY_DEFEND_SELECTED,

  /** The current enemy selected an intent that is neither attack nor defence. */
  ENEMY_OTHER_SELECTED,

  /** The current enemy's action has finished resolving. */
  ENEMY_ACTION_RESOLVED,

  /** Another eligible enemy is available to take a turn. */
  MORE_ENEMIES,

  /** Every eligible enemy has acted and the enemy phase is finished. */
  ENEMY_PHASE_COMPLETE,

  // Battle outcomes
  /** All enemies have been defeated, so the battle is won. */
  ENEMIES_DEFEATED,

  /** The player has been defeated, so the battle is lost. */
  PLAYER_DEFEATED
}
