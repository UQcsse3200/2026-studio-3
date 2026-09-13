package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardValidator;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.ArrayList;
import java.util.List;

/** Resolves Team 6 card configs into Team 5 card effect results. */
public class CardEffectResolver {
  private final CardService cardService;
  private final EffectExecutor effectExecutor;

  public CardEffectResolver() {
    this(null, new EffectExecutor());
  }

  public CardEffectResolver(CardService cardService) {
    this(cardService, new EffectExecutor());
  }

  public CardEffectResolver(EffectExecutor effectExecutor) {
    this(null, effectExecutor);
  }

  public CardEffectResolver(CardService cardService, EffectExecutor effectExecutor) {
    if (effectExecutor == null) {
      throw new IllegalArgumentException("Effect executor cannot be null");
    }
    this.cardService = cardService;
    this.effectExecutor = effectExecutor;
  }

  /** Resolves a card that has already been retrieved from Team 6's card library. */
  public CardEffectResolution resolve(CardConfig card, PlayerEffectState playerState) {
    validate(card, playerState);

    List<ResolvedCardEffect> results = new ArrayList<>();
    for (int i = 0; i < card.effects.length; i++) {
      results.add(effectExecutor.resolve(card.id, card.effects[i], card.target, i, playerState));
    }
    return new CardEffectResolution(card.id, results);
  }

  /**
   * Resolves a card using read-only combat state supplied by the unified card-play flow.
   *
   * <p>This does not mutate Team 5's backwards-compatible {@link PlayerEffectState}. When Team 7 is
   * the source of truth for player statuses, later Strength values should come from the next
   * external context instead.
   */
  public CardEffectResolution resolve(CardConfig card, CardEffectResolutionContext context) {
    validate(card, context);

    List<ResolvedCardEffect> results = new ArrayList<>();
    for (int i = 0; i < card.effects.length; i++) {
      results.add(effectExecutor.resolve(card.id, card.effects[i], card.target, i, context));
    }
    return new CardEffectResolution(card.id, results);
  }

  /** Resolves a card by ID through the Team 6 card service supplied to this resolver. */
  public CardEffectResolution resolve(String cardId, PlayerEffectState playerState) {
    if (cardService == null) {
      throw new IllegalStateException("Card service is required to resolve by card ID");
    }
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    CardConfig card =
        cardService
            .getCard(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown card ID: " + cardId));
    return resolve(card, playerState);
  }

  /** Resolves a card by ID using read-only combat state supplied by the caller. */
  public CardEffectResolution resolve(String cardId, CardEffectResolutionContext context) {
    if (cardService == null) {
      throw new IllegalStateException("Card service is required to resolve by card ID");
    }
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    CardConfig card =
        cardService
            .getCard(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown card ID: " + cardId));
    return resolve(card, context);
  }

  private void validate(CardConfig card, PlayerEffectState playerState) {
    if (playerState == null) {
      throw new IllegalArgumentException("Player effect state cannot be null");
    }
    validateCard(card);
  }

  private void validate(CardConfig card, CardEffectResolutionContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Card effect resolution context cannot be null");
    }
    validateCard(card);
  }

  private void validateCard(CardConfig card) {
    List<String> errors = CardValidator.validate(card);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid card config: " + String.join("; ", errors));
    }
    for (EffectConfig effect : card.effects) {
      if (effect == null) {
        throw new IllegalArgumentException("Card effects cannot contain null");
      }
    }
  }
}
