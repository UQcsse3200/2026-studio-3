package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.shop.ShopDisplay.ShopItemState;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopDisplayTest {
  private final ShopItem item = new ShopItem("offer", "card", "Test Card", 20, 1);

  @Test
  void shouldShowAvailableWhenPurchaseCheckSucceeds() {
    assertEquals(
        ShopItemState.AVAILABLE, ShopDisplay.getItemState(PurchaseResult.available(item), false));
  }

  @Test
  void shouldShowUnaffordableWhenGoldIsInsufficient() {
    assertEquals(
        ShopItemState.UNAFFORDABLE,
        ShopDisplay.getItemState(
            PurchaseResult.failure(PurchaseResult.Status.INSUFFICIENT_GOLD, item), false));
  }

  @Test
  void shouldShowSoldAfterPurchase() {
    assertEquals(
        ShopItemState.SOLD, ShopDisplay.getItemState(PurchaseResult.available(item), true));
  }

  @Test
  void shouldShowSoldWhenStockIsEmpty() {
    assertEquals(
        ShopItemState.SOLD,
        ShopDisplay.getItemState(
            PurchaseResult.failure(PurchaseResult.Status.OUT_OF_STOCK, item), false));
  }

  @Test
  void shouldShowUnavailableForOtherFailures() {
    assertEquals(
        ShopItemState.UNAVAILABLE,
        ShopDisplay.getItemState(
            PurchaseResult.failure(PurchaseResult.Status.INVALID_ITEM, item), false));
  }
}
