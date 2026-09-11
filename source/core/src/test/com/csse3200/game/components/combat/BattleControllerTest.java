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
            assertFalse(controller.submitCardPlayRequest(new CardPlayRequest("strike", "enemy-1")));
            assertFalse(controller.submitCardPlayRequest(new CardPlayRequest("defend", "player")));
            assertNull(controller.getCardPlayRequest());
          }
        });

    controller.start();

    assertTrue(attempted.get());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertFalse(phaseHistory.contains(BattlePhase.CARD_RESOLVING));

    // Normal submissions still work once event processing finishes.
    assertTrue(controller.submitCardPlayRequest(new CardPlayRequest("strike", "enemy-1")));
  }

  @Test
  void shouldPreserveSuccessfulResultWhenListenerSubmitsAnotherCard() {
    controller.start();
    AtomicBoolean attempted = new AtomicBoolean(false);

    controller.addPhaseChangeListener(
        (previous, next) -> {
          if (next == BattlePhase.PLAYER_TURN && attempted.compareAndSet(false, true)) {
            assertFalse(controller.submitCardPlayRequest(new CardPlayRequest("defend", "player")));
          }
        });

    assertTrue(controller.submitCardPlayRequest(new CardPlayRequest("strike", "enemy-1")));

    assertTrue(attempted.get());
    assertNull(controller.getCardPlayRequest());
    assertEquals(1, phaseHistory.stream().filter(p -> p == BattlePhase.CARD_RESOLVING).count());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldKeepOriginalCardWhenAnotherIsSubmittedDuringResolution() {
    controller.start();
    CardPlayRequest original = new CardPlayRequest("strike", "enemy-1");
    AtomicBoolean attempted = new AtomicBoolean(false);
    controller.addPhaseChangeListener(
        (previous, next) -> {
          if (next == BattlePhase.CARD_RESOLVING) {
            attempted.set(true);
            assertFalse(controller.submitCardPlayRequest(new CardPlayRequest("defend", "player")));
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
    controller.addBattleLogListener(
        message -> {
          throw new IllegalStateException("Listener failed");
        });

    assertThrows(
        IllegalStateException.class,
        () -> controller.submitCardPlayRequest(new CardPlayRequest("strike", "enemy-1")));

    assertNull(controller.getCardPlayRequest());
    assertFalse(controller.submitCardPlayRequest(new CardPlayRequest("defend", "player")));
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
