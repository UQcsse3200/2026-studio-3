package com.csse3200.game.components.enemy;

import com.csse3200.game.cards.CardType;
import com.csse3200.game.components.Component;
import java.util.Objects;

/**
 * Stores the player's observed combat behaviour for enemy AI.
 *
 * <p>One instance is attached to the player entity for each combat encounter. {@link
 * PlayerTrackerComponent} observes the player's actions and writes the resulting information into
 * this component.
 *
 * <p>Enemy behaviour components read immutable {@link PlayerMemory} snapshots when constructing
 * their AI contexts.
 *
 * <p>This component does not listen to battle events, resolve card instances or access card
 * configurations. Those responsibilities belong to {@link PlayerTrackerComponent}.
 */
public class EnemyMemoryComponent extends Component {
  private int attackCardsPlayed;
  private int skillCardsPlayed;

  private int cardsPlayedThisTurn;
  private int cardsPlayedLastTurn;

  private int consecutiveTurnsWithoutBlock;
  private boolean gainedBlockThisTurn;

  /**
   * Records one card successfully played by the player.
   *
   * <p>The player tracker resolves the card and determines whether it provided positive block
   * before calling this method.
   *
   * <p>All card types count towards the number of cards played during the current turn. Only ATTACK
   * and SKILL cards contribute to their corresponding lifetime counters.
   *
   * @param cardType category of the successfully played card
   * @param gainedBlock whether the card provided positive block
   * @throws NullPointerException when cardType is null
   */
  public void recordCardPlayed(CardType cardType, boolean gainedBlock) {
    Objects.requireNonNull(cardType, "cardType cannot be null");

    cardsPlayedThisTurn++;

    switch (cardType) {
      case ATTACK -> attackCardsPlayed++;
      case SKILL -> skillCardsPlayed++;
      default -> {
        // Other card types still count towards cardsPlayedThisTurn.
      }
    }

    if (gainedBlock) {
      gainedBlockThisTurn = true;
    }
  }

  /**
   * Settles the recorded information when the player's real turn ends.
   *
   * <p>The number of cards played this turn becomes cardsPlayedLastTurn. The consecutive no-block
   * streak is reset if the player gained block during the turn; otherwise it increases by one.
   *
   * <p>After settlement, all temporary per-turn state is reset.
   */
  public void settlePlayerTurn() {
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
   * Returns an immutable snapshot of the currently stored player behaviour.
   *
   * <p>Changes made to this component after the call do not affect previously returned snapshots.
   *
   * @return immutable player-memory snapshot
   */
  public PlayerMemory snapshot() {
    return new PlayerMemory(
        attackCardsPlayed, skillCardsPlayed, consecutiveTurnsWithoutBlock, cardsPlayedLastTurn);
  }
}
