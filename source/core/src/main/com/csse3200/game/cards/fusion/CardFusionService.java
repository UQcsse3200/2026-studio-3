package com.csse3200.game.cards.fusion;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Applies the once-per-run Card Fusion rule: consume three Common card copies to gain one random
 * Rare card.
 */
public final class CardFusionService {
  public static final int REQUIRED_CARD_COUNT = 3;

  private final CardService cardService;
  private final Random random;

  /** Creates a fusion service with non-deterministic reward selection. */
  public CardFusionService(CardService cardService) {
    this(cardService, new Random());
  }

  /** Creates a fusion service with an injectable random source for deterministic tests. */
  public CardFusionService(CardService cardService, Random random) {
    this.cardService = Objects.requireNonNull(cardService, "cardService must not be null");
    this.random = Objects.requireNonNull(random, "random must not be null");
  }

  /**
   * Returns the exact Common card copies that may be selected for fusion in the supplied run.
   *
   * @param runState active run that owns the persistent player deck
   * @return immutable snapshot of eligible owned card instances
   */
  public List<CardInstance> getEligibleCards(RunState runState) {
    PlayerDeck deck = getDeck(runState);
    return deck.getCards().stream()
        .filter(card -> hasRarity(card.cardId(), Rarity.COMMON))
        .toList();
  }

  /**
   * Consumes three selected Common card instances and adds one equally likely Rare card.
   *
   * <p>All validation happens before the deck is changed, so a failed request leaves the player's
   * deck and once-per-run allowance untouched.
   *
   * @param runState active run that owns the persistent player deck and fusion allowance
   * @param selectedInstanceIds exactly three distinct selected runtime card IDs
   * @return the awarded Rare card ID on success, or a reason no fusion occurred
   */
  public CardFusionResult fuse(RunState runState, List<String> selectedInstanceIds) {
    Objects.requireNonNull(runState, "runState must not be null");
    if (runState.hasUsedCardFusion()) {
      return CardFusionResult.failure(CardFusionFailureReason.FUSION_ALREADY_USED);
    }
    if (!hasValidSelectionSize(selectedInstanceIds)) {
      return CardFusionResult.failure(CardFusionFailureReason.INVALID_SELECTION);
    }

    PlayerDeck deck = getDeck(runState);
    Map<String, CardInstance> ownedCardsByInstanceId = indexByInstanceId(deck.getCards());
    List<CardInstance> selectedCards = new ArrayList<>(REQUIRED_CARD_COUNT);
    for (String instanceId : selectedInstanceIds) {
      CardInstance card = ownedCardsByInstanceId.get(instanceId);
      if (card == null) {
        return CardFusionResult.failure(CardFusionFailureReason.CARD_NOT_IN_DECK);
      }
      if (!hasRarity(card.cardId(), Rarity.COMMON)) {
        return CardFusionResult.failure(CardFusionFailureReason.CARD_NOT_COMMON);
      }
      selectedCards.add(card);
    }

    List<CardConfig> rareCards = availableRareCards();
    if (rareCards.isEmpty()) {
      return CardFusionResult.failure(CardFusionFailureReason.NO_RARE_CARD_AVAILABLE);
    }

    CardConfig reward = rareCards.get(random.nextInt(rareCards.size()));
    for (CardInstance selectedCard : selectedCards) {
      boolean removed = deck.removeCardInstance(selectedCard.instanceId());
      if (!removed) {
        throw new IllegalStateException("Validated fusion card disappeared from the player deck");
      }
    }
    deck.addCard(reward.id);
    runState.markCardFusionUsed();
    return CardFusionResult.success(reward.id);
  }

  private PlayerDeck getDeck(RunState runState) {
    return runState.getOrCreatePlayerDeck(cardService);
  }

  private boolean hasRarity(String cardId, Rarity rarity) {
    return cardService.getCard(cardId).map(card -> card.rarity == rarity).orElse(false);
  }

  private static boolean hasValidSelectionSize(List<String> selectedInstanceIds) {
    if (selectedInstanceIds == null || selectedInstanceIds.size() != REQUIRED_CARD_COUNT) {
      return false;
    }
    Set<String> uniqueIds = new HashSet<>(selectedInstanceIds);
    return uniqueIds.size() == REQUIRED_CARD_COUNT
        && uniqueIds.stream().allMatch(id -> id != null && !id.isBlank());
  }

  private static Map<String, CardInstance> indexByInstanceId(List<CardInstance> cards) {
    Map<String, CardInstance> cardsByInstanceId = new HashMap<>();
    for (CardInstance card : cards) {
      cardsByInstanceId.put(card.instanceId(), card);
    }
    return cardsByInstanceId;
  }

  private List<CardConfig> availableRareCards() {
    return cardService.getAllCards().stream()
        .filter(card -> card.rarity == Rarity.RARE)
        .sorted(Comparator.comparing(card -> card.id))
        .toList();
  }
}
