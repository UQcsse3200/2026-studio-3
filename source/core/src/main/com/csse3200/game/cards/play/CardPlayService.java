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

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates Team 5 card play with Team 7 energy state.
 *
 * <p>This class does not own player energy. It asks Team 7's {@link EnergyComponent} to spend the
 * card cost, then resolves Team 5 card effects and moves the card from hand to discard.
 */
public final class CardPlayService {
  private static final int REARRANGE_ENERGY_COST = 1;

  private final CardService cardService;
  private final CardEffectResolutionService resolutionService;
  private final BattleDeck battleDeck;
  private final EnergyComponent energyComponent;
  private final PlayerStateView playerStateView;
  private final EnemyStateView enemyStateView;
  private final CardCooldownTracker cooldownTracker;

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
    this.cooldownTracker = new CardCooldownTracker(battleDeck);
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
   * Ticks every discarded card's cooldown down by one player round, retrieving any that reach zero
   * straight back into the hand. Intended to be called once at the start of each player round (see
   * {@code BattleController.enterPlayerStart}).
   *
   * @return IDs of cards retrieved this round, in retrieval order (empty if none)
   */
  public List<String> onPlayerRoundStart() {
    return cooldownTracker.tickRoundAndRetrieve();
  }

  /**
   * @return a snapshot of the current hand, for callers (e.g. the battle controller) that need to
   *     refresh the UI after {@link #onPlayerRoundStart()} changes it without a card being played
   */
  public List<CardInstance> currentHand() {
    return battleDeck.getHand();
  }

  /**
   * Checks whether a specific card instance is currently tracked as on cooldown (discarded and
   * waiting to be automatically retrieved), as distinct from any other copy of the same card.
   *
   * @param instance the card instance to check
   * @return true if that exact instance is on cooldown
   */
  public boolean isOnCooldown(CardInstance instance) {
    return cooldownTracker.isOnCooldown(instance);
  }

  /**
   * @return a snapshot of every card instance currently sitting in the discard pile — i.e. every
   *     instance NOT eligible to be placed back in hand by {@link #rearrangeHand}, regardless of
   *     whether it's formally tracked by cooldown (it always is on this path, but callers should
   *     treat discard-pile membership, not cooldown tracking, as the source of truth for "can this
   *     be selected")
   */
  public List<CardInstance> discardedInstances() {
    return battleDeck.getDiscardPileInstances();
  }

  /**
   * @return a snapshot of every card instance the player owns for this battle — draw pile, hand and
   *     discard pile combined — for callers (e.g. the deck-rearrange UI) that need to offer the
   *     whole pool regardless of which pile a given copy currently sits in
   */
  public List<CardInstance> allInstances() {
    return battleDeck.getAllInstances();
  }

  /**
   * Replaces the current hand with an exact set of card instances chosen from the player's full
   * pool, at a cost of 1 energy — but only when the player's actually-playable hand changes as a
   * result, so re-confirming the same cards is free and never touches energy.
   *
   * <p>A selected instance that's currently on cooldown in the discard pile is allowed — it isn't
   * moved (its cooldown keeps counting down exactly as it would have anyway) — but it's filtered
   * out before touching the battle deck's hand, since a card that's still cooling down can never
   * physically sit in a playable hand. Selecting it is purely a UI-visible "reserve this slot for
   * when it returns" choice; see {@code BattleScreen.onDeckRearranged} for where that's rendered.
   *
   * @param newHand exact card instances the player chose (any mix of currently-playable and
   *     currently-on-cooldown instances)
   * @return true if the playable hand was actually changed (and energy spent), false if the request
   *     was a no-op because the playable portion of the selection matched the current hand already
   * @throws IllegalArgumentException if a requested instance isn't part of this battle deck at all
   * @throws IllegalStateException if energy could not be spent for a genuine change
   */
  public boolean rearrangeHand(List<CardInstance> newHand) {
    if (newHand == null) {
      throw new IllegalArgumentException("newHand must not be null");
    }
    Set<CardInstance> onCooldown = Set.copyOf(battleDeck.getDiscardPileInstances());
    List<CardInstance> playableSelection =
            newHand.stream().filter(instance -> !onCooldown.contains(instance)).toList();

    if (Set.copyOf(playableSelection).equals(Set.copyOf(battleDeck.getHandInstances()))) {
      return false;
    }
    if (!energyComponent.canAfford(REARRANGE_ENERGY_COST)
            || !energyComponent.spendEnergy(REARRANGE_ENERGY_COST)) {
      throw new IllegalStateException("Not enough energy to rearrange the hand");
    }
    try {
      battleDeck.setHandInstances(playableSelection);
    } catch (RuntimeException exception) {
      energyComponent.restoreEnergy(REARRANGE_ENERGY_COST);
      throw exception;
    }
    return true;
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
    if (target == null || target.type() != TargetType.SINGLE_ENEMY || enemyStateView == null) {
      return true;
    }
    return enemyStateView.isTargetAvailable(target.targetId());
  }

  private CardEffectResolution resolveEffects(
          ResolvedCard card,
          CardPlayTarget target) {

    if (playerStateView == null && enemyStateView == null) {
      return resolutionService.resolve(card);
    }

    return resolutionService.resolve(
            card,
            buildResolutionContext(target));
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
