package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import java.util.List;

/**
 * Public Team 5 entry point for resolving cards and querying the current turn's results.
 *
 * <p>The service owns only Team 5's calculation state and result history. It does not apply the
 * returned effects to real player or enemy statistics; consuming systems remain responsible for
 * those entity updates.
 */
public final class CardEffectResolutionService {
  private final CardEffectResolver resolver;
  private final PlayerEffectState playerEffectState;
  private final CardEffectResultStore resultStore;

  /** Creates a combat-scoped service backed by Team 6's public card retrieval API. */
  public CardEffectResolutionService(CardService cardService) {
    this(
        new CardEffectResolver(requireCardService(cardService)),
        new PlayerEffectState(),
        new TurnEffectStore());
  }

  /** Creates a service with explicit dependencies for integration and testing. */
  public CardEffectResolutionService(
      CardEffectResolver resolver,
      PlayerEffectState playerEffectState,
      CardEffectResultStore resultStore) {
    if (resolver == null) {
      throw new IllegalArgumentException("Card effect resolver cannot be null");
    }
    if (playerEffectState == null) {
      throw new IllegalArgumentException("Player effect state cannot be null");
    }
    if (resultStore == null) {
      throw new IllegalArgumentException("Card effect result store cannot be null");
    }
    this.resolver = resolver;
    this.playerEffectState = playerEffectState;
    this.resultStore = resultStore;
  }

  /** Resolves a card by ID and records the successful result for current-turn consumers. */
  public CardEffectResolution resolve(String cardId) {
    return record(resolver.resolve(cardId, playerEffectState));
  }

  /** Resolves a card by ID with read-only combat state and records the successful result. */
  public CardEffectResolution resolve(String cardId, CardEffectResolutionContext context) {
    return record(resolver.resolve(cardId, context));
  }

  /** Resolves an already retrieved Team 6 card config and records the successful result. */
  public CardEffectResolution resolve(CardConfig card) {
    return record(resolver.resolve(card, playerEffectState));
  }

  /** Resolves an already retrieved card config with read-only combat state. */
  public CardEffectResolution resolve(CardConfig card, CardEffectResolutionContext context) {
    return record(resolver.resolve(card, context));
  }

  /**
   * @return recorded card resolutions in play order
   */
  public List<CardEffectResolution> getResolutions() {
    return resultStore.getResolutions();
  }

  /**
   * @return current-turn enemy effects for Team 1 or the battle system
   */
  public List<ResolvedCardEffect> getEnemyEffects() {
    return resultStore.getEnemyEffects();
  }

  /**
   * @return current-turn player effects for Team 7 or the battle system
   */
  public List<ResolvedCardEffect> getPlayerEffects() {
    return resultStore.getPlayerEffects();
  }

  /**
   * @return current-turn effects matching the requested type
   */
  public List<ResolvedCardEffect> getEffectsOfType(EffectType type) {
    return resultStore.getEffectsOfType(type);
  }

  /**
   * Clears queryable turn results while preserving combat-long calculation state such as strength.
   */
  public void clearTurnResults() {
    resultStore.clear();
  }

  private CardEffectResolution record(CardEffectResolution resolution) {
    resultStore.record(resolution);
    return resolution;
  }

  private static CardService requireCardService(CardService cardService) {
    if (cardService == null) {
      throw new IllegalArgumentException("Card service cannot be null");
    }
    return cardService;
  }
}
