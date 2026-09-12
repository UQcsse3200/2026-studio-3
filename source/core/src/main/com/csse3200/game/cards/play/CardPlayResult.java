package com.csse3200.game.cards.play;

import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import java.util.List;

/** Immutable result of attempting to play one card from the player's current hand. */
public record CardPlayResult(
    String cardId,
    CardPlayTarget target,
    boolean success,
    int energyCost,
    CardEffectResolution effectResolution,
    CardPlayFailureReason failureReason,
    DeckSnapshot deckSnapshot) {
  public CardPlayResult {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (energyCost < 0) {
      throw new IllegalArgumentException("Energy cost cannot be negative");
    }
    if (failureReason == null) {
      throw new IllegalArgumentException("Card play failure reason cannot be null");
    }
    if (deckSnapshot == null) {
      throw new IllegalArgumentException("Card play deck snapshot cannot be null");
    }
    if (success) {
      if (failureReason != CardPlayFailureReason.NONE) {
        throw new IllegalArgumentException("Successful card play cannot have a failure reason");
      }
      if (effectResolution == null) {
        throw new IllegalArgumentException("Successful card play must include a resolution");
      }
    } else {
      if (failureReason == CardPlayFailureReason.NONE) {
        throw new IllegalArgumentException("Failed card play must include a failure reason");
      }
      if (effectResolution != null) {
        throw new IllegalArgumentException("Failed card play cannot include a resolution");
      }
    }
  }

  /** Backwards-compatible constructor for callers that do not yet send target/deck information. */
  public CardPlayResult(
      String cardId,
      boolean successful,
      int energyCost,
      CardEffectResolution resolution,
      CardPlayFailureReason failureReason) {
    this(cardId, null, successful, energyCost, resolution, failureReason, DeckSnapshot.empty());
  }

  /**
   * Creates a successful play result.
   *
   * @param cardId played card ID
   * @param energyCost energy spent through Team 7
   * @param resolution resolved card effects from Team 5
   * @return successful card play result
   */
  public static CardPlayResult success(
      String cardId, int energyCost, CardEffectResolution resolution) {
    return success(cardId, null, energyCost, resolution, DeckSnapshot.empty());
  }

  /** Creates a successful result for the unified card-play request flow. */
  public static CardPlayResult success(
      String cardId,
      CardPlayTarget target,
      int energyCost,
      CardEffectResolution resolution,
      DeckSnapshot deckSnapshot) {
    return new CardPlayResult(
        cardId, target, true, energyCost, resolution, CardPlayFailureReason.NONE, deckSnapshot);
  }

  /**
   * Creates a failed play result.
   *
   * @param cardId requested card ID
   * @param energyCost energy cost that would have been paid
   * @param failureReason reason the card was not played
   * @return failed card play result
   */
  public static CardPlayResult failure(
      String cardId, int energyCost, CardPlayFailureReason failureReason) {
    return failure(cardId, null, energyCost, failureReason, DeckSnapshot.empty());
  }

  /** Creates a failed result for the unified card-play request flow. */
  public static CardPlayResult failure(
      String cardId,
      CardPlayTarget target,
      int energyCost,
      CardPlayFailureReason failureReason,
      DeckSnapshot deckSnapshot) {
    return new CardPlayResult(cardId, target, false, energyCost, null, failureReason, deckSnapshot);
  }

  /** Backwards-compatible alias for the earlier result API. */
  public boolean successful() {
    return success;
  }

  /** Backwards-compatible alias for the earlier result API. */
  public CardEffectResolution resolution() {
    return effectResolution;
  }

  /**
   * @return resolved enemy-targeting effects, or an empty list when the card was not played
   */
  public List<ResolvedCardEffect> enemyEffects() {
    return effectResolution == null ? List.of() : effectResolution.enemyEffects();
  }

  /**
   * @return resolved player-targeting effects, or an empty list when the card was not played
   */
  public List<ResolvedCardEffect> playerEffects() {
    return effectResolution == null ? List.of() : effectResolution.playerEffects();
  }

  /**
   * @return immutable post-attempt hand snapshot for Team 3/UI
   */
  public List<String> updatedHand() {
    return deckSnapshot.updatedHand();
  }

  /**
   * @return immutable post-attempt draw-pile snapshot for Team 3/UI
   */
  public List<String> updatedDrawPile() {
    return deckSnapshot.updatedDrawPile();
  }

  /**
   * @return immutable post-attempt discard-pile snapshot for Team 3/UI
   */
  public List<String> updatedDiscardPile() {
    return deckSnapshot.updatedDiscardPile();
  }
}
