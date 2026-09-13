package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopServiceTest {
  @Test
  void shouldPurchaseAvailableItem() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 2)});

    PurchaseResult result = shop.purchase("heal", inventory);

    assertTrue(result.isSuccess());
    assertEquals(PurchaseResult.Status.SUCCESS, result.getStatus());
    assertEquals(30, inventory.getGold());
    assertEquals(1, inventory.getCardCount("card_heal"));
    assertEquals(1, shop.getItem("heal").stock);
  }

  @Test
  void shouldCheckPurchaseWithoutChangingInventoryOrStock() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)});

    PurchaseResult result = shop.canPurchase("heal", inventory);

    assertTrue(result.isSuccess());
    assertEquals("Item can be purchased.", result.getMessage());
    assertEquals(50, inventory.getGold());
    assertEquals(0, inventory.getCardCount("card_heal"));
    assertEquals(1, shop.getItem("heal").stock);
  }

  @Test
  void shouldConsumeStockAcrossMultiplePurchases() {
    InventoryComponent inventory = new InventoryComponent(45);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("strike", "card_strike", "Strike", 15, 2)});

    assertTrue(shop.purchase("strike", inventory).isSuccess());
    assertTrue(shop.purchase("strike", inventory).isSuccess());
    PurchaseResult thirdPurchase = shop.purchase("strike", inventory);

    assertFalse(thirdPurchase.isSuccess());
    assertEquals(PurchaseResult.Status.OUT_OF_STOCK, thirdPurchase.getStatus());
    assertEquals(15, inventory.getGold());
    assertEquals(2, inventory.getCardCount("card_strike"));
    assertEquals(0, shop.getItem("strike").stock);
  }

  @Test
  void shouldRejectUnknownItemWithoutChangingInventory() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 2)});

    PurchaseResult result = shop.purchase("missing", inventory);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.ITEM_NOT_FOUND, result.getStatus());
    assertEquals(50, inventory.getGold());
    assertEquals(0, inventory.getCardCount("card_heal"));
    assertEquals(2, shop.getItem("heal").stock);
  }

  @Test
  void shouldRejectOutOfStockItemWithoutChangingInventory() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("shield", "card_shield", "Shield", 35, 0)});

    PurchaseResult result = shop.purchase("shield", inventory);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.OUT_OF_STOCK, result.getStatus());
    assertEquals(50, inventory.getGold());
    assertEquals(0, inventory.getCardCount("card_shield"));
    assertEquals(0, shop.getItem("shield").stock);
  }

  @Test
  void shouldRejectUnaffordableItemWithoutChangingInventoryOrStock() {
    InventoryComponent inventory = new InventoryComponent(10);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 2)});

    PurchaseResult result = shop.purchase("heal", inventory);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.INSUFFICIENT_GOLD, result.getStatus());
    assertEquals(10, inventory.getGold());
    assertEquals(0, inventory.getCardCount("card_heal"));
    assertEquals(2, shop.getItem("heal").stock);
  }

  @Test
  void shouldRejectInvalidItemWithoutChangingInventoryOrStock() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("broken", "", "Broken", 20, 1)});

    PurchaseResult result = shop.purchase("broken", inventory);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.INVALID_ITEM, result.getStatus());
    assertEquals(50, inventory.getGold());
    assertEquals(0, inventory.getCards().size());
    assertEquals(1, shop.getItem("broken").stock);
  }

  @Test
  void shouldRejectNullInventoryWithoutChangingStock() {
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)});

    PurchaseResult result = shop.purchase("heal", null);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.INVALID_INVENTORY, result.getStatus());
    assertEquals(1, shop.getItem("heal").stock);
  }

  @Test
  void shouldKeepFirstDuplicateItemIdAndReportValidationError() {
    ShopService shop =
        new ShopService(
            new ShopItem[] {
              new ShopItem("heal", "card_heal", "Heal", 20, 1),
              new ShopItem("heal", "card_duplicate", "Duplicate", 1, 1)
            });

    assertEquals("card_heal", shop.getItem("heal").cardId);
    assertFalse(shop.getValidationErrors().isEmpty());
  }

  @Test
  void shouldExposeReadOnlyShopDataViews() {
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)});

    assertThrows(UnsupportedOperationException.class, () -> shop.getItems().clear());
    assertThrows(
        UnsupportedOperationException.class, () -> shop.getValidationErrors().add("mutate"));
  }

  @Test
  void shouldRestockExistingItemsOnlyWithPositiveQuantity() {
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 0)});

    assertFalse(shop.isItemPurchasable("heal"));
    assertTrue(shop.restockItem("heal", 2));
    assertTrue(shop.isItemPurchasable("heal"));
    assertEquals(2, shop.getItem("heal").stock);

    assertFalse(shop.restockItem("heal", 0));
    assertFalse(shop.restockItem("missing", 1));
    assertEquals(2, shop.getItem("heal").stock);
  }

  @Test
  void shouldSupportConfigConstructors() {
    ShopConfig config = new ShopConfig();
    config.items = new ShopItem[] {new ShopItem("strike", "card_strike", "Strike", 15, 1)};

    ShopService shop = new ShopService(config);

    assertEquals("card_strike", shop.getItem("strike").cardId);
  }
}
