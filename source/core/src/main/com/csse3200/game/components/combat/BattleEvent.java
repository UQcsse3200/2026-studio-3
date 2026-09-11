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

  /** The player submitted a card to resolve. */
  CARD_PLAY_REQUESTED,

  /** The player requested to finish their turn. */
  PLAYER_END_REQUESTED,

  /** Card resolution is complete and the player may play another card. */
  CARD_RESOLVED,

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
