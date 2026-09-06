package com.csse3200.game.components.combat;

import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.CardEffectResolver;
import com.csse3200.game.cards.effects.CardPlayResult;
import com.csse3200.game.cards.effects.PlayerEffectState;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The central controller of the current state of the battle loop. Controls what phase the battle is
 * currently in, what actions are currently allowed and what the illegal and legal transitions are
 * This is functionally the Finite State Machine.
 */
public class BattleController {
  private BattlePhase currentPhase;
  private int currentEnemyIndex;
  private EnemyIntent currentEnemyIntent;
  private PlayerIntent currentPlayerIntent;
  private final BattleTransitions battleTransitions;
  private final EventHandler eventHandler;
  private final Deque<BattleEvent> eventQueue;
  private final Entity player;
  private final List<Entity> enemies;
  private static final String PHASE_CHANGED_EVENT = "battlePhaseChanged";
  private static final String BATTLE_LOG_EVENT = "battleLog";
  private static final String BATTLE_ENDED_EVENT = "battleEnded";
  private static final String ENEMY_EFFECTS_EVENT = "enemyEffects";
  private static final String PLAYER_EFFECTS_EVENT = "playerEffects";
  private static final String HAND_CHANGED_EVENT = "handChanged";
  private boolean pendingEvent;
  private CardPlayRequest pendingCard;

  /** Team 5's card-effect resolver (Team 6 configs -> resolved effects); null without cards. */
  private final CardEffectResolver effectResolver;

  /** Team 6 card library, used to look up a played card's config; null without cards. */
  private final CardService cardService;

  /** Team 5's per-battle player effect state, carrying Strength between plays. */
  private final PlayerEffectState playerEffectState;

  /** Team 5-owned deck state; null when the loop runs without cards. */
  private final BattleDeck battleDeck;

  public BattleController(Entity player, List<Entity> enemies) throws IllegalArgumentException {
    this(player, enemies, null, null, null);
  }

  /**
   * @param player the player entity
   * @param enemies the enemies in the encounter
   * @param effectResolver Team 5's card-effect resolver, or {@code null} to run without cards
   * @param cardService Team 6's card library for config lookup, or {@code null} to run without
   *     cards
   * @param battleDeck the battle deck state, or {@code null} to run without cards
   */
  public BattleController(
      Entity player,
      List<Entity> enemies,
      CardEffectResolver effectResolver,
      CardService cardService,
      BattleDeck battleDeck)
      throws IllegalArgumentException {

    this.effectResolver = effectResolver;
    this.cardService = cardService;
    this.playerEffectState = new PlayerEffectState();
    this.battleDeck = battleDeck;
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
    this.currentPlayerIntent = null;
    this.eventHandler = new EventHandler();
    this.eventQueue = new ArrayDeque<>();
  }

  /**
   * Handles a single event atomically.
   *
   * @param event The event within the Battle Loop to handle.
   */
  public void handle(BattleEvent event) {
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

  /** Player intends to attack the enemy on their turn */
  public void selectAttack() {
    this.currentPlayerIntent = PlayerIntent.ATTACK;
  }

  /** Player intends to defend themselves on their turn */
  public void selectDefend() {
    this.currentPlayerIntent = PlayerIntent.DEFEND;
  }

  /** Player intends to do other actions on their turn */
  public void selectOther() {
    this.currentPlayerIntent = PlayerIntent.OTHER;
  }

  /** Player decides to end their turn */
  public void endPlayerTurn() {
    this.currentPlayerIntent = PlayerIntent.END_PLAYER_TURN;
    if (canHandle(BattleEvent.PLAYER_END_REQUESTED)) {
      handle(BattleEvent.PLAYER_END_REQUESTED);
    }
  }

  /**
   * Resets the current battle to a completely new battle that has no previous player and enemy
   * turns
   */
  public void resetBattle() {
    if (this.pendingEvent) {
      throw new IllegalStateException("There is an event in progress.");
    }

    // Saving the previous phase to inform the event listeners
    BattlePhase previousPhase = this.currentPhase;

    // Normal housekeeping for resetting the state machine.
    this.eventQueue.clear();
    this.setCurrentEnemyIndex(-1);
    this.setEnemyIntent(null);
    this.setPlayerIntent(null);
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
   * Adds a listener for short, human-readable descriptions of what just happened in the battle.
   * Used by the UI to pop up "you did X" / "the enemy did Y" messages between turns.
   *
   * @param listener receives the message text
   */
  public void addBattleLogListener(EventListener1<String> listener) {
    Objects.requireNonNull(listener, "Listener must not be null.");
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
    Objects.requireNonNull(listener, "Listener must not be null.");
    eventHandler.addListener(BATTLE_ENDED_EVENT, listener);
  }

  /**
   * Adds a listener that receives the resolved enemy-facing effects of a played card, for Team 1 to
   * apply. The controller also applies these itself so the encounter still resolves.
   *
   * @param listener receives the resolved effects
   */
  public void addEnemyEffectsListener(EventListener1<List<ResolvedCardEffect>> listener) {
    Objects.requireNonNull(listener, "Listener must not be null.");
    eventHandler.addListener(ENEMY_EFFECTS_EVENT, listener);
  }

  /**
   * Adds a listener that receives the resolved player-facing effects of a played card, for Team 7
   * to apply. The controller also applies these itself so the encounter still resolves.
   *
   * @param listener receives the resolved effects
   */
  public void addPlayerEffectsListener(EventListener1<List<ResolvedCardEffect>> listener) {
    Objects.requireNonNull(listener, "Listener must not be null.");
    eventHandler.addListener(PLAYER_EFFECTS_EVENT, listener);
  }

  /**
   * Adds a listener that receives the player's hand (card IDs) after it changes, e.g. once a played
   * card has moved from hand to discard. The UI uses this to refresh the on-screen hand.
   *
   * @param listener receives the updated hand
   */
  public void addHandChangedListener(EventListener1<List<String>> listener) {
    Objects.requireNonNull(listener, "Listener must not be null.");
    eventHandler.addListener(HAND_CHANGED_EVENT, listener);
  }

  /** Sends a one-line description of the latest battle action to any log listeners. */
  private void narrate(String message) {
    eventHandler.trigger(BATTLE_LOG_EVENT, message);
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

  /*------------------------- Setters ----------------------------*/

  private void setCurrentPhase(BattlePhase nextPhase) {
    this.currentPhase = nextPhase;
  }

  private void setCurrentEnemyIndex(int currentEnemyIndex) {
    this.currentEnemyIndex = currentEnemyIndex;
  }

  private void setEnemyIntent(EnemyIntent intent) {
    this.currentEnemyIntent = intent;
  }

  private void setPlayerIntent(PlayerIntent intent) {
    this.currentPlayerIntent = intent;
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
   * Retrieves the current active enemy instance from the list of available enemies
   *
   * @return the current active enemy instance in the battle
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
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    return !stats.isDead();
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
    if (!canHandle(event)) {
      return false;
    }
    pendingCard = cardPlayRequest;
    currentPlayerIntent = playerIntent;
    handle(event);
    return true;
  }

  /*------------------------- Possible Action Branches ----------------------------*/

  private void enterSetup() {
    // Coordinate battle setup.
    this.setCurrentEnemyIndex(0);
    handle(BattleEvent.SETUP_COMPLETE);
  }

  private void enterRevealIntents() {
    // reset enemy index to reduce the chance of buggy behaviour with dead enemies
    this.setCurrentEnemyIndex(-1); // TODO: Probably a better way to do this.

    // Rolls intent for alive each enemy.
    for (Entity enemy : this.enemies) {
      if (this.isEnemyAlive(enemy)) {
        enemy.getComponent(EnemyBehaviourComponent.class).rollIntent();
      }
    }

    // If an enemy is alive set it to the current intent
    if (this.targetNextEnemy()) {
      this.setEnemyIntent(resolveEnemyIntent(this.getEnemy()));
    } else {
      // If no enemies are alive - remove stale intent
      this.setEnemyIntent(null);
    }
    handle(BattleEvent.INTENTS_REVEALED);
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

  private void enterPlayerStart() {
    if (this.isBattleOver()) {
      return;
    }
    // Start-of-turn operations: refill energy for the new player turn.
    EnergyComponent energy = playerEnergy();
    if (energy != null) {
      energy.onTurnStart();
    }
    handle(BattleEvent.PLAYER_TURN_STARTED);
  }

  private EnergyComponent playerEnergy() {
    return this.player.getComponent(EnergyComponent.class);
  }

  private void enterPlayerTurn() {
    // Enable or accept player actions.
    if (this.isBattleOver()) {
      return;
    }
    // wait for ui to submit card or end tyurn
  }

  private void finishPlayerCardAction() {
    pendingCard = null;
    currentPlayerIntent = null;
    handle(BattleEvent.PLAYER_ACTION_RESOLVED);
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

    CardPlayResult result = playCardThroughCardSystem(request);
    if (result == null) {
      // Card system not wired in (e.g. unit tests without a resolution service).
      narrate("You played " + request.cardID() + ".");
      finishPlayerCardAction();
      return;
    }

    if (!result.success()) {
      // No effects produced; the card stays in hand and the player keeps their turn.
      narrate("Couldn't play " + request.cardID() + ": " + result.failureReason());
      finishPlayerCardAction();
      return;
    }

    dispatchCardEffects(request, result);
    narrate(summarise(request, result));
    finishPlayerCardAction();
  }

  /**
   * Plays the submitted card: looks its config up in Team 6's library, checks it is in hand and
   * affordable, resolves its effects through Team 5's {@link CardEffectResolver}, then commits the
   * energy spend, moves the card to the discard pile and draws a replacement so the hand stays
   * topped up.
   *
   * @return the result, or {@code null} when no card system is wired in
   */
  private CardPlayResult playCardThroughCardSystem(CardPlayRequest request) {
    if (effectResolver == null || cardService == null || battleDeck == null) {
      return null;
    }

    Optional<CardConfig> maybeCard = cardService.getCard(request.cardID());
    if (maybeCard.isEmpty()) {
      return CardPlayResult.failure(
          "Unknown card: " + request.cardID(), request.cardID(), request.targetID(), battleDeck);
    }
    CardConfig card = maybeCard.get();

    if (!battleDeck.getHand().contains(card.id)) {
      return CardPlayResult.failure("Card not in hand", card.id, request.targetID(), battleDeck);
    }

    EnergyComponent energy = playerEnergy();
    if (energy != null && !energy.canAfford(card.cost)) {
      return CardPlayResult.failure("Not enough energy", card.id, request.targetID(), battleDeck);
    }

    CardEffectResolution resolution = effectResolver.resolve(card, playerEffectState);

    if (energy != null) {
      energy.spendEnergy(card.cost);
    }
    battleDeck.playCard(card.id);
    battleDeck.drawOne();

    return CardPlayResult.success(
        card.id,
        request.targetID(),
        resolution.enemyEffects(),
        resolution.playerEffects(),
        battleDeck,
        card.cost);
  }

  /**
   * Passes the resolved effects to the other systems ({@code enemyEffects} to Team 1, {@code
   * playerEffects} to Team 7) and also applies them directly so the encounter resolves even before
   * those systems subscribe.
   */
  private void dispatchCardEffects(CardPlayRequest request, CardPlayResult result) {
    List<ResolvedCardEffect> enemyEffects = result.enemyEffects();
    List<ResolvedCardEffect> playerEffects = result.playerEffects();

    eventHandler.trigger(ENEMY_EFFECTS_EVENT, enemyEffects);
    eventHandler.trigger(PLAYER_EFFECTS_EVENT, playerEffects);

    applyEnemyEffects(livingEnemyTargets(request), enemyEffects);
    applyPlayerEffects(playerEffects);

    // The played card has left the hand (see playCardThroughCardSystem) — tell the UI to refresh.
    eventHandler.trigger(HAND_CHANGED_EVENT, result.updatedHand());
  }

  /**
   * Chooses which enemies a card's enemy effects hit. Self-targeting cards hit nothing; everything
   * else hits every living enemy, which covers both the single-enemy encounter and ALL_ENEMIES
   * cards. Precise single-target selection can be layered on when encounters have several enemies.
   */
  private List<Entity> livingEnemyTargets(CardPlayRequest request) {
    if ("player".equalsIgnoreCase(request.targetID())) {
      return List.of();
    }
    List<Entity> targets = new ArrayList<>();
    for (Entity enemy : this.enemies) {
      if (isEnemyAlive(enemy)) {
        targets.add(enemy);
      }
    }
    return targets;
  }

  private void applyEnemyEffects(List<Entity> targets, List<ResolvedCardEffect> effects) {
    for (Entity enemy : targets) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats == null) {
        continue;
      }
      for (ResolvedCardEffect effect : effects) {
        switch (effect.type()) {
          case DAMAGE -> stats.takeDamage(effect.value());
          case POISON ->
              stats.applyStatusEffect(
                  new StatusEffect("poison", effect.value(), effect.duration()));
          case VULNERABLE ->
              stats.applyStatusEffect(
                  new StatusEffect("vulnerable", effect.value(), effect.duration()));
          default -> {
            // BLOCK / HEAL / STRENGTH are not enemy-facing.
          }
        }
      }
    }
  }

  private void applyPlayerEffects(List<ResolvedCardEffect> effects) {
    CombatStatsComponent stats = this.player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return;
    }
    for (ResolvedCardEffect effect : effects) {
      switch (effect.type()) {
        case BLOCK -> stats.addArmor(effect.value());
        case HEAL -> stats.heal(effect.value());
        default -> {
          // STRENGTH is already folded into the resolver's running player state.
        }
      }
    }
  }

  private String summarise(CardPlayRequest request, CardPlayResult result) {
    StringBuilder summary = new StringBuilder("You played ").append(request.cardID());
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

  public void enterPlayerEnd() {
    // Coordinate end-of-turn operations.
    if (this.isBattleOver()) {
      return;
    }
    handle(BattleEvent.PLAYER_TURN_ENDED);
  }

  private void enterPlayerResolved() {
    // Check battle outcome before allowing another action.
    if (this.isBattleOver()) {
      return;
    }
    handle(BattleEvent.PLAYER_CONTINUES);
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
    Entity enemy = getEnemy();

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
    Entity enemy = getEnemy();

    EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
    if (behaviour != null) {
      behaviour.executeIntent(this.player);
    }
    narrate("Enemy braces for the next hit.");
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyOther() {
    Entity enemy = getEnemy();

    EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);
    if (behaviour != null) {
      behaviour.executeIntent(this.player);
    }
    narrate("Enemy makes its move.");
    handle(BattleEvent.ENEMY_ACTION_RESOLVED);
  }

  private void enterEnemyResolved() {
    // If the battle is over, abort and head straight to ending
    if (this.isBattleOver()) {
      return;
    }

    // If another enemy is successfully targeted.
    if (this.targetNextEnemy()) {
      this.setEnemyIntent(resolveEnemyIntent(this.getEnemy()));
      handle(BattleEvent.MORE_ENEMIES);
      return;
    }

    this.cleanUp();
    handle(BattleEvent.ENEMY_PHASE_COMPLETE);
  }

  private void enterVictory() {
    this.cleanUp();
    narrate("Victory! Every enemy has been defeated.");
    eventHandler.trigger(BATTLE_ENDED_EVENT, Boolean.TRUE);
  }

  private void enterDefeat() {
    this.cleanUp();
    narrate("Defeat. The player has fallen.");
    eventHandler.trigger(BATTLE_ENDED_EVENT, Boolean.FALSE);
  }
}
