package com.csse3200.game.components.combat;

import java.util.EnumMap;
import java.util.Map;

/** Helper class for deciding which phase transitions are allowed. */
public class BattleTransitions {
  private final Map<BattlePhase, Map<BattleEvent, BattlePhase>> allowedTransitions =
      new EnumMap<>(BattlePhase.class);

  public BattleTransitions() {
    // Set up Transitions
    this.addTransition(BattlePhase.SETUP, BattleEvent.SETUP_COMPLETE, BattlePhase.REVEAL_INTENTS);

    this.addTransition(
        BattlePhase.REVEAL_INTENTS, BattleEvent.INTENTS_REVEALED, BattlePhase.PLAYER_START);

    this.addTransition(
        BattlePhase.PLAYER_START, BattleEvent.PLAYER_TURN_STARTED, BattlePhase.PLAYER_TURN);

    this.addTransition(BattlePhase.PLAYER_START, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY);

    this.addTransition(BattlePhase.PLAYER_START, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT);

    // Player Transitions
    this.addTransition(
        BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_ATTACK_SELECTED, BattlePhase.PLAYER_ATTACK);

    this.addTransition(
        BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_DEFEND_SELECTED, BattlePhase.PLAYER_DEFEND);

    this.addTransition(
        BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_OTHER_SELECTED, BattlePhase.PLAYER_OTHER);

    this.addTransition(
        BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_END_REQUESTED, BattlePhase.PLAYER_END);

    this.addTransition(
        BattlePhase.PLAYER_ATTACK, BattleEvent.PLAYER_ACTION_RESOLVED, BattlePhase.PLAYER_RESOLVED);

    this.addTransition(
        BattlePhase.PLAYER_DEFEND, BattleEvent.PLAYER_ACTION_RESOLVED, BattlePhase.PLAYER_RESOLVED);

    this.addTransition(
        BattlePhase.PLAYER_OTHER, BattleEvent.PLAYER_ACTION_RESOLVED, BattlePhase.PLAYER_RESOLVED);

    this.addTransition(
        BattlePhase.PLAYER_RESOLVED, BattleEvent.PLAYER_CONTINUES, BattlePhase.PLAYER_TURN);

    this.addTransition(
        BattlePhase.PLAYER_RESOLVED, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT);

    this.addTransition(
        BattlePhase.PLAYER_RESOLVED, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY);

    this.addTransition(BattlePhase.PLAYER_END, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT);

    this.addTransition(BattlePhase.PLAYER_END, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY);

    this.addTransition(
        BattlePhase.PLAYER_END, BattleEvent.PLAYER_TURN_ENDED, BattlePhase.ENEMY_TURN);

    // Enemy Transitions
    this.addTransition(
        BattlePhase.ENEMY_TURN, BattleEvent.ENEMY_ATTACK_SELECTED, BattlePhase.ENEMY_ATTACK);

    this.addTransition(
        BattlePhase.ENEMY_TURN, BattleEvent.ENEMY_DEFEND_SELECTED, BattlePhase.ENEMY_DEFEND);

    this.addTransition(
        BattlePhase.ENEMY_TURN, BattleEvent.ENEMY_OTHER_SELECTED, BattlePhase.ENEMY_OTHER);

    this.addTransition(
        BattlePhase.ENEMY_ATTACK, BattleEvent.ENEMY_ACTION_RESOLVED, BattlePhase.ENEMY_RESOLVED);

    this.addTransition(
        BattlePhase.ENEMY_DEFEND, BattleEvent.ENEMY_ACTION_RESOLVED, BattlePhase.ENEMY_RESOLVED);

    this.addTransition(
        BattlePhase.ENEMY_OTHER, BattleEvent.ENEMY_ACTION_RESOLVED, BattlePhase.ENEMY_RESOLVED);

    this.addTransition(BattlePhase.ENEMY_RESOLVED, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT);

    this.addTransition(
        BattlePhase.ENEMY_RESOLVED, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY);

    this.addTransition(
        BattlePhase.ENEMY_RESOLVED, BattleEvent.MORE_ENEMIES, BattlePhase.ENEMY_TURN);

    this.addTransition(
        BattlePhase.ENEMY_RESOLVED, BattleEvent.ENEMY_PHASE_COMPLETE, BattlePhase.REVEAL_INTENTS);
  }

  /** Helper function that adds allowed transitions to the transition table. */
  private void addTransition(
      BattlePhase currentPhase, BattleEvent incomingEvent, BattlePhase resultingPhase) {
    allowedTransitions
        .computeIfAbsent(currentPhase, ignored -> new EnumMap<>(BattleEvent.class))
        .put(incomingEvent, resultingPhase);
  }

  /**
   * Gets the phase reached by applying an event to the current phase. (mostly for testing purposes)
   *
   * @param currentPhase the phase before the event
   * @param incomingEvent the event to apply
   * @return the resulting phase, or {@code null} when the transition is not allowed
   */
  public BattlePhase getNextPhase(BattlePhase currentPhase, BattleEvent incomingEvent) {
    Map<BattleEvent, BattlePhase> transitions = allowedTransitions.get(currentPhase);
    // Below guards against null pointer exception
    return transitions == null ? null : transitions.get(incomingEvent);
  }
}
