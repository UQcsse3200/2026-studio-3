package com.csse3200.game.cards.play;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardValidator;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.CardEffectResolutionContext;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.player.EnergyComponent;
import java.util.Optional;

/**
 * Coordinates Team 5 card play with Team 7 energy state.
 *
 * <p>This class does not own player energy. It asks Team 7's {@link EnergyComponent} to spend the
 * card cost, then resolves Team 5 card effects and moves the card from hand to discard.
 */
public final class CardPlayService {
  private final CardService cardService;
  private final CardEffectResolutionService resolutionService;
  private final BattleDeck battleDeck;
  private final EnergyComponent energyComponent;
  private final PlayerStateView playerStateView;
  private final EnemyStateView enemyStateView;

  /**
   * Creates a play service using Team 6 card retrieval, Team 5 effect resolution and Team 7 energy.
   *
   * @param cardService source of card configs
   * @param battleDeck current combat deck state
   * @param energyComponent Team 7 player energy component
   */
  public CardPlayService(
      CardService cardService, BattleDeck battleDeck, EnergyComponent energyComponent) {
    this(
        cardService,
        new CardEffectResolutionService(requireCardService(cardService)),
        battleDeck,
        energyComponent,
        null,
        null);
  }

  /** Creates a play service that can resolve damage from Team 7/Team 1 read-only state views. */
  public CardPlayService(
      CardService cardService,
      BattleDeck battleDeck,
      EnergyComponent energyComponent,
      PlayerStateView playerStateView,
      EnemyStateView enemyStateView) {
    this(
        cardService,
        new CardEffectResolutionService(requireCardService(cardService)),
        battleDeck,
        energyComponent,
        playerStateView,
        enemyStateView);
  }

  /** Creates a play service with explicit dependencies for integration and testing. */
  public CardPlayService(
      CardService cardService,
      CardEffectResolutionService resolutionService,
      BattleDeck battleDeck,
      EnergyComponent energyComponent) {
    this(cardService, resolutionService, battleDeck, energyComponent, null, null);
  }

  /** Creates a play service with explicit dependencies and optional read-only state views. */
  public CardPlayService(
      CardService cardService,
      CardEffectResolutionService resolutionService,
      BattleDeck battleDeck,
      EnergyComponent energyComponent,
      PlayerStateView playerStateView,
      EnemyStateView enemyStateView) {
    this.cardService = requireCardService(cardService);
    if (resolutionService == null) {
      throw new IllegalArgumentException("Card effect resolution service cannot be null");
    }
    if (battleDeck == null) {
      throw new IllegalArgumentException("Battle deck cannot be null");
    }
    if (energyComponent == null) {
      throw new IllegalArgumentException("Energy component cannot be null");
    }
    this.resolutionService = resolutionService;
    this.battleDeck = battleDeck;
    this.energyComponent = energyComponent;
    this.playerStateView = playerStateView;
    this.enemyStateView = enemyStateView;
  }

  /** Resolves an exact copy currently in hand; invalid/missing definitions are not playable. */
  public Optional<ResolvedCard> resolveInHand(String instanceId) {
    CardInstance instance = battleDeck.getCardInHand(instanceId).orElse(null);
    if (instance == null) return Optional.empty();
    CardConfig config = cardService.getCard(instance.cardId()).orElse(null);
    if (config == null || !CardValidator.validate(config).isEmpty()) return Optional.empty();
    try {
      ResolvedCard card = new CardResolver().resolve(config, instance);
      return CardValidator.validateResolved(card).isEmpty() ? Optional.of(card) : Optional.empty();
    } catch (IllegalArgumentException | IllegalStateException exception) {
      return Optional.empty();
    }
  }

  /** Checks affordability of an exact copy; use the request overload to check a selected target. */
  public boolean canPlay(String instanceId) {
    return resolveInHand(instanceId)
        .filter(card -> energyComponent.canAfford(card.cost()))
        .isPresent();
  }

  /** Read-only check of instance, resolved cost, target type and target availability. */
  public boolean canPlay(CardPlayRequest request) {
    return request != null
        && resolveInHand(request.instanceId())
            .filter(
                card ->
                    isValidTarget(card, request.target()) && isTargetAvailable(request.target()))
            .filter(card -> energyComponent.canAfford(card.cost()))
            .isPresent();
  }

  /**
   * Validates an instance request, spends resolved cost once, resolves its effects and discards
   * that exact copy. Expected failures leave energy and all piles unchanged. Unexpected failures
   * restore energy and propagate; consumers apply returned effects separately.
   */
  public CardPlayResult playCard(CardPlayRequest request) {
    if (request == null) throw new IllegalArgumentException("Card play request cannot be null");
    CardInstance instance = battleDeck.getCardInHand(request.instanceId()).orElse(null);
    if (instance == null) return failure(request, null, 0, CardPlayFailureReason.CARD_NOT_IN_HAND);
    if (cardService.getCard(instance.cardId()).isEmpty()) {
      return failure(request, instance.cardId(), 0, CardPlayFailureReason.UNKNOWN_CARD);
    }
    ResolvedCard card = resolveInHand(request.instanceId()).orElse(null);
    if (card == null)
      return failure(request, instance.cardId(), 0, CardPlayFailureReason.INVALID_CARD_CONFIG);
    if (!isValidTarget(card, request.target()) || !isTargetAvailable(request.target())) {
      return failure(request, card.cardId(), card.cost(), CardPlayFailureReason.INVALID_TARGET);
    }
    if (!energyComponent.canAfford(card.cost()) || !energyComponent.spendEnergy(card.cost())) {
      return failure(request, card.cardId(), card.cost(), CardPlayFailureReason.NOT_ENOUGH_ENERGY);
    }
    try {
      CardEffectResolution resolution =
          playerStateView == null && enemyStateView == null
              ? resolutionService.resolve(card)
              : resolutionService.resolve(card, buildResolutionContext(request.target()));
      if (!battleDeck.playCard(card.instanceId())) {
        throw new IllegalStateException("Card left hand during play: " + card.instanceId());
      }
      return CardPlayResult.success(
          card.instanceId(),
          card.cardId(),
          request.target(),
          card.cost(),
          resolution,
          DeckSnapshot.from(battleDeck));
    } catch (RuntimeException exception) {
      energyComponent.restoreEnergy(card.cost());
      throw exception;
    }
  }

  private CardPlayResult failure(
      CardPlayRequest request, String cardId, int cost, CardPlayFailureReason reason) {
    return CardPlayResult.failure(
        request.instanceId(),
        cardId,
        request.target(),
        cost,
        reason,
        DeckSnapshot.from(battleDeck));
  }

  private boolean isValidTarget(ResolvedCard card, CardPlayTarget target) {
    return target != null && card.target() == target.type();
  }

  private boolean isTargetAvailable(CardPlayTarget target) {
    return target.type() != TargetType.SINGLE_ENEMY
        || enemyStateView == null
        || enemyStateView.isTargetAvailable(target.targetId());
  }

  private CardEffectResolutionContext buildResolutionContext(CardPlayTarget target) {
    int strength = playerStateView == null ? 0 : playerStateView.statusValue(EffectType.STRENGTH);
    int outgoingFeeble =
        playerStateView == null ? 0 : playerStateView.statusValue(EffectType.FEEBLE);
    int targetVulnerable = 0;

    if (target != null && target.type() == TargetType.SINGLE_ENEMY && enemyStateView != null) {
      targetVulnerable = enemyStateView.statusValue(target.targetId(), EffectType.VULNERABLE);
    }

    return new CardEffectResolutionContext(strength, outgoingFeeble, targetVulnerable);
  }

  private static CardService requireCardService(CardService cardService) {
    if (cardService == null) {
      throw new IllegalArgumentException("Card service cannot be null");
    }
    return cardService;
  }
}
