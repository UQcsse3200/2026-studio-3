package com.csse3200.game.components.combat;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.components.enemy.IntentType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.events.listeners.EventListener2;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * The central controller of the current state of the battle loop. Controls what phase the battle is
 * currently in, what actions are currently allowed and what the illegal and legal transitions are
 * This is functionally the Finite State Machine.
 */
public class BattleController {
  private BattlePhase currentPhase;
  private int currentEnemyIndex;
  private EnemyIntent currentEnemyIntent;
  private final BattleTransitions battleTransitions;
  private final EventHandler eventHandler;
  private final Deque<BattleEvent> eventQueue;
  private final Entity player;
  private final List<Entity> enemies;
  private static final String PHASE_CHANGED_EVENT = "battlePhaseChanged";
  private boolean pendingEvent;

  public BattleController(Entity player, List<Entity> enemies) throws IllegalArgumentException {

    this.player = player;
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null.");
    }

    // Guards against empty list or null enemies.
    this.enemies = enemies;
    if (this.enemies == null || this.enemies.isEmpty()) {
      throw new IllegalArgumentException("Enemies array cannot be empty.");
    } else if (!this.enemies.stream().allMatch(Objects::nonNull)) {
      throw new IllegalArgumentException("One or more enemies are null.");
    }

    this.battleTransitions = new BattleTransitions();
    this.currentPhase = BattlePhase.SETUP;
    this.currentEnemyIndex = -1;
    this.currentEnemyIntent = null;
    this.eventHandler = new EventHandler();
    this.eventQueue = new ArrayDeque<>();
  }

  /**
   * Handles a single event atomically.
   *
   * <p>NOTE: This function shouldn't be used outside of this class. This function is public
   * currently only for testing. If you need to start the state machine for testing, use start().
   *
   * @param event The event within the Battle Loop to handle.
   */
  public void handle(BattleEvent event) { // TODO: This is public for the sake of tests
    Objects.requireNonNull(event, "event cannot be null");
    this.eventQueue.addLast(event);

    // Guards against recursion impacting order of events.
    if (this.pendingEvent) {
      return;
    }
    // pendingEvent keeps events atomic.
    this.pendingEvent = true;

    // Takes an event from the queue, attempts to process atomically
    try {
      while (!this.eventQueue.isEmpty()) {
        BattleEvent currentEvent = this.eventQueue.removeFirst();
        this.processEvent(currentEvent);
      }
    } catch (RuntimeException e) {
      // If something goes wrong, makes sure that invalid events aren't kept in queue
      this.eventQueue.clear();
      throw e;
    } finally {
      this.pendingEvent = false;
    }
  }

  /**
   * Transitions from the current phase to the next phase.
   *
   * @param nextPhase The phase to transition to.
   */
  private void transition(BattlePhase nextPhase) {
    BattlePhase previousPhase = currentPhase;
    this.setCurrentPhase(nextPhase);
    this.notifyPhaseChange(previousPhase, this.getCurrentPhase());
    this.phaseChange(nextPhase);
  }

  /**
   * Handles an individual event that occurs within a battle loop.
   *
   * @param event The event to be handled.
   * @throws IllegalStateException When the given transition isn't allowed.
   */
  private void processEvent(BattleEvent event) {
    BattlePhase nextPhase = battleTransitions.getNextPhase(this.getCurrentPhase(), event);
    this.validateEventTransition(event, nextPhase);
    this.transition(nextPhase);
  }

  /**
   * The dispatch function for the action branches.
   *
   * @param phase The phase to dispatch.
   */
  private void phaseChange(BattlePhase phase) {
    switch (phase) {
      // Setup States
      case SETUP -> enterSetup();
      case REVEAL_INTENTS -> enterRevealIntents();

      // Player States
      case PLAYER_START -> enterPlayerStart();
      case PLAYER_TURN -> enterPlayerTurn();
      case PLAYER_ATTACK -> enterPlayerAttack();
      case PLAYER_DEFEND -> enterPlayerDefend();
      case PLAYER_OTHER -> enterPlayerOther();
      case PLAYER_END -> enterPlayerEnd();
      case PLAYER_RESOLVED -> enterPlayerResolved();

      // Enemy States
      case ENEMY_TURN -> enterEnemyTurn();
      case ENEMY_ATTACK -> enterEnemyAttack();
      case ENEMY_DEFEND -> enterEnemyDefend();
      case ENEMY_OTHER -> enterEnemyOther();
      case ENEMY_RESOLVED -> enterEnemyResolved();

      // Terminal States
      case VICTORY -> enterVictory();
      case DEFEAT -> enterDefeat();
    }
  }

  /*--------------------------- Public Methods -----------------------------*/

  /** Starts the battle encounter. */
  public void start() throws IllegalStateException {
    if (this.getCurrentPhase() != BattlePhase.SETUP) {
      throw new IllegalStateException("The battle has already begun!");
    }
    handle(BattleEvent.SETUP_COMPLETE);
  }

  /**
   * Adds a listener to the event handler, which ultimately informs external teams about a phase
   * change.
   *
   * @param listener The instantiated external listener.
   */
  public void addPhaseChangeListener(EventListener2<BattlePhase, BattlePhase> listener) {
    Objects.requireNonNull(listener, "Listener must not be null.");
    eventHandler.addListener(PHASE_CHANGED_EVENT, listener);
  }

  /**
   * Returns the current targeted enemy.
   *
   * @return An int representing the targeted entity within the array.
   */
  public int getCurrentEnemyIndex() {
    return this.currentEnemyIndex;
  }

  /**
   * Convenience function for returning if a given event can be handled within a state.
   *
   * @param event The event to check.
   * @return True if the event is valid to be applied. False if not.
   */
  public boolean canHandle(BattleEvent event) {
    return this.battleTransitions.getNextPhase(this.currentPhase, event) != null;
  }

  /*------------------------- Getters & Setters ----------------------------*/

  public BattlePhase getCurrentPhase() {
    return this.currentPhase;
  }

  private void setCurrentPhase(BattlePhase nextPhase) {
    this.currentPhase = nextPhase;
  }

  private void setCurrentEnemyIndex(int currentEnemyIndex) {
    this.currentEnemyIndex = currentEnemyIndex;
  }

  private void setEnemyIntent(EnemyIntent intent) {
    this.currentEnemyIntent = intent;
  }

  /*------------------------- Helper functions ----------------------------*/

  /**
   * Targets the next available enemy within the enemy array.
   *
   * @return True if a new target has been chosen. False if all enemies are dead.
   */
  private boolean targetNextEnemy() {
    // Starts from index after currently targeted enemy.
    for (int i = this.currentEnemyIndex + 1; i < this.enemies.size(); i++) {
      Entity currentEnemy = this.enemies.get(i);
      // Checks status of each enemy
      if (isEnemyAlive(currentEnemy)) {
        this.setCurrentEnemyIndex(i);
        return true;
      }
    }
    return false;
  }

  /**
   * A helper function that validates whether a transition is allowed.
   *
   * @param event The currently executing event.
   * @param nextPhase The speculative next phase to transition to.
   * @throws IllegalStateException Throws when the state transition is deemed illegal.
   */
  private void validateEventTransition(BattleEvent event, BattlePhase nextPhase)
      throws IllegalStateException {
    if (Objects.isNull(nextPhase)) {
      throw new IllegalStateException(
          "Invalid battle transition: " + this.currentPhase + "-->" + event);
    }
  }

  /**
   * Stub function for notifying UI or any listeners about a phase change.
   *
   * @param previousPhase The phase that is being left.
   * @param nextPhase The phase that is being entered.
   */
  private void notifyPhaseChange(BattlePhase previousPhase, BattlePhase nextPhase) {
    eventHandler.trigger(PHASE_CHANGED_EVENT, previousPhase, nextPhase);
  }

  /**
   * Gets the enemy at the current enemy index.
   *
   * @return The enemy at the current index.
   */
  private Entity getEnemy() {
    if (this.currentEnemyIndex < 0 || this.currentEnemyIndex >= enemies.size()) {
      throw new IllegalStateException("No active enemy.");
    }
    return this.enemies.get(this.currentEnemyIndex);
  }

  /**
   * Returns if the enemy is alive. NOTE: I couldn't find an existing helper/API for this, but in
   * the future this should probably be put in another module.
   *
   * @param enemy The enemy to be checked.
   * @return True if the enemy is alive, False if not.
   */
  private boolean isEnemyAlive(Entity enemy) {
    EnemyStatsComponent stats = enemy.getComponent(EnemyStatsComponent.class);
    return stats.isAlive();
  }

  /**
   * Checks the outcome of the battle.
   *
   * @return True if the battle is over, False if it isn't.
   */
  private boolean isBattleOver() {
    CombatStatsComponent playerStats = this.player.getComponent(CombatStatsComponent.class);
    boolean allEnemiesDead = this.enemies.stream().noneMatch(this::isEnemyAlive);

    if (playerStats.isDead()) {
      handle(BattleEvent.PLAYER_DEFEATED);
      return true;
    }

    if (allEnemiesDead) {
      handle(BattleEvent.ENEMIES_DEFEATED);
      return true;
    }
    return false;
  }

  /** Cleans up the variables after a round or the battle sequence is done. */
  private void cleanUp() {
    this.setCurrentEnemyIndex(-1);
  }

  /*------------------------- Possible Action Branches ----------------------------*/

  private void enterSetup() {
    // Coordinate battle setup.
    this.setCurrentEnemyIndex(0);
    handle(BattleEvent.SETUP_COMPLETE);
  }

  private void enterRevealIntents() {
    this.setCurrentEnemyIndex(-1); // TODO: Probably a better way to do this.

    // Rolls intent for alive each enemy.
    for (Entity enemy : this.enemies) {
      if (this.isEnemyAlive(enemy)) {
        enemy.getComponent(EnemyBehaviourComponent.class).rollIntent();
      }
    }

    // If an enemy is alive set it to the current intent
    if (this.targetNextEnemy()) {
      this.setEnemyIntent(
          this.getEnemy().getComponent(EnemyBehaviourComponent.class).getCurrentIntent());
    } else {
      // If no enemies are alive - remove stale intent
      this.setEnemyIntent(null);
    }
    handle(BattleEvent.INTENTS_REVEALED);
  }

  private void enterPlayerStart() {
    if (this.isBattleOver()) {
      return;
    }
    // Coordinate start-of-turn operations.
    handle(BattleEvent.PLAYER_TURN_STARTED);
  }

  private void enterPlayerTurn() {
    // Enable or accept player actions.
    this.isBattleOver();
  }

  private void enterPlayerAttack() {
    // Ask the relevant system to execute the submitted attack.
  }

  private void enterPlayerDefend() {
    // Ask the relevant system to execute the submitted defence.
  }

  private void enterPlayerOther() {
    // Ask the relevant system to execute the submitted action.
  }

  private void enterPlayerEnd() {
    // Coordinate end-of-turn operations.
    this.isBattleOver();
  }

  private void enterPlayerResolved() {
    // Check battle outcome before allowing another action.
    this.isBattleOver();
  }

  private void enterEnemyTurn() {
    // Begin the current enemy's action.
    if (currentEnemyIntent.getType() == IntentType.ATTACK) {
      handle(BattleEvent.ENEMY_ATTACK_SELECTED);
    } else if (currentEnemyIntent.getType() == IntentType.DEFEND) {
      handle(BattleEvent.ENEMY_DEFEND_SELECTED);
    } else {
      handle(BattleEvent.ENEMY_OTHER_SELECTED);
    }
  }

  private void enterEnemyAttack() {
    // Ask the enemy system to execute its attack intent.
    Entity enemy = getEnemy();

    // execute the intent
    enemy.getComponent(EnemyBehaviourComponent.class).executeIntent(this.player);
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyDefend() {
    // Ask the enemy system to execute its defend intent.
    Entity enemy = getEnemy();

    // execute the intent
    enemy.getComponent(EnemyBehaviourComponent.class).executeIntent(this.player);
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyOther() {
    // Ask the enemy system to execute its other intent.
    Entity enemy = getEnemy();

    // execute the intent
    enemy.getComponent(EnemyBehaviourComponent.class).executeIntent(this.player);
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyResolved() {
    // If the battle is over, abort and head straight to ending
    if (this.isBattleOver()) {
      return;
    }

    // If another enemy is successfully targeted.
    if (this.targetNextEnemy()) {
      this.setEnemyIntent(
          this.getEnemy().getComponent(EnemyBehaviourComponent.class).getCurrentIntent());
      handle(BattleEvent.MORE_ENEMIES);
      return;
    }

    this.cleanUp();
    handle(BattleEvent.ENEMY_PHASE_COMPLETE);
  }

  private void enterVictory() {
    // Notify other systems that the battle was won.
    this.cleanUp();
  }

  private void enterDefeat() {
    // Notify other systems that the battle was lost.
    this.cleanUp();
  }
}
