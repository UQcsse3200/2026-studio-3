package com.csse3200.game.encounters.integration;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;

/** Preserves the existing Sprint 1 mock-inventory purchase path behind the new shop boundary. */
public final class InventoryShopTransactionAdapter implements ShopTransactionGateway {
  private final InventoryComponent inventory;

  /**
   * Creates a legacy transaction adapter for the Sprint 1 inventory.
   *
   * @param inventory temporary Sprint 1 player inventory
   */
  public InventoryShopTransactionAdapter(InventoryComponent inventory) {
    this.inventory = Objects.requireNonNull(inventory, "inventory cannot be null");
  }

  @Override
  public int getCurrency() {
    return inventory.getGold();
  }

  @Override
  public ShopTransactionStatus validatePurchase(String cardId, int price) {
    if (cardId == null || cardId.isBlank()) {
      return ShopTransactionStatus.INVALID_CARD;
    }
    if (price < 0) {
      return ShopTransactionStatus.INVALID_PRICE;
    }
    if (!inventory.hasGold(price)) {
      return ShopTransactionStatus.INSUFFICIENT_CURRENCY;
    }
    return ShopTransactionStatus.READY;
  }

  @Override
  public ShopTransactionStatus purchaseCard(String cardId, int price) {
    ShopTransactionStatus validation = validatePurchase(cardId, price);
    if (validation != ShopTransactionStatus.READY) {
      return validation;
    }
    return inventory.purchaseCard(cardId, price)
        ? ShopTransactionStatus.SUCCESS
        : ShopTransactionStatus.TRANSACTION_FAILED;
  }
}
