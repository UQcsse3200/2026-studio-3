package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component intended to be used by the player to track their inventory.
 *
 * <p>Tracks the player's gold and card quantities. Can also be used as a more generic component for
 * other entities.
 */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);
  private int gold;
  private final Map<String, Integer> cards = new HashMap<>();

  public InventoryComponent(int gold) {
    setGold(gold);
  }

  /**
   * Returns the player's gold.
   *
   * @return entity's gold
   */
  public int getGold() {
    return this.gold;
  }

  /**
   * Returns if the player has a certain amount of gold.
   *
   * @param gold required amount of gold
   * @return player has greater than or equal to the required amount of gold
   */
  public boolean hasGold(int gold) {
    return gold >= 0 && this.gold >= gold;
  }

  /**
   * Sets the player's gold. Gold has a minimum bound of 0.
   *
   * @param gold gold
   */
  public void setGold(int gold) {
    this.gold = Math.max(gold, 0);
    logger.debug("Setting gold to {}", this.gold);

    if (entity != null) {
      entity.getEvents().trigger("updateGold", this.gold);
    }
  }

  /**
   * Adds to the player's gold. The amount added can be negative.
   *
   * @param gold gold to add
   */
  public void addGold(int gold) {
    setGold(this.gold + gold);
  }

  public boolean spendGold(int gold) {
    if (!hasGold(gold)) {
      return false;
    }

    addGold(-gold);
    return true;
  }

  /**
   * Adds one card to the inventory.
   *
   * @param cardId stable card identifier
   * @return true if the card was added
   */
  public boolean addCard(String cardId) {
    return addCard(cardId, 1);
  }

  /**
   * Adds a quantity of a card to the inventory.
   *
   * @param cardId stable card identifier
   * @param quantity number of cards to add
   * @return true if the cards were added
   */
  public boolean addCard(String cardId, int quantity) {
    if (!isValidCardId(cardId) || quantity <= 0) {
      logger.warn("Ignoring invalid card addition: cardId={}, quantity={}", cardId, quantity);
      return false;
    }

    cards.merge(cardId, quantity, Integer::sum);
    logger.debug("Added {} of card {}. New quantity: {}", quantity, cardId, cards.get(cardId));
    return true;
  }

  /**
   * Returns how many copies of a card are in the inventory.
   *
   * @param cardId stable card identifier
   * @return card quantity, or 0 when not owned
   */
  public int getCardCount(String cardId) {
    return cards.getOrDefault(cardId, 0);
  }

  /**
   * Returns if the inventory contains at least one copy of a card.
   *
   * @param cardId stable card identifier
   * @return true if the card is owned
   */
  public boolean hasCard(String cardId) {
    return getCardCount(cardId) > 0;
  }

  /**
   * Removes one copy of a card from the temporary Sprint 1 collection.
   *
   * <p>This is primarily used to roll back a shop purchase if the matching currency update fails.
   * Team 5's persistent deck will replace this temporary storage after cross-team integration.
   *
   * @param cardId stable card identifier
   * @return true if one copy was removed
   */
  public boolean removeCard(String cardId) {
    int count = getCardCount(cardId);
    if (count <= 0) {
      return false;
    }

    if (count == 1) {
      cards.remove(cardId);
    } else {
      cards.put(cardId, count - 1);
    }
    return true;
  }

  /**
   * Returns an immutable view of owned card quantities.
   *
   * @return card quantities by card id
   */
  public Map<String, Integer> getCards() {
    return Collections.unmodifiableMap(cards);
  }

  /**
   * Attempts to buy a card atomically from the inventory perspective. Gold is only deducted if the
   * card can also be added.
   *
   * @param cardId stable card identifier
   * @param price card price in gold
   * @return true if the card was added and gold was deducted
   */
  public boolean purchaseCard(String cardId, int price) {
    if (price < 0 || !hasGold(price)) {
      return false;
    }
    if (!addCard(cardId)) {
      return false;
    }

    spendGold(price);
    return true;
  }

  private boolean isValidCardId(String cardId) {
    return cardId != null && !cardId.isBlank();
  }

  /**
   * Subtracts a specific amount of gold from the player's inventory. Checks if the player has
   * enough gold before subtracting.
   *
   * @param amount the amount of gold to subtract
   * @return true if successful, false if not enough gold or invalid amount
   */
  public boolean subtractGold(int amount) {
    if (amount < 0) {
      return false;
    }

    if (!hasGold(amount)) {
      return false;
    }

    setGold(this.gold - amount);
    return true;
  }
}
