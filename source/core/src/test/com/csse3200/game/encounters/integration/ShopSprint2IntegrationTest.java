package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopInventoryGenerator;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopSprint2IntegrationTest {
  @Test
  void shouldPurchaseGeneratedCardThroughProductionCardAndDeckApis() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    PlayerDeck playerDeck = new PlayerDeck(List.of("strike", "defend"));
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 1_000);
    ShopService shop = new ShopInventoryGenerator(cardService, new Random(19)).createShop(3, 1, 1);
    ShopItem offer = shop.getItems().iterator().next();
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new CardServiceCatalogAdapter(cardService), new PlayerDeckAdapter(playerDeck));

    PurchaseResult result = shop.purchaseWithGateway(offer.id, transactions);

    assertTrue(result.isSuccess());
    assertEquals(1_000 - offer.price, player.getCurrency());
    assertEquals(3, playerDeck.size());
    assertTrue(playerDeck.contains(offer.cardId));
    assertEquals(0, offer.stock);
  }

  @Test
  void shouldPreserveMoneyDeckOrderAndStockWhenCurrencyCommitFails() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    PlayerDeck playerDeck = new PlayerDeck(List.of("strike", "defend"));
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    player.failNextCurrencyUpdate();
    ShopItem offer = new ShopItem("strike-offer", "strike", "Strike", 20, 1);
    ShopService shop = new ShopService(new ShopItem[] {offer});
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new CardServiceCatalogAdapter(cardService), new PlayerDeckAdapter(playerDeck));

    PurchaseResult result = shop.purchaseWithGateway(offer.id, transactions);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.TRANSACTION_FAILED, result.getStatus());
    assertEquals(50, player.getCurrency());
    assertEquals(List.of("strike", "defend"), playerDeck.getCardIds());
    assertEquals(1, offer.stock);
  }

  @Test
  void shouldRejectCardThatCardServiceKnowsButPlayerDeckCannotAccept() {
    PlayerDeck playerDeck = new PlayerDeck();
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    ShopItem offer = new ShopItem("future-offer", "future-card", "Future", 20, 1);
    ShopService shop = new ShopService(new ShopItem[] {offer});
    CardCatalogGateway futureCatalog = cardId -> "future-card".equals(cardId);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, futureCatalog, new PlayerDeckAdapter(playerDeck));

    PurchaseResult result = shop.purchaseWithGateway(offer.id, transactions);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.TRANSACTION_FAILED, result.getStatus());
    assertEquals(50, player.getCurrency());
    assertTrue(playerDeck.isEmpty());
    assertEquals(1, offer.stock);
  }
}
