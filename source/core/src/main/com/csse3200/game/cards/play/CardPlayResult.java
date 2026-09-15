package com.csse3200.game.cards.play;

import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.List;

/** Immutable result of attempting to play one card from the player's current hand. */
public record CardPlayResult(
    String instanceId,
    String cardId,
    CardPlayTarget target,
    boolean success,
    int energyCost,
    CardEffectResolution effectResolution,
    CardPlayFailureReason failureReason,
    DeckSnapshot deckSnapshot) {
  public CardPlayResult {
    if (instanceId == null || instanceId.isBlank()) {
      throw new IllegalArgumentException("Instance ID cannot be null or blank");
    }
    if (success && (cardId == null || cardId.isBlank())) {
      throw new IllegalArgumentException("Successful play must identify its card definition");
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

  /** Creates a successful result for the unified card-play request flow. */
  public static CardPlayResult success(
      String instanceId,
      String cardId,
      CardPlayTarget target,
      int energyCost,
      CardEffectResolution resolution,
      DeckSnapshot deckSnapshot) {
    return new CardPlayResult(
        instanceId,
        cardId,
        target,
        true,
        energyCost,
        resolution,
        CardPlayFailureReason.NONE,
        deckSnapshot);
  }

  /** Creates a failed result for the unified card-play request flow. */
  public static CardPlayResult failure(
      String instanceId,
      String cardId,
      CardPlayTarget target,
      int energyCost,
      CardPlayFailureReason failureReason,
      DeckSnapshot deckSnapshot) {
    return new CardPlayResult(
        instanceId, cardId, target, false, energyCost, null, failureReason, deckSnapshot);
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
  public List<CardInstance> updatedHand() {
    return deckSnapshot.updatedHand();
  }

  /**
   * @return immutable post-attempt draw-pile snapshot for Team 3/UI
   */
  public List<CardInstance> updatedDrawPile() {
    return deckSnapshot.updatedDrawPile();
  }

  /**
   * @return immutable post-attempt discard-pile snapshot for Team 3/UI
   */
  public List<CardInstance> updatedDiscardPile() {
    return deckSnapshot.updatedDiscardPile();
  }
}
