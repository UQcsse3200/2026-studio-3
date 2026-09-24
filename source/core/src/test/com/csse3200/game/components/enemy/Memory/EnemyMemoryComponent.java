package com.csse3200.game.components.enemy;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import java.util.Objects;

/**
 * Tracks the player's combat behaviour for enemy AI.
 *
 * <p>One instance belongs to the player entity for the lifetime of a combat encounter. All enemies
 * read the same immutable {@link PlayerMemory} snapshot, ensuring that they observe a consistent
 * record of the player's behaviour.
 *
 * <p>The component listens to successful card-play and battle-phase events exposed by {@link
 * BattleController}. Card instances are resolved through the battle deck and card service before
 * being classified.
 */
public class EnemyMemoryComponent extends Component {
  private final BattleController battleController;
  private final CardService cardService;
  private final BattleDeck battleDeck;

  private int attackCardsPlayed;
  private int skillCardsPlayed;
  private int consecutiveTurnsWithoutBlock;
  private int cardsPlayedThisTurn;
  private int cardsPlayedLastTurn;
  private boolean gainedBlockThisTurn;

  /**
   * Creates a player memory component for one combat encounter.
   *
   * @param battleController source of successful card-play and battle-phase events
   * @param cardService source of card configurations
   * @param battleDeck source of runtime card-instance information
   */
  public EnemyMemoryComponent(
      BattleController battleController, CardService cardService, BattleDeck battleDeck) {
    this.battleController =
        Objects.requireNonNull(battleController, "battleController cannot be null");
    this.cardService = Objects.requireNonNull(cardService, "cardService cannot be null");
    this.battleDeck = Objects.requireNonNull(battleDeck, "battleDeck cannot be null");
  }

  /**
   * Subscribes to combat events.
   *
   * <p>{@code cardPlayed} is emitted only after a card play succeeds, so rejected card plays do not
   * affect memory.
   */
  @Override
  public void create() {
    super.create();
    battleController.addCardPlayedListener(this::onCardPlayed);
    battleController.addPhaseChangeListener(this::onPhaseChanged);
  }

  /**
   * Returns an immutable copy of the currently recorded player behaviour.
   *
   * @return current player-memory snapshot
   */
  public PlayerMemory snapshot() {
    return new PlayerMemory(
        attackCardsPlayed, skillCardsPlayed, consecutiveTurnsWithoutBlock, cardsPlayedLastTurn);
  }

  /**
   * Records one successfully played card.
   *
   * <p>The event provides a runtime instance ID rather than a card-definition ID. The instance is
   * therefore resolved through {@link BattleDeck}, after which its {@link CardConfig} is retrieved
   * through {@link CardService}.
   *
   * @param instanceId ID of the exact card instance that was played
   * @param targetId ID of the selected target; unused by player-memory classification
   */
  private void onCardPlayed(String instanceId, String targetId) {
    CardConfig config = findCardConfig(instanceId);
    if (config == null) {
      return;
    }

    cardsPlayedThisTurn++;

    if (config.type == CardType.ATTACK) {
      attackCardsPlayed++;
    } else if (config.type == CardType.SKILL) {
      skillCardsPlayed++;
    }

    if (grantsBlock(config)) {
      gainedBlockThisTurn = true;
    }
  }

  /**
   * Settles the player's memory when the real player turn ends.
   *
   * <p>The battle state machine temporarily transitions from {@link BattlePhase#PLAYER_TURN} to
   * {@link BattlePhase#CARD_RESOLVING} whenever a card is played. That transition does not end the
   * player's turn and must not settle the counters. A transition from {@code PLAYER_TURN} to
   * another non-resolution phase represents leaving the player turn.
   *
   * @param previousPhase phase being exited
   * @param nextPhase phase being entered
   */
  private void onPhaseChanged(BattlePhase previousPhase, BattlePhase nextPhase) {
    boolean leavingPlayerTurn =
        previousPhase == BattlePhase.PLAYER_TURN
            && nextPhase != BattlePhase.PLAYER_TURN
            && nextPhase != BattlePhase.CARD_RESOLVING;

    if (leavingPlayerTurn) {
      settleTurn();
    }
  }

  /**
   * Carries the current card count into the completed-turn field, updates the no-block streak and
   * resets all per-turn state.
   */
  private void settleTurn() {
    cardsPlayedLastTurn = cardsPlayedThisTurn;

    if (gainedBlockThisTurn) {
      consecutiveTurnsWithoutBlock = 0;
    } else {
      consecutiveTurnsWithoutBlock++;
    }

    cardsPlayedThisTurn = 0;
    gainedBlockThisTurn = false;
  }

  /**
   * Finds the shared card configuration belonging to a runtime card instance.
   *
   * <p>The {@code cardPlayed} event is emitted after the successful card has been moved from the
   * hand to the discard pile. {@link BattleDeck#getAllInstances()} is used because it searches the
   * draw pile, hand and discard pile rather than only the current hand.
   *
   * @param instanceId runtime card-instance ID
   * @return matching card configuration, or {@code null} when the instance or definition is unknown
   */
  private CardConfig findCardConfig(String instanceId) {
    if (instanceId == null || instanceId.isBlank()) {
      return null;
    }

    for (CardInstance instance : battleDeck.getAllInstances()) {
      if (instance.instanceId().equals(instanceId)) {
        return cardService.getCard(instance.cardId()).orElse(null);
      }
    }

    return null;
  }

  /**
   * Determines whether a card contains a positive block effect.
   *
   * @param config card configuration to inspect
   * @return true when the card grants a positive amount of block
   */
  private boolean grantsBlock(CardConfig config) {
    if (config.effects == null) {
      return false;
    }

    for (EffectConfig effect : config.effects) {
      if (effect != null && effect.type == EffectType.BLOCK && effect.value > 0) {
        return true;
      }
    }

    return false;
  }
}
