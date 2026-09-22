package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.shop.ShopDisplay.ShopItemState;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopItem;
import java.util.Optional;
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

  @Test
  void shouldResolveArtworkFromTheSharedCardService() {
    CardService cardService = mock(CardService.class);
    CardConfig card = new CardConfig();
    card.name = "Battle Card";
    card.description = "Deal 8 damage.";
    card.cost = 2;
    card.texturePath = "images/cards/card.png";
    when(cardService.getCard("card")).thenReturn(Optional.of(card));

    ShopDisplay display = new ShopDisplay(null, cardService);

    assertEquals("images/cards/card.png", display.resolveArtworkPath(item));
    assertEquals("Battle Card", display.resolveCardName(item));
    assertEquals("Deal 8 damage.", display.resolveCardDescription(item));
    assertEquals("2 ENERGY", display.resolveEnergyText(item));
    assertNull(display.resolveArtworkPath(null));
  }

  @Test
  void shouldFallBackToShopTextWhenCardDefinitionIsMissing() {
    CardService cardService = mock(CardService.class);
    when(cardService.getCard("card")).thenReturn(Optional.empty());
    ShopItem describedItem =
        new ShopItem("offer", "card", "Fallback Card", "Fallback description.", 20, 1);

    ShopDisplay display = new ShopDisplay(null, cardService);

    assertEquals("Fallback Card", display.resolveCardName(describedItem));
    assertEquals("Fallback description.", display.resolveCardDescription(describedItem));
    assertEquals("-- ENERGY", display.resolveEnergyText(describedItem));
  }
}
