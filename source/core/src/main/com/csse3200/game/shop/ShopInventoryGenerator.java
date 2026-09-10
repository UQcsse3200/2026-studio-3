package com.csse3200.game.shop;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.configs.CardConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Builds a varied shop inventory from Team 6's current card library. */
public final class ShopInventoryGenerator {
  public static final int DEFAULT_OFFER_COUNT = 3;
  public static final int DEFAULT_MIN_STOCK = 1;
  public static final int DEFAULT_MAX_STOCK = 3;

  private static final int COMMON_PRICE = 20;
  private static final int UNCOMMON_PRICE = 35;
  private static final int RARE_PRICE = 50;
  private static final int PRICE_VARIANCE = 5;

  private final CardService cardService;
  private final Random random;

  /** Creates a generator with nondeterministic inventory selection. */
  public ShopInventoryGenerator(CardService cardService) {
    this(cardService, new Random());
  }

  /**
   * Creates a generator with an injectable random source for deterministic tests and seeded runs.
   *
   * @param cardService Team 6 card data source
   * @param random random source used for selection, price variation, and stock
   */
  public ShopInventoryGenerator(CardService cardService, Random random) {
    this.cardService = Objects.requireNonNull(cardService, "cardService cannot be null");
    this.random = Objects.requireNonNull(random, "random cannot be null");
  }

  /** Generates a shop using the default offer count and stock range. */
  public ShopService createShop() {
    return createShop(DEFAULT_OFFER_COUNT, DEFAULT_MIN_STOCK, DEFAULT_MAX_STOCK);
  }

  /**
   * Generates a shop from a random subset of currently registered cards.
   *
   * <p>If fewer valid cards are available than requested, every valid card is offered once.
   *
   * @param offerCount maximum number of distinct cards to offer
   * @param minStock minimum stock for each offer, inclusive
   * @param maxStock maximum stock for each offer, inclusive
   * @return independent shop service for one encounter
   */
  public synchronized ShopService createShop(int offerCount, int minStock, int maxStock) {
    validateSettings(offerCount, minStock, maxStock);

    List<CardConfig> candidates = getValidCandidates();
    shuffle(candidates);
    int selectedCount = Math.min(offerCount, candidates.size());
    ShopItem[] items = new ShopItem[selectedCount];
    for (int index = 0; index < selectedCount; index++) {
      CardConfig card = candidates.get(index);
      items[index] =
          new ShopItem(
              "shop_" + card.id,
              card.id,
              card.name,
              card.description,
              generatePrice(card.rarity),
              randomBetween(minStock, maxStock));
    }
    return new ShopService(items);
  }

  private List<CardConfig> getValidCandidates() {
    List<CardConfig> candidates = new ArrayList<>();
    List<CardConfig> cards = cardService.getAllCards();
    if (cards == null) {
      return candidates;
    }

    for (CardConfig card : cards) {
      if (card != null
          && card.id != null
          && !card.id.isBlank()
          && card.name != null
          && !card.name.isBlank()
          && card.rarity != null
          && cardService.getCard(card.id).isPresent()) {
        candidates.add(card);
      }
    }
    candidates.sort(Comparator.comparing(card -> card.id));
    return candidates;
  }

  private void shuffle(List<CardConfig> cards) {
    for (int index = cards.size() - 1; index > 0; index--) {
      int other = random.nextInt(index + 1);
      CardConfig card = cards.get(index);
      cards.set(index, cards.get(other));
      cards.set(other, card);
    }
  }

  private int generatePrice(Rarity rarity) {
    int basePrice;
    switch (rarity) {
      case RARE:
        basePrice = RARE_PRICE;
        break;
      case UNCOMMON:
        basePrice = UNCOMMON_PRICE;
        break;
      case COMMON:
      default:
        basePrice = COMMON_PRICE;
        break;
    }
    return Math.max(0, basePrice + randomBetween(-PRICE_VARIANCE, PRICE_VARIANCE));
  }

  private int randomBetween(int minimum, int maximum) {
    return minimum + random.nextInt(maximum - minimum + 1);
  }

  private static void validateSettings(int offerCount, int minStock, int maxStock) {
    if (offerCount <= 0) {
      throw new IllegalArgumentException("offerCount must be positive");
    }
    if (minStock <= 0) {
      throw new IllegalArgumentException("minStock must be positive");
    }
    if (maxStock < minStock) {
      throw new IllegalArgumentException("maxStock must be at least minStock");
    }
  }
}
