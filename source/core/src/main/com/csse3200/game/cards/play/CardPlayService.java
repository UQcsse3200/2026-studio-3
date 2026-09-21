package com.csse3200.game.cards.play;

import com.csse3200.game.cards.CardCooldown;
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
import com.csse3200.game.components.player.EnergyComponent;
import java.util.List;
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

  /**
   * Read-only check for whether a card is currently playable.
   *
   * <p>This is intended for UI previews. The authoritative play path remains {@link #playCard},
   * which calls Team 7's {@link EnergyComponent#spendEnergy(int)}.
   *
   * @param cardId card ID to check
   * @return true if the card is in hand and Team 7 says the player can afford it
   */
  public boolean canPlay(String cardId) {
    CardConfig card = getPlayableCardConfig(cardId);
    return battleDeck.getHand().contains(card.id) && energyComponent.canAfford(card.cost);
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

//  /**
//   * @return a snapshot of the current hand as distinct card instances, for callers (e.g. UI
//   *     dimming) that need to tell duplicate copies of the same card apart
//   */
//  public List<CardInstance> handInstances() {
//    return battleDeck.getHand();
//  }

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
   * Read-only preview for the unified Team 3 request flow.
   *
   * @param request card and selected target
   * @return true only when the card, target, hand and energy checks currently pass
   */
  public boolean canPlay(CardPlayRequest request) {
    if (request == null) {
      return false;
    }
    CardConfig card = cardService.getCard(request.cardId()).orElse(null);
    return card != null
        && CardValidator.validate(card).isEmpty()
        && isValidTarget(card, request.target())
        && isTargetAvailable(request.target())
        && battleDeck.getHand().contains(card.id)
        && energyComponent.canAfford(card.cost);
  }

  /**
   * Attempts to play a card from the current hand.
   *
   * <p>When successful, Team 7 energy is spent first, then Team 5 resolves effects, then the battle
   * deck moves the card from hand to discard. When energy is insufficient, no card effects are
   * resolved and the hand remains unchanged.
   *
   * @param cardId card ID to play
   * @return structured play result with either resolved effects or a failure reason
   */
  public CardPlayResult playCard(String cardId) {
    CardConfig card = getPlayableCardConfig(cardId);
    return playValidatedCard(card, null);
  }

  /**
   * Unified Team 5 entry point for a card play attempt from Team 3/battle flow.
   *
   * <p>Expected validation failures are returned as data. A failed result never spends energy or
   * moves the card. A successful result spends energy exactly once, records the resolved effects,
   * moves the card to the discard pile and includes immutable deck snapshots.
   *
   * @param request card ID and already selected target
   * @return complete result for UI coordination and Team 1/Team 7 effect consumers
   */
  public CardPlayResult playCard(CardPlayRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Card play request cannot be null");
    }

    CardConfig card = cardService.getCard(request.cardId()).orElse(null);
    if (card == null) {
      return failure(request, 0, CardPlayFailureReason.UNKNOWN_CARD);
    }
    if (!CardValidator.validate(card).isEmpty()) {
      return failure(request, Math.max(card.cost, 0), CardPlayFailureReason.INVALID_CARD_CONFIG);
    }
    if (!isValidTarget(card, request.target())) {
      return failure(request, card.cost, CardPlayFailureReason.INVALID_TARGET);
    }
    if (!isTargetAvailable(request.target())) {
      return failure(request, card.cost, CardPlayFailureReason.INVALID_TARGET);
    }
    return playValidatedCard(card, request.target());
  }

  private CardPlayResult playValidatedCard(CardConfig card, CardPlayTarget target) {
    if (!battleDeck.getHand().contains(card.id)) {
      return failure(card.id, target, card.cost, CardPlayFailureReason.CARD_NOT_IN_HAND);
    }

    // This read-only check gives the common failure a stable reason. spendEnergy() remains the
    // authoritative atomic check-and-spend in case energy changes between the two calls.
    if (!energyComponent.canAfford(card.cost) || !energyComponent.spendEnergy(card.cost)) {
      return failure(card.id, target, card.cost, CardPlayFailureReason.NOT_ENOUGH_ENERGY);
    }

    try {
      CardEffectResolution resolution = resolveEffects(card, target);
      CardInstance discarded = battleDeck.discardCardInstance(card.id);
      if (discarded == null) {
        throw new IllegalStateException(
            "Card was no longer in hand after energy was spent: " + card.id);
      }
      cooldownTracker.trackDiscard(discarded, CardCooldown.roundsFor(card));
      return CardPlayResult.success(
          card.id, target, card.cost, resolution, DeckSnapshot.from(battleDeck));
    } catch (RuntimeException exception) {
      // Restore Team 7 energy when a later internal operation fails. The exception is rethrown so
      // callers never mistake an incomplete play for a normal validation failure.
      energyComponent.restoreEnergy(card.cost);
      throw exception;
    }
  }

  private CardPlayResult failure(
      CardPlayRequest request, int energyCost, CardPlayFailureReason failureReason) {
    return failure(request.cardId(), request.target(), energyCost, failureReason);
  }

  private CardPlayResult failure(
      String cardId, CardPlayTarget target, int energyCost, CardPlayFailureReason failureReason) {
    return CardPlayResult.failure(
        cardId, target, energyCost, failureReason, DeckSnapshot.from(battleDeck));
  }

  private boolean isValidTarget(CardConfig card, CardPlayTarget target) {
    if (target == null || card.target != target.type()) {
      return false;
    }
    return card.target != TargetType.SINGLE_ENEMY || target.targetId() != null;
  }

  private boolean isTargetAvailable(CardPlayTarget target) {
    if (target == null || target.type() != TargetType.SINGLE_ENEMY || enemyStateView == null) {
      return true;
    }
    return enemyStateView.isTargetAvailable(target.targetId());
  }

  private CardEffectResolution resolveEffects(CardConfig card, CardPlayTarget target) {
    if (playerStateView == null && enemyStateView == null) {
      return resolutionService.resolve(card);
    }
    return resolutionService.resolve(card, buildResolutionContext(target));
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

  private CardConfig getPlayableCardConfig(String cardId) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    CardConfig card =
        cardService
            .getCard(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown card ID: " + cardId));
    List<String> errors = CardValidator.validate(card);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid card config: " + String.join("; ", errors));
    }
    return card;
  }

  private static CardService requireCardService(CardService cardService) {
    if (cardService == null) {
      throw new IllegalArgumentException("Card service cannot be null");
    }
    return cardService;
  }
}
