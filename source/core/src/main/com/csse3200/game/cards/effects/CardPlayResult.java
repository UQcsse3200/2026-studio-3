package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.deck.BattleDeck;
import java.util.List;

/**
 * One-stop result of a single card-play attempt.
 *
 * <p>This mirrors the shape Team 5's card system is moving towards: Team 3 makes one call for a
 * card play and everything it needs to refresh the UI and hand work to other systems comes back
 * here. {@code BattleController.playCardThroughCardSystem} assembles it from Team 5's {@link
 * CardEffectResolver} plus the deck and energy bookkeeping.
 *
 * <p>On failure no effects are produced and the card stays in hand. On success the deck snapshots
 * already reflect the played card moved from hand to discard, and {@link #energyCost()} is the
 * energy that was spent.
 */
public record CardPlayResult(
    boolean success,
    String failureReason,
    String cardId,
    String targetId,
    List<ResolvedCardEffect> enemyEffects,
    List<ResolvedCardEffect> playerEffects,
    List<String> updatedHand,
    List<String> updatedDrawPile,
    List<String> updatedDiscardPile,
    int energyCost) {

  public CardPlayResult {
    enemyEffects = List.copyOf(enemyEffects == null ? List.of() : enemyEffects);
    playerEffects = List.copyOf(playerEffects == null ? List.of() : playerEffects);
    updatedHand = List.copyOf(updatedHand == null ? List.of() : updatedHand);
    updatedDrawPile = List.copyOf(updatedDrawPile == null ? List.of() : updatedDrawPile);
    updatedDiscardPile = List.copyOf(updatedDiscardPile == null ? List.of() : updatedDiscardPile);
  }

  /**
   * Builds a failed result. No effects, card stays in hand, deck snapshots are unchanged.
   *
   * @param reason human-readable reason the card could not be played
   * @param cardId the card that was attempted
   * @param targetId the requested target
   * @param deck the (unchanged) battle deck to snapshot
   * @return a failed result
   */
  public static CardPlayResult failure(
      String reason, String cardId, String targetId, BattleDeck deck) {
    return new CardPlayResult(
        false,
        reason,
        cardId,
        targetId,
        List.of(),
        List.of(),
        deck.getHand(),
        deck.getDrawPile(),
        deck.getDiscardPile(),
        0);
  }

  /**
   * Builds a successful result. Deck snapshots should be taken after the card has been moved to the
   * discard pile.
   *
   * @param cardId the card that was played
   * @param targetId the target the card was played on
   * @param enemyEffects resolved effects for enemy-facing systems (Team 1)
   * @param playerEffects resolved effects for player-facing systems (Team 7)
   * @param deck the battle deck to snapshot, already updated
   * @param energyCost energy that was spent playing the card
   * @return a successful result
   */
  public static CardPlayResult success(
      String cardId,
      String targetId,
      List<ResolvedCardEffect> enemyEffects,
      List<ResolvedCardEffect> playerEffects,
      BattleDeck deck,
      int energyCost) {
    return new CardPlayResult(
        true,
        null,
        cardId,
        targetId,
        enemyEffects,
        playerEffects,
        deck.getHand(),
        deck.getDrawPile(),
        deck.getDiscardPile(),
        energyCost);
  }
}
