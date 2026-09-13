package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import org.junit.jupiter.api.Test;

class ShopServiceIntegrationTest {
  @Test
  void shouldKeepStockAndPlayerStateWhenCardLibraryRejectsOffer() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(player, new MockCardCatalogGateway(), deck);
    ShopService shop =
        new ShopService(
            new ShopItem[] {new ShopItem("unknown-offer", "missing-card", "Unknown", 20, 1)});

    PurchaseResult result = shop.purchaseWithGateway("unknown-offer", transactions);

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.CARD_NOT_FOUND, result.getStatus());
    assertEquals(50, player.getCurrency());
    assertEquals(0, deck.getCardIds().size());
    assertEquals(1, shop.getItem("unknown-offer").stock);
  }
}
