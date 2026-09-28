package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopDisplay.ShopItemState;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import java.util.List;
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
    card.texturePath = "images/cards/card.png";
    when(cardService.getCard("card")).thenReturn(Optional.of(card));

    ShopDisplay display = new ShopDisplay(null, cardService);

    assertEquals("images/shop/cards/card.png", display.resolveArtworkPath(item));
    assertNull(display.resolveArtworkPath(null));
  }

  @Test
  void openingShopMarksOfferedCardsAsSeen() {
    CardConfig card = new CardConfig();
    card.id = item.cardId;
    CardDiscoveryService discovery = new CardDiscoveryService(List.of(card));
    ServiceLocator.registerCardDiscoveryService(discovery);

    RenderService renderService = new RenderService();
    Stage stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());

    Entity shop =
        new Entity()
            .addComponent(
                new ShopDisplay(
                    new InventoryComponent(100), new ShopService(new ShopItem[] {item})));
    try {
      shop.create();

      assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get(item.cardId));
    } finally {
      shop.dispose();
      stage.dispose();
    }
  }
}
