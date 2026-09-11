package com.csse3200.game.components.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BattleTransitionsTest {
  private BattleTransitions transitions;

  @BeforeEach
  void setUp() {
    transitions = new BattleTransitions();
  }

  @Test
  void shouldAllowDiagramTransitions() {
    for (Transition transition : diagramTransitions()) {
      assertEquals(
          transition.resultingPhase(),
          transitions.getNextPhase(transition.currentPhase(), transition.event()),
          () -> "Unexpected result for " + transition.currentPhase() + " + " + transition.event());
    }
  }

  @Test
  void shouldRejectTransitionsNotInDiagram() {
    Map<BattlePhase, Map<BattleEvent, BattlePhase>> expectedTransitions =
        new EnumMap<>(BattlePhase.class);
    for (Transition transition : diagramTransitions()) {
      expectedTransitions
          .computeIfAbsent(transition.currentPhase(), ignored -> new EnumMap<>(BattleEvent.class))
          .put(transition.event(), transition.resultingPhase());
    }

    for (BattlePhase phase : BattlePhase.values()) {
      for (BattleEvent event : BattleEvent.values()) {
        if (!expectedTransitions.getOrDefault(phase, Map.of()).containsKey(event)) {
          assertNull(
              transitions.getNextPhase(phase, event),
              () -> "Transition should not be allowed for " + phase + " + " + event);
        }
      }
    }
  }

  private static List<Transition> diagramTransitions() {
    return List.of(
        transition(BattlePhase.SETUP, BattleEvent.SETUP_COMPLETE, BattlePhase.REVEAL_INTENTS),
        transition(
            BattlePhase.REVEAL_INTENTS, BattleEvent.INTENTS_REVEALED, BattlePhase.PLAYER_START),
        transition(
            BattlePhase.PLAYER_START, BattleEvent.PLAYER_TURN_STARTED, BattlePhase.PLAYER_TURN),
        transition(BattlePhase.PLAYER_START, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY),
        transition(BattlePhase.PLAYER_START, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT),
        transition(
            BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_ATTACK_SELECTED, BattlePhase.PLAYER_ATTACK),
        transition(
            BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_DEFEND_SELECTED, BattlePhase.PLAYER_DEFEND),
        transition(
            BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_OTHER_SELECTED, BattlePhase.PLAYER_OTHER),
        transition(
            BattlePhase.PLAYER_TURN, BattleEvent.PLAYER_END_REQUESTED, BattlePhase.PLAYER_END),
        transition(
            BattlePhase.PLAYER_ATTACK,
            BattleEvent.PLAYER_ACTION_RESOLVED,
            BattlePhase.PLAYER_RESOLVED),
        transition(
            BattlePhase.PLAYER_DEFEND,
            BattleEvent.PLAYER_ACTION_RESOLVED,
            BattlePhase.PLAYER_RESOLVED),
        transition(
            BattlePhase.PLAYER_OTHER,
            BattleEvent.PLAYER_ACTION_RESOLVED,
            BattlePhase.PLAYER_RESOLVED),
        transition(
            BattlePhase.PLAYER_RESOLVED, BattleEvent.PLAYER_CONTINUES, BattlePhase.PLAYER_TURN),
        transition(BattlePhase.PLAYER_RESOLVED, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY),
        transition(BattlePhase.PLAYER_RESOLVED, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT),
        transition(BattlePhase.PLAYER_END, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT),
        transition(BattlePhase.PLAYER_END, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY),
        transition(BattlePhase.PLAYER_END, BattleEvent.PLAYER_TURN_ENDED, BattlePhase.ENEMY_TURN),
        transition(
            BattlePhase.ENEMY_TURN, BattleEvent.ENEMY_ATTACK_SELECTED, BattlePhase.ENEMY_ATTACK),
        transition(
            BattlePhase.ENEMY_TURN, BattleEvent.ENEMY_DEFEND_SELECTED, BattlePhase.ENEMY_DEFEND),
        transition(
            BattlePhase.ENEMY_TURN, BattleEvent.ENEMY_OTHER_SELECTED, BattlePhase.ENEMY_OTHER),
        transition(
            BattlePhase.ENEMY_ATTACK,
            BattleEvent.ENEMY_ACTION_RESOLVED,
            BattlePhase.ENEMY_RESOLVED),
        transition(
            BattlePhase.ENEMY_DEFEND,
            BattleEvent.ENEMY_ACTION_RESOLVED,
            BattlePhase.ENEMY_RESOLVED),
        transition(
            BattlePhase.ENEMY_OTHER, BattleEvent.ENEMY_ACTION_RESOLVED, BattlePhase.ENEMY_RESOLVED),
        transition(BattlePhase.ENEMY_RESOLVED, BattleEvent.PLAYER_DEFEATED, BattlePhase.DEFEAT),
        transition(BattlePhase.ENEMY_RESOLVED, BattleEvent.ENEMIES_DEFEATED, BattlePhase.VICTORY),
        transition(BattlePhase.ENEMY_RESOLVED, BattleEvent.MORE_ENEMIES, BattlePhase.ENEMY_TURN),
        transition(
            BattlePhase.ENEMY_RESOLVED,
            BattleEvent.ENEMY_PHASE_COMPLETE,
            BattlePhase.REVEAL_INTENTS));
  }

  private static Transition transition(
      BattlePhase currentPhase, BattleEvent event, BattlePhase resultingPhase) {
    return new Transition(currentPhase, event, resultingPhase);
  }

  private record Transition(
      BattlePhase currentPhase, BattleEvent event, BattlePhase resultingPhase) {}
}
