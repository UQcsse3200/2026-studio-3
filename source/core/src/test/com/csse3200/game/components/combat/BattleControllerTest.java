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

import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.csse3200.game.components.StatusEffect;

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
    CardPlayRequest request =
        new CardPlayRequest("strike", new CardPlayTarget(TargetType.SINGLE_ENEMY, "enemy-1"));

    boolean accepted = controller.submitCardPlayRequest(request);

    assertTrue(accepted);
    assertTrue(phaseHistory.contains(BattlePhase.CARD_RESOLVING));
    // No resolution service is wired in this unit test, so the card is consumed and the player
    // keeps their turn.
    assertNull(controller.getCardPlayRequest());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldApplyValidTransitions() {
    controller.handle(BattleEvent.SETUP_COMPLETE);
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    controller.handle(BattleEvent.CARD_PLAY_REQUESTED);

    // A card action with no card attached resolves immediately and returns to the player.
    assertTrue(phaseHistory.contains(BattlePhase.CARD_RESOLVING));
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldReportHandledEvents() {
    assertTrue(controller.canHandle(BattleEvent.SETUP_COMPLETE));
    assertFalse(controller.canHandle(BattleEvent.CARD_PLAY_REQUESTED));

    controller.handle(BattleEvent.SETUP_COMPLETE);

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertTrue(controller.canHandle(BattleEvent.CARD_PLAY_REQUESTED));
    assertTrue(controller.canHandle(BattleEvent.PLAYER_END_REQUESTED));
    assertFalse(controller.canHandle(BattleEvent.INTENTS_REVEALED));
    assertFalse(controller.canHandle(BattleEvent.SETUP_COMPLETE));
  }

  @Test
  void shouldCompletePlayerActionCycleSynchronously() {
    advanceToPlayerTurn();
    phaseHistory.clear();

    controller.handle(BattleEvent.CARD_PLAY_REQUESTED);

    // One busy phase covers every card, then input becomes available again.
    assertEquals(List.of(BattlePhase.CARD_RESOLVING, BattlePhase.PLAYER_TURN), phaseHistory);
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
            controller.handle(BattleEvent.CARD_PLAY_REQUESTED);
            phaseWhenListenerRan.set(controller.getCurrentPhase());
          }
        });

    controller.start();

    // The queued event had not been processed yet when the listener observed the phase.
    assertEquals(BattlePhase.PLAYER_TURN, phaseWhenListenerRan.get());
    // Once processed, the attack resolved synchronously and control returned to the player.
    assertTrue(phaseHistory.contains(BattlePhase.CARD_RESOLVING));
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
            IllegalStateException.class, () -> controller.handle(BattleEvent.CARD_PLAY_REQUESTED));

    assertEquals("Invalid battle transition: SETUP-->CARD_PLAY_REQUESTED", exception.getMessage());
    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
  }

  @Test
  void shouldRejectCardSubmissionFromPhaseListener() {
    AtomicBoolean attempted = new AtomicBoolean(false);

    controller.addPhaseChangeListener(
        (previous, next) -> {
          if (next == BattlePhase.PLAYER_TURN && attempted.compareAndSet(false, true)) {
            assertFalse(
                controller.submitCardPlayRequest(CardPlayRequest.singleEnemy("strike", "enemy-1")));
            assertFalse(controller.submitCardPlayRequest(CardPlayRequest.self("defend")));
            assertNull(controller.getCardPlayRequest());
          }
        });

    controller.start();

    assertTrue(attempted.get());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertFalse(phaseHistory.contains(BattlePhase.CARD_RESOLVING));

    // Normal submissions still work once event processing finishes.
    assertTrue(controller.submitCardPlayRequest(CardPlayRequest.singleEnemy("strike", "enemy-1")));
  }

  @Test
  void shouldSkipEnemyKilledDuringTurn() {
      controller.start();
      killEnemy(0);

      controller.endPlayerTurn();

      verify(firstEnemyBehaviour, never()).executeIntent(player);
      verify(secondEnemyBehaviour).executeIntent(player);
      assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

  }

  @Test
  void shouldWinWhenEnemiesDeadBeforeActing() {
     controller.addPhaseChangeListener(
             (previous, next) -> {
                 if (next == BattlePhase.ENEMY_TURN) {
                     killEnemy(controller.getCurrentEnemyIndex());
                 }
             }
     );

     controller.start();
     controller.endPlayerTurn();

     verify(firstEnemyBehaviour, never()).executeIntent(player);
     verify(secondEnemyBehaviour, never()).executeIntent(player);
     assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
  }

  @Test
  void shouldContinueIfEnemySurvives() {
      controller.addPhaseChangeListener(
              (previous, next) -> {
                  if (next == BattlePhase.ENEMY_TURN &&
                          controller.getCurrentEnemyIndex() == 1) {
                      killEnemy(1);
                  }
              }
      );

      controller.start();
      controller.endPlayerTurn();

      verify(firstEnemyBehaviour).executeIntent(player);
      verify(secondEnemyBehaviour, never()).executeIntent(player);
      assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldPreserveSuccessfulResultWhenListenerSubmitsAnotherCard() {
    controller.start();
    AtomicBoolean attempted = new AtomicBoolean(false);

    controller.addPhaseChangeListener(
        (previous, next) -> {
          if (next == BattlePhase.PLAYER_TURN && attempted.compareAndSet(false, true)) {
            assertFalse(controller.submitCardPlayRequest(CardPlayRequest.self("defend")));
          }
        });

    assertTrue(controller.submitCardPlayRequest(CardPlayRequest.singleEnemy("strike", "enemy-1")));

    assertTrue(attempted.get());
    assertNull(controller.getCardPlayRequest());
    assertEquals(1, phaseHistory.stream().filter(p -> p == BattlePhase.CARD_RESOLVING).count());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldKeepOriginalCardWhenAnotherIsSubmittedDuringResolution() {
    controller.start();
    CardPlayRequest original = CardPlayRequest.singleEnemy("strike", "enemy-1");
    AtomicBoolean attempted = new AtomicBoolean(false);
    controller.addPhaseChangeListener(
        (previous, next) -> {
          if (next == BattlePhase.CARD_RESOLVING) {
            attempted.set(true);
            assertFalse(controller.submitCardPlayRequest(CardPlayRequest.self("defend")));
            assertEquals(original, controller.getCardPlayRequest());
            controller.endPlayerTurn();
          }
        });

    assertTrue(controller.submitCardPlayRequest(original));

    assertTrue(attempted.get());
    assertNull(controller.getCardPlayRequest());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    verify(firstEnemyBehaviour, never()).executeIntent(player);
  }

  @Test
  void shouldClearPendingCardIfResolutionListenerThrows() {
    controller.start();
    controller.addPhaseChangeListener(
        (previous, next) -> {
          if (next == BattlePhase.CARD_RESOLVING) {
            throw new IllegalStateException("Listener failed");
          }
        });

    assertThrows(
        IllegalStateException.class,
        () -> controller.submitCardPlayRequest(CardPlayRequest.singleEnemy("strike", "enemy-1")));

    assertNull(controller.getCardPlayRequest());
    assertFalse(controller.submitCardPlayRequest(CardPlayRequest.self("defend")));
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

  private void killEnemy(int index) {
      CombatStatsComponent stats =
              enemies.get(index).getComponent(CombatStatsComponent.class);
      when(stats.isDead()).thenReturn(true);
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

  /** Verifies poison consumes defenses, ticks once per turn and expires. */
  @Test
  void poisonShouldUseDefensesAndExpireAfterTwoEnemyTurns() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 0);
    stats.setBlock(3);
    stats.setArmor(4);
    stats.applyStatusEffect(new StatusEffect("POISON", 5, 2));

    Entity enemy = createPoisonTestEnemy(stats, firstEnemyBehaviour);
    BattleController battle = new BattleController(player, List.of(enemy));

    List<Integer> healthAtAction = new ArrayList<>();
    org.mockito.Mockito.doAnswer(
                    invocation -> {
                      healthAtAction.add(stats.getHealth());
                      return null;
                    })
            .when(firstEnemyBehaviour)
            .executeIntent(player);

    battle.start();
    battle.endPlayerTurn();

    assertEquals(20, stats.getHealth());
    assertEquals(0, stats.getBlock());
    assertEquals(2, stats.getArmor());
    assertEquals(1, stats.getStatusEffect("POISON").getDuration());
    assertEquals(List.of(20), healthAtAction);
    assertEquals(BattlePhase.PLAYER_TURN, battle.getCurrentPhase());

    battle.endPlayerTurn();

    assertEquals(17, stats.getHealth());
    assertEquals(0, stats.getArmor());
    assertNull(stats.getStatusEffect("POISON"));
    assertEquals(List.of(20, 17), healthAtAction);

    battle.endPlayerTurn();

    assertEquals(17, stats.getHealth());
    assertEquals(List.of(20, 17, 17), healthAtAction);
    verify(firstEnemyBehaviour, times(3)).executeIntent(player);
  }

  /** Verifies a poisoned enemy dies before acting while the next enemy still acts. */
  @Test
  void poisonShouldSkipKilledEnemyAndContinueToNextEnemy() {
    CombatStatsComponent poisonedStats = new CombatStatsComponent(3, 0);
    poisonedStats.applyStatusEffect(new StatusEffect("POISON", 3, 1));

    CombatStatsComponent healthyStats = new CombatStatsComponent(20, 0);

    Entity poisonedEnemy = createPoisonTestEnemy(poisonedStats, firstEnemyBehaviour);
    Entity healthyEnemy = createPoisonTestEnemy(healthyStats, secondEnemyBehaviour);

    BattleController battle = new BattleController(player, List.of(poisonedEnemy, healthyEnemy));

    battle.start();
    battle.endPlayerTurn();

    assertEquals(0, poisonedStats.getHealth());
    assertNull(poisonedStats.getStatusEffect("POISON"));
    assertEquals(20, healthyStats.getHealth());
    verify(firstEnemyBehaviour, never()).executeIntent(player);
    verify(secondEnemyBehaviour).executeIntent(player);
    assertEquals(BattlePhase.PLAYER_TURN, battle.getCurrentPhase());
  }

  /** Verifies lethal poison on the final enemy ends the battle exactly once. */
  @Test
  void poisonShouldWinWhenLastEnemyDiesBeforeActing() {
    CombatStatsComponent stats = new CombatStatsComponent(3, 0);
    stats.applyStatusEffect(new StatusEffect("POISON", 3, 1));

    Entity enemy = createPoisonTestEnemy(stats, firstEnemyBehaviour);
    BattleController battle = new BattleController(player, List.of(enemy));

    List<Boolean> outcomes = new ArrayList<>();
    battle.addBattleEndListener(won -> outcomes.add(won));

    battle.start();
    battle.endPlayerTurn();

    assertEquals(0, stats.getHealth());
    verify(firstEnemyBehaviour, never()).executeIntent(player);
    assertEquals(BattlePhase.VICTORY, battle.getCurrentPhase());
    assertEquals(List.of(true), outcomes);
  }

  /**
   * Creates an enemy with real combat stats and a controlled defending intent.
   *
   * @param stats the combat stats used for damage and status processing
   * @param behaviour the mocked enemy behavior
   * @return the enemy entity
   */
  private Entity createPoisonTestEnemy(
          CombatStatsComponent stats, EnemyBehaviourComponent behaviour) {
    when(behaviour.getCurrentIntent()).thenReturn(EnemyIntent.defend(1));
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));

    return new Entity().addComponent(stats).addComponent(behaviour);
  }
}
