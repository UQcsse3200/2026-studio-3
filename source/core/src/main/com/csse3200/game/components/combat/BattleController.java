package com.csse3200.game.components.combat;

import com.csse3200.game.cards.effects.*;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayResult;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.events.listeners.EventListener1;
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
  private final Entity player;

  private final List<Entity> enemies;
  private int currentEnemyIndex;
  private EnemyIntent currentEnemyIntent;

  private BattlePhase currentPhase;
  private final BattleTransitions battleTransitions;
  private final EventHandler eventHandler;
  private final Deque<BattleEvent> eventQueue;
  private boolean processingEvents;

  private final CardEffectHandler effectHandler;
  private final CardPlayService cardPlayService;
  private CardPlayRequest pendingCard;
  private boolean lastCardPlaySucceeded;

  /** Logging Strings & Error messages */
  private static final String PHASE_CHANGED_EVENT = "battlePhaseChanged";

  private static final String BATTLE_LOG_EVENT = "battleLog";
  private static final String BATTLE_ENDED_EVENT = "battleEnded";
  private static final String ENEMY_EFFECTS_EVENT = "enemyEffects";
  private static final String PLAYER_EFFECTS_EVENT = "playerEffects";
  private static final String HAND_CHANGED_EVENT = "handChanged";
  private static final String LISTENER_NOT_NULL = "Listener must not be null.";

  public BattleController(Entity player, List<Entity> enemies) throws IllegalArgumentException {
    this(player, enemies, null, null);
    this.currentPhase = BattlePhase.SETUP;
  }

  /**
   * @param player the player entity
   * @param enemies the enemies in the encounter
   * @param cardPlayService coordinator of the card play
   */
  public BattleController(
      Entity player,
      List<Entity> enemies,
      CardEffectHandler effectHandler,
      CardPlayService cardPlayService)
      throws IllegalArgumentException {

    this.player = player;
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null.");
    }

    this.effectHandler = effectHandler;
    this.cardPlayService = cardPlayService;

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
   * Queues an event and processes queued events in order.
   *
   * <p>Events submitted during processing are queued until the current event finishes. If
   * processing throws, remaining events are discarded; changes already made are not rolled back.
   *
   * @param event the battle event to handle
   */
  public void handle(BattleEvent event) {
    Objects.requireNonNull(event, "event cannot be null");
    this.eventQueue.addLast(event);

    // Guards against recursion impacting order of events.
    if (this.processingEvents) {
      return;
    }
    // Prevents nested calls from processing the queue recursively
    this.processingEvents = true;
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
      this.processingEvents = false;
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

  /**
   * Starts the battle encounter
   *
   * @throws IllegalStateException if the battle has already begun
   */
  public void start() throws IllegalStateException {
    if (this.getCurrentPhase() != BattlePhase.SETUP) {
      throw new IllegalStateException("The battle has already begun!");
    }
    handle(BattleEvent.SETUP_COMPLETE);
  }

  /**
   * Reports whether the player currently has the given status effect.
   *
   * <p>Read-only: exposes a query about the player rather than the player entity itself, so callers
   * cannot mutate player state through this controller.
   *
   * @param effectType identifier of the status effect, as stored by CombatStatsComponent
   * @return true if the player carries an active effect with this identifier
   */
  public boolean playerHasStatusEffect(String effectType) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    return stats != null && stats.hasStatusEffect(effectType);
  }

  /**
   * Player intends to attack the enemy on their turn
   *
   * @deprecated Cards are submitted through submitPlayCardRequest
   */
  @Deprecated
  public void selectAttack() {}

  /**
   * Player intends to defend themselves on their turn
   *
   * @deprecated Cards are submitted through submitPlayCardRequest
   */
  @Deprecated
  public void selectDefend() {}

  /**
   * Player intends to do another action on the turn.
   *
   * @deprecated Cards are submitted through submitPlayCardRequest
   */
  @Deprecated
  public void selectOther() {}

  /** Player decides to end their turn */
  public void endPlayerTurn() {
    if (canHandle(BattleEvent.PLAYER_END_REQUESTED)) {
      handle(BattleEvent.PLAYER_END_REQUESTED);
    }
  }

  /**
   * Resets the current battle to a completely new battle that has no previous player and enemy
   * turns.
   */
  public void resetBattle() {
    if (this.processingEvents) {
      throw new IllegalStateException("There is an event in progress.");
    }

    // Saving the previous phase to inform the event listeners
    BattlePhase previousPhase = this.currentPhase;

    // Normal housekeeping for resetting the state machine.
    this.eventQueue.clear();
    this.resetEnemyCursor();
    this.setEnemyIntent(null);
    this.setCurrentPhase(BattlePhase.SETUP);
    this.notifyPhaseChange(previousPhase, BattlePhase.SETUP);
  }

  /**
   * Retrieves the current phase of the battle (e.g. player turn, enemy turn, player attack, etc.)
   *
   * @return the current phase of the BattleController instance
   */
  public BattlePhase getCurrentPhase() {
    return this.currentPhase;
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
   * Adds a listener to the event handler, which ultimately informs external teams about a phase
   * change.
   *
   * @param listener The instantiated external listener.
   */
  public void addPhaseChangeListener(EventListener2<BattlePhase, BattlePhase> listener) {
    Objects.requireNonNull(listener, LISTENER_NOT_NULL);
    eventHandler.addListener(PHASE_CHANGED_EVENT, listener);
  }

  /**
   * Adds a listener for short, human-readable descriptions of what just happened in the battle.
   * Used by the UI to pop up "you did X" / "the enemy did Y" messages between turns.
   *
   * @param listener receives the message text
   */
  public void addBattleLogListener(EventListener1<String> listener) {
    Objects.requireNonNull(listener, LISTENER_NOT_NULL);
    eventHandler.addListener(BATTLE_LOG_EVENT, listener);
  }

  /**
   * Adds a listener for the end of the battle. The argument is {@code true} on a win (all enemies
   * defeated) and {@code false} on a loss (player defeated). This is the hook the UI uses to switch
   * to the victory or defeat screen.
   *
   * @param listener receives the win/loss flag
   */
  public void addBattleEndListener(EventListener1<Boolean> listener) {
    Objects.requireNonNull(listener, LISTENER_NOT_NULL);
    eventHandler.addListener(BATTLE_ENDED_EVENT, listener);
  }

  /**
   * Adds a listener that receives the resolved enemy-facing effects of a played card, for Team 1 to
   * apply. The controller also applies these itself so the encounter still resolves.
   *
   * @param listener receives the resolved effects
   */
  public void addEnemyEffectsListener(EventListener1<List<ResolvedCardEffect>> listener) {
    Objects.requireNonNull(listener, LISTENER_NOT_NULL);
    eventHandler.addListener(ENEMY_EFFECTS_EVENT, listener);
  }

  /**
   * Adds a listener that receives the resolved player-facing effects of a played card, for Team 7
   * to apply. The controller also applies these itself so the encounter still resolves.
   *
   * @param listener receives the resolved effects
   */
  public void addPlayerEffectsListener(EventListener1<List<ResolvedCardEffect>> listener) {
    Objects.requireNonNull(listener, LISTENER_NOT_NULL);
    eventHandler.addListener(PLAYER_EFFECTS_EVENT, listener);
  }

  /**
   * Adds a listener that receives the player's hand (card IDs) after it changes, e.g. once a played
   * card has moved from hand to discard. The UI uses this to refresh the on-screen hand.
   *
   * @param listener receives the updated hand
   */
  public void addHandChangedListener(EventListener1<List<String>> listener) {
    Objects.requireNonNull(listener, LISTENER_NOT_NULL);
    eventHandler.addListener(HAND_CHANGED_EVENT, listener);
  }

  /** Sends a one-line description of the latest battle action to any log listeners. */
  private void narrate(String message) {
    eventHandler.trigger(BATTLE_LOG_EVENT, message);
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

  /*----------------------------- Setters --------------------------------*/

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
  private boolean advanceToNextLivingEnemy() {
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
   * Retrieves the current active enemy instance from the list of available enemies
   *
   * @return the current active enemy instance in the battle
   */
  private Entity getActiveEnemy() {
    if (this.currentEnemyIndex < 0 || this.currentEnemyIndex >= enemies.size()) {
      throw new IllegalStateException("No active enemy.");
    }
    return this.enemies.get(this.currentEnemyIndex);
  }

  /**
   * Checks whether the combat has ended, and queues the corresponding outcome event. If both sides
   * are defeated, the player defeat takes precedence.
   *
   * @return True if the battle is over, False if it isn't.
   */
  private boolean queueBattleOutcomeIfOver() {
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

  /** Resets the enemy cursor to the default 'no target' value */
  private void resetEnemyCursor() {
    this.setCurrentEnemyIndex(-1);
  }

  public CardPlayRequest getCardPlayRequest() {
    return this.pendingCard;
  }

  public Boolean submitCardPlayRequest(CardPlayRequest cardPlayRequest, PlayerIntent playerIntent) {
    Objects.requireNonNull(cardPlayRequest, "cardPlayRequest cannot be null.");
    Objects.requireNonNull(playerIntent, "playerIntent cannot be null.");

    BattleEvent event =
        switch (playerIntent) {
          case ATTACK -> BattleEvent.PLAYER_ATTACK_SELECTED;
          case DEFEND -> BattleEvent.PLAYER_DEFEND_SELECTED;
          case OTHER -> BattleEvent.PLAYER_OTHER_SELECTED;
          case END_PLAYER_TURN ->
              throw new IllegalArgumentException("End turn is not a card action");
        };

    if (processingEvents || !canHandle(event)) {
      return false;
    }
    lastCardPlaySucceeded = false;
    pendingCard = cardPlayRequest;
    handle(event);
    return lastCardPlaySucceeded;
  }

  /**
   * Prints a summary string for a game event that occurs. For use in printing actions within a
   * battle sequence.
   *
   * @param request The card that is being played.
   * @param result The result of the card being played.
   * @return A string summarising the card being played and the resulting actions.
   */
  private String summarise(CardPlayRequest request, CardPlayResult result) {
    StringBuilder summary = new StringBuilder("You played ").append(request.cardId());
    for (ResolvedCardEffect effect : result.enemyEffects()) {
      summary
          .append(" - ")
          .append(effect.type())
          .append(' ')
          .append(effect.value())
          .append(" to enemy");
    }
    for (ResolvedCardEffect effect : result.playerEffects()) {
      summary
          .append(" - ")
          .append(effect.type())
          .append(' ')
          .append(effect.value())
          .append(" to you");
    }
    return summary.append('.').toString();
  }

  /**
   * Reads the enemy's telegraphed intent, falling back to a basic attack when the enemy behaviour
   * has not decided one yet. This keeps the loop moving until Team 1's intent patterns (#20) land.
   *
   * @param enemy the enemy to read an intent from
   * @return a non-null intent
   */
  private EnemyIntent resolveEnemyIntent(Entity enemy) {
    EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
    EnemyIntent intent = behaviour != null ? behaviour.getCurrentIntent() : null;
    if (intent != null && intent.getType() != IntentType.UNKNOWN) {
      return intent;
    }
    int attack = 0;
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    if (stats != null) {
      attack = stats.getBaseAttack();
    }
    return EnemyIntent.attack(attack);
  }

  /**
   * Returns if the enemy is alive.
   *
   * @param enemy The enemy to be checked.
   * @return True if the enemy is alive, False if not.
   */
  public boolean isEnemyAlive(Entity enemy) {
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    return !stats.isDead();
  }

  private EnergyComponent playerEnergy() {
    return this.player.getComponent(EnergyComponent.class);
  }

  private void finishPlayerCardAction() {
    pendingCard = null;
    handle(BattleEvent.PLAYER_ACTION_RESOLVED);
  }

  /**
   * Resolves the card the player submitted: asks the card system for a single {@link
   * CardPlayResult}, hands the resolved effects to the other systems, applies them so the encounter
   * still progresses, narrates what happened, then returns control to the player.
   */
  private void resolvePlayerCard() {
    CardPlayRequest request = this.pendingCard;
    if (request == null) {
      // FSM driven directly with no card attached (e.g. unit tests). Nothing to resolve.
      finishPlayerCardAction();
      return;
    }

    // Some controller unit tests intentionally run without the card system.
    if (cardPlayService == null) {
      lastCardPlaySucceeded = true;
      finishPlayerCardAction();
      return;
    }

    CardPlayResult result = cardPlayService.playCard(request);

    if (result == null) {
      // Card system not wired in (e.g. unit tests without a resolution service).
      lastCardPlaySucceeded = true;
      narrate("You played " + request.cardId() + ".");
      finishPlayerCardAction();
      return;
    }

    if (!result.success()) {
      // No effects produced; the card stays in hand and the player keeps their turn.
      lastCardPlaySucceeded = false;
      narrate("Couldn't play " + request.cardId() + ": " + result.failureReason());
      finishPlayerCardAction();
      return;
    }

    lastCardPlaySucceeded = true;
    dispatchCardEffects(request, result);
    narrate(summarise(request, result));
    finishPlayerCardAction();
  }

  /**
   * Passes the resolved effects to the other systems ({@code enemyEffects} to Team 1, {@code
   * playerEffects} to Team 7) and also applies them directly so the encounter resolves even before
   * those systems subscribe.
   *
   * @param request the card the player intends to play
   * @param result the result of the card play attempt
   */
  private void dispatchCardEffects(CardPlayRequest request, CardPlayResult result) {
    List<ResolvedCardEffect> enemyEffects = result.enemyEffects();
    List<ResolvedCardEffect> playerEffects = result.playerEffects();

    eventHandler.trigger(ENEMY_EFFECTS_EVENT, enemyEffects);
    eventHandler.trigger(PLAYER_EFFECTS_EVENT, playerEffects);

    effectHandler.applyEnemyEffects(
        effectHandler.getLivingEnemyTargets(request, this.enemies), enemyEffects);
    effectHandler.applyPlayerEffects(playerEffects, this.player);

    // The played card has left the hand — tell the UI to refresh.
    eventHandler.trigger(HAND_CHANGED_EVENT, result.updatedHand());
  }

  /*--------------------------- Possible Action Branches ----------------------------*/

  private void enterSetup() {
    // Coordinate battle setup.
    this.setCurrentEnemyIndex(0);
    handle(BattleEvent.SETUP_COMPLETE);
  }

  private void enterRevealIntents() {
    // reset enemy index to reduce the chance of buggy behaviour with dead enemies
    this.resetEnemyCursor();

    // Rolls intent for alive each enemy.
    for (Entity enemy : this.enemies) {
      if (this.isEnemyAlive(enemy)) {
        EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
        // Enemies live on their own entity and cannot reach the player, so hand the player's stats
        // over each round. Refreshing here keeps the AI reading the player's current condition.
        behaviour.setPlayerStats(player.getComponent(CombatStatsComponent.class));
        behaviour.rollIntent();
      }
    }

    // If an enemy is alive set it to the current intent
    if (this.advanceToNextLivingEnemy()) {
      this.setEnemyIntent(resolveEnemyIntent(this.getActiveEnemy()));
    } else {
      // If no enemies are alive - remove stale intent
      this.setEnemyIntent(null);
    }
    handle(BattleEvent.INTENTS_REVEALED);
  }

  /** Enters the 'player start' state of the FSM */
  private void enterPlayerStart() {
    if (this.queueBattleOutcomeIfOver()) {
      return;
    }
    // Start-of-turn operations: refill energy for the new player turn.
    EnergyComponent energy = playerEnergy();
    if (energy != null) {
      energy.onTurnStart();
    }
    handle(BattleEvent.PLAYER_TURN_STARTED);
  }

  private void enterPlayerTurn() {
    // Enable or accept player actions.
    this.queueBattleOutcomeIfOver();
    // wait for ui to submit card or end turn
  }

  private void enterPlayerAttack() {
    resolvePlayerCard();
  }

  private void enterPlayerDefend() {
    resolvePlayerCard();
  }

  private void enterPlayerOther() {
    resolvePlayerCard();
  }

  private void enterPlayerEnd() {
    // Coordinate end-of-turn operations.
    if (this.queueBattleOutcomeIfOver()) {
      return;
    }
    handle(BattleEvent.PLAYER_TURN_ENDED);
  }

  private void enterPlayerResolved() {
    // Check battle outcome before allowing another action.
    if (this.queueBattleOutcomeIfOver()) {
      return;
    }
    handle(BattleEvent.PLAYER_CONTINUES);
  }

  private void enterEnemyTurn() {
    // Begin the current enemy's action.
    BattleEvent event =
        switch (currentEnemyIntent.getType()) {
          case ATTACK -> BattleEvent.ENEMY_ATTACK_SELECTED;
          case DEFEND -> BattleEvent.ENEMY_DEFEND_SELECTED;
          default -> BattleEvent.ENEMY_OTHER_SELECTED;
        };
    handle(event);
  }

  private void enterEnemyAttack() {
    Entity enemy = getActiveEnemy();

    // Team 1's executeIntent now applies the hit to the player's CombatStatsComponent itself, so
    // the controller no longer re-applies the damage. Read the player's HP either side of the call
    // to narrate how hard the hit landed.
    CombatStatsComponent playerStats = this.player.getComponent(CombatStatsComponent.class);
    int healthBefore = playerStats != null ? playerStats.getHealth() : 0;

    EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
    if (behaviour != null) {
      behaviour.executeIntent(this.player);
    }

    int damage = playerStats != null ? Math.max(0, healthBefore - playerStats.getHealth()) : 0;
    narrate(
        "Enemy attacks for "
            + damage
            + (playerStats != null ? " (you have " + playerStats.getHealth() + " HP)" : "")
            + ".");
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyDefend() {
    Entity enemy = getActiveEnemy();

    EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
    if (behaviour != null) {
      behaviour.executeIntent(this.player);
    }
    narrate("Enemy braces for the next hit.");
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyOther() {
    Entity enemy = getActiveEnemy();

    EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
    if (behaviour != null) {
      behaviour.executeIntent(this.player);
    }
    narrate("Enemy makes its move.");
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyResolved() {
    // If the battle is over, abort and head straight to ending
    if (this.queueBattleOutcomeIfOver()) {
      return;
    }

    // If another enemy is successfully targeted.
    if (this.advanceToNextLivingEnemy()) {
      this.setEnemyIntent(resolveEnemyIntent(this.getActiveEnemy()));
      handle(BattleEvent.MORE_ENEMIES);
      return;
    }

    this.resetEnemyCursor();
    handle(BattleEvent.ENEMY_PHASE_COMPLETE);
  }

  private void enterVictory() {
    this.resetEnemyCursor();
    narrate("Victory! Every enemy has been defeated.");
    eventHandler.trigger(BATTLE_ENDED_EVENT, Boolean.TRUE);
  }

  private void enterDefeat() {
    this.resetEnemyCursor();
    narrate("Defeat. The player has fallen.");
    eventHandler.trigger(BATTLE_ENDED_EVENT, Boolean.FALSE);
  }
}
