package com.csse3200.game.components.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BattleControllerTest {
  private BattleController controller;
  private Entity player;
  private List<Entity> enemies;
  private EnemyBehaviourComponent firstEnemyBehaviour;
  private EnemyBehaviourComponent secondEnemyBehaviour;
  private final List<BattlePhase> phaseHistory = new ArrayList<>();

  @BeforeEach
  void setUp() {
    player = new Entity().addComponent(new CombatStatsComponent(20, 0));
    player.addComponent(new PlayerActions());
    firstEnemyBehaviour = mock(EnemyBehaviourComponent.class);
    secondEnemyBehaviour = mock(EnemyBehaviourComponent.class);
    enemies =
        List.of(
            createLivingDefendingEnemy(firstEnemyBehaviour),
            createLivingDefendingEnemy(secondEnemyBehaviour));
    controller = new BattleController(player, enemies);
    phaseHistory.clear();
    controller.addPhaseChangeListener((previous, next) -> phaseHistory.add(next));
  }

  @Test
  void shouldRejectNullPlayer() {
    assertThrows(IllegalArgumentException.class, () -> new BattleController(null, enemies));
  }

  @Test
  void shouldRejectEmptyEnemyList() {
    assertThrows(IllegalArgumentException.class, () -> new BattleController(player, List.of()));
  }

  @Test
  void shouldRejectNullEnemyList() {
    assertThrows(IllegalArgumentException.class, () -> new BattleController(player, null));
  }

  @Test
  void shouldRejectNullEnemy() {
    List<Entity> enemiesWithNull = Collections.singletonList(null);

    assertThrows(
        IllegalArgumentException.class, () -> new BattleController(player, enemiesWithNull));
  }

  @Test
  void shouldStartInSetupWithNoCurrentEnemy() {
    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
    assertEquals(-1, controller.getCurrentEnemyIndex());
  }

  @Test
  void shouldWaitForPlayerInputWhenPlayerTurnStarts() {
    controller.start();

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldResolveSubmittedAttackCardSynchronously() {
    advanceToPlayerTurn();
    CardPlayRequest request = new CardPlayRequest("strike", "enemy-1");

    boolean accepted = controller.submitCardPlayRequest(request, PlayerIntent.ATTACK);

    assertTrue(accepted);
    assertTrue(phaseHistory.contains(BattlePhase.PLAYER_ATTACK));
    // No resolution service is wired in this unit test, so the card is consumed and the player
    // keeps their turn.
    assertNull(controller.getCardPlayRequest());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldApplyValidTransitions() {
    controller.handle(BattleEvent.SETUP_COMPLETE);
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    controller.handle(BattleEvent.PLAYER_ATTACK_SELECTED);

    // A card action with no card attached resolves immediately and returns to the player.
    assertTrue(phaseHistory.contains(BattlePhase.PLAYER_ATTACK));
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldReportHandledEvents() {
    assertTrue(controller.canHandle(BattleEvent.SETUP_COMPLETE));
    assertFalse(controller.canHandle(BattleEvent.PLAYER_ATTACK_SELECTED));

    controller.handle(BattleEvent.SETUP_COMPLETE);

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertTrue(controller.canHandle(BattleEvent.PLAYER_ATTACK_SELECTED));
    assertTrue(controller.canHandle(BattleEvent.PLAYER_END_REQUESTED));
    assertFalse(controller.canHandle(BattleEvent.INTENTS_REVEALED));
    assertFalse(controller.canHandle(BattleEvent.SETUP_COMPLETE));
  }

  @Test
  void shouldCompletePlayerActionCycleSynchronously() {
    advanceToPlayerTurn();

    controller.handle(BattleEvent.PLAYER_ATTACK_SELECTED);

    // The action steps through PLAYER_ATTACK and PLAYER_RESOLVED on its own, then hands control
    // back to the player for their next card.
    assertTrue(phaseHistory.contains(BattlePhase.PLAYER_ATTACK));
    assertTrue(phaseHistory.contains(BattlePhase.PLAYER_RESOLVED));
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldProcessMultipleEnemies() {
    advanceToEnemyTurn();

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(0, controller.getCurrentEnemyIndex());
    verify(firstEnemyBehaviour, times(2)).rollIntent();
    verify(firstEnemyBehaviour).executeIntent(player);
    verify(secondEnemyBehaviour, times(2)).rollIntent();
    verify(secondEnemyBehaviour).executeIntent(player);
  }

  @Test
  void shouldCompleteTwoBattleRounds() {
    controller.start();

    completePlayerTurn();
    completePlayerTurn();

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    verify(firstEnemyBehaviour, times(2)).executeIntent(player);
    verify(secondEnemyBehaviour, times(2)).executeIntent(player);
  }

  @Test
  void shouldSkipDeadEnemies() {
    EnemyBehaviourComponent deadEnemyBehaviour = mock(EnemyBehaviourComponent.class);
    controller =
        new BattleController(
            player,
            List.of(
                createDefendingEnemy(deadEnemyBehaviour, false),
                createLivingDefendingEnemy(firstEnemyBehaviour)));

    controller.start();
    completePlayerTurn();

    verify(deadEnemyBehaviour, never()).rollIntent();
    verify(deadEnemyBehaviour, never()).executeIntent(player);
    verify(firstEnemyBehaviour).executeIntent(player);
  }

  @Test
  void shouldRollLivingIntents() {
    controller.start();

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    verify(firstEnemyBehaviour).rollIntent();
    verify(secondEnemyBehaviour).rollIntent();
  }

  @Test
  void shouldEnterVictoryWhenAllEnemiesAreDead() {
    controller =
        new BattleController(
            player,
            List.of(
                createDefendingEnemy(firstEnemyBehaviour, false),
                createDefendingEnemy(secondEnemyBehaviour, false)));

    controller.start();

    assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
  }

  @Test
  void shouldEnterDefeatWhenPlayerIsDead() {
    player = new Entity().addComponent(new CombatStatsComponent(0, 0));
    controller = new BattleController(player, enemies);

    controller.start();

    assertEquals(BattlePhase.DEFEAT, controller.getCurrentPhase());
  }

  @Test
  void shouldDamagePlayer() {
    // The AI opens with an attack; executeIntent hits the player for the enemy's base attack (5).
    EnemyBehaviourComponent attackingBehaviour = new EnemyBehaviourComponent("test");
    Entity enemy =
        new Entity().addComponent(new CombatStatsComponent(10, 5)).addComponent(attackingBehaviour);
    controller = new BattleController(player, List.of(enemy));

    controller.start();
    completePlayerTurn();

    assertEquals(15, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldRejectStartingBattleTwice() {
    controller.start();

    assertThrows(IllegalStateException.class, controller::start);
  }

  @Test
  void shouldQueuePhaseChangeListenerEvents() {
    AtomicReference<BattlePhase> phaseWhenListenerRan = new AtomicReference<>();
    AtomicBoolean alreadyFired = new AtomicBoolean(false);
    controller.addPhaseChangeListener(
        (previousPhase, nextPhase) -> {
          if (nextPhase == BattlePhase.PLAYER_TURN && alreadyFired.compareAndSet(false, true)) {
            // This event must be queued, not handled re-entrantly while a transition is running.
            controller.handle(BattleEvent.PLAYER_ATTACK_SELECTED);
            phaseWhenListenerRan.set(controller.getCurrentPhase());
          }
        });

    controller.start();

    // The queued event had not been processed yet when the listener observed the phase.
    assertEquals(BattlePhase.PLAYER_TURN, phaseWhenListenerRan.get());
    // Once processed, the attack resolved synchronously and control returned to the player.
    assertTrue(phaseHistory.contains(BattlePhase.PLAYER_ATTACK));
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldRejectEventsAfterVictory() {
    controller =
        new BattleController(
            player,
            List.of(
                createDefendingEnemy(firstEnemyBehaviour, false),
                createDefendingEnemy(secondEnemyBehaviour, false)));

    controller.start();

    assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
    assertFalse(controller.canHandle(BattleEvent.PLAYER_TURN_STARTED));
    assertThrows(
        IllegalStateException.class, () -> controller.handle(BattleEvent.PLAYER_TURN_STARTED));
    assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
  }

  @Test
  void shouldRejectEventsAfterDefeat() {
    player = new Entity().addComponent(new CombatStatsComponent(0, 0));
    controller = new BattleController(player, enemies);

    controller.start();

    assertEquals(BattlePhase.DEFEAT, controller.getCurrentPhase());
    assertFalse(controller.canHandle(BattleEvent.PLAYER_TURN_STARTED));
    assertThrows(
        IllegalStateException.class, () -> controller.handle(BattleEvent.PLAYER_TURN_STARTED));
    assertEquals(BattlePhase.DEFEAT, controller.getCurrentPhase());
  }

  @Test
  void shouldRejectNullEvent() {
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> controller.handle(null));

    assertEquals("event cannot be null", exception.getMessage());
    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
  }

  @Test
  void shouldRejectInvalidTransition() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> controller.handle(BattleEvent.PLAYER_ATTACK_SELECTED));

    assertEquals(
        "Invalid battle transition: SETUP-->PLAYER_ATTACK_SELECTED", exception.getMessage());
    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
  }

  private void advanceToPlayerTurn() {
    controller.handle(BattleEvent.SETUP_COMPLETE);
  }

  private void advanceToEnemyTurn() {
    advanceToPlayerTurn();
    completePlayerTurn();
  }

  private void completePlayerTurn() {
    // PLAYER_END_REQUESTED runs the whole end-of-turn and enemy phase on its own.
    controller.handle(BattleEvent.PLAYER_END_REQUESTED);
  }

  private Entity createLivingDefendingEnemy(EnemyBehaviourComponent behaviour) {
    return createDefendingEnemy(behaviour, true);
  }

  private Entity createDefendingEnemy(EnemyBehaviourComponent behaviour, boolean alive) {
    Entity enemy = mock(Entity.class);
    CombatStatsComponent stats = mock(CombatStatsComponent.class);
    when(enemy.getComponent(EnemyBehaviourComponent.class)).thenReturn(behaviour);
    when(enemy.getComponent(CombatStatsComponent.class)).thenReturn(stats);
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
    when(stats.isDead()).thenReturn(!alive);
    return enemy;
  }
}
