package com.csse3200.game.shop;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.encounters.integration.InventoryShopTransactionAdapter;
import com.csse3200.game.encounters.integration.ShopTransactionGateway;
import com.csse3200.game.encounters.integration.ShopTransactionStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handles shop inventory lookup and transaction-safe purchase flow. */
public class ShopService {
  private final Map<String, ShopItem> items = new LinkedHashMap<>();
  private final List<String> validationErrors = new ArrayList<>();

  public ShopService(ShopConfig config) {
    this(config == null ? new ShopItem[0] : config.getItems());
  }

  public ShopService(ShopItem[] shopItems) {
    if (shopItems == null) {
      return;
    }

    for (ShopItem item : shopItems) {
      if (item == null) {
        validationErrors.add("Shop item entry is null.");
        continue;
      }
      if (item.id == null || item.id.isBlank()) {
        validationErrors.add("Shop item is missing an id.");
        continue;
      }
      if (items.containsKey(item.id)) {
        validationErrors.add(String.format("Duplicate shop item id ignored: %s.", item.id));
        continue;
      }
      if (!item.isValid()) {
        validationErrors.add(String.format("Invalid shop item data: %s.", item.id));
      }

      items.put(item.id, item);
    }
  }

  /**
   * Attempts to purchase a shop item. The player's inventory is updated only after item, stock, and
   * gold checks pass.
   *
   * @param itemId shop item identifier
   * @param inventory player inventory
   * @return purchase result with success or failure reason
   */
  public PurchaseResult purchase(String itemId, InventoryComponent inventory) {
    ShopTransactionGateway transactions =
        inventory == null ? null : new InventoryShopTransactionAdapter(inventory);
    return purchaseWithGateway(itemId, transactions);
  }

  /**
   * Attempts to purchase an item through the cross-team transaction boundary.
   *
   * @param itemId shop item identifier
   * @param transactions Player/Card/Deck transaction boundary
   * @return purchase result with success or failure reason
   */
  public PurchaseResult purchaseWithGateway(String itemId, ShopTransactionGateway transactions) {
    PurchaseResult check = canPurchaseWithGateway(itemId, transactions);
    if (!check.isSuccess()) {
      return check;
    }

    ShopItem item = check.getItem();
    ShopTransactionStatus transaction = transactions.purchaseCard(item.cardId, item.price);
    if (transaction != ShopTransactionStatus.SUCCESS) {
      return fromTransactionStatus(transaction, item);
    }

    item.decreaseStock();
    return PurchaseResult.success(item);
  }

  /**
   * Checks whether a purchase can be made without changing gold, cards, or stock.
   *
   * @param itemId shop item identifier
   * @param inventory player inventory
   * @return success if the item is purchasable with the current inventory
   */
  public PurchaseResult canPurchase(String itemId, InventoryComponent inventory) {
    ShopTransactionGateway transactions =
        inventory == null ? null : new InventoryShopTransactionAdapter(inventory);
    return canPurchaseWithGateway(itemId, transactions);
  }

  /**
   * Checks a purchase through the Player/Card/Deck boundary without changing any state.
   *
   * @param itemId shop item identifier
   * @param transactions Player/Card/Deck transaction boundary
   * @return success when the item and all dependent systems accept the purchase
   */
  public PurchaseResult canPurchaseWithGateway(String itemId, ShopTransactionGateway transactions) {
    ShopItem item = items.get(itemId);
    if (item == null) {
      return PurchaseResult.failure(PurchaseResult.Status.ITEM_NOT_FOUND, null);
    }
    if (!item.isValid()) {
      return PurchaseResult.failure(PurchaseResult.Status.INVALID_ITEM, item);
    }
    if (!item.hasStock()) {
      return PurchaseResult.failure(PurchaseResult.Status.OUT_OF_STOCK, item);
    }
    if (transactions == null) {
      return PurchaseResult.failure(PurchaseResult.Status.INVALID_INVENTORY, item);
    }

    ShopTransactionStatus validation = transactions.validatePurchase(item.cardId, item.price);
    if (validation != ShopTransactionStatus.READY) {
      return fromTransactionStatus(validation, item);
    }

    return PurchaseResult.available(item);
  }

  public boolean containsItem(String itemId) {
    return items.containsKey(itemId);
  }

  public boolean isItemPurchasable(String itemId) {
    ShopItem item = items.get(itemId);
    return item != null && item.isPurchasable();
  }

  public boolean restockItem(String itemId, int quantity) {
    ShopItem item = items.get(itemId);
    return item != null && item.increaseStock(quantity);
  }

  public ShopItem getItem(String itemId) {
    return items.get(itemId);
  }

  public Collection<ShopItem> getItems() {
    return Collections.unmodifiableCollection(items.values());
  }

  public List<String> getValidationErrors() {
    return Collections.unmodifiableList(validationErrors);
  }

  private PurchaseResult fromTransactionStatus(ShopTransactionStatus transaction, ShopItem item) {
    if (transaction == ShopTransactionStatus.INSUFFICIENT_CURRENCY) {
      return PurchaseResult.failure(PurchaseResult.Status.INSUFFICIENT_GOLD, item);
    }
    if (transaction == ShopTransactionStatus.CARD_NOT_FOUND) {
      return PurchaseResult.failure(PurchaseResult.Status.CARD_NOT_FOUND, item);
    }
    if (transaction == ShopTransactionStatus.INVALID_CARD
        || transaction == ShopTransactionStatus.INVALID_PRICE) {
      return PurchaseResult.failure(PurchaseResult.Status.INVALID_ITEM, item);
    }
    return PurchaseResult.failure(PurchaseResult.Status.TRANSACTION_FAILED, item);
  }
}
