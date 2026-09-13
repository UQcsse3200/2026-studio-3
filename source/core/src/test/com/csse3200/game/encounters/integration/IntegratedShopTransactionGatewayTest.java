package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import org.junit.jupiter.api.Test;

class IntegratedShopTransactionGatewayTest {
  @Test
  void shouldAddCardAndDeductCurrency() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.SUCCESS, result);
    assertEquals(30, player.getCurrency());
    assertEquals(1, deck.getCardCount("card_heal"));
  }

  @Test
  void shouldRejectUnknownCardWithoutChargingPlayer() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("missing", 20);

    assertEquals(ShopTransactionStatus.CARD_NOT_FOUND, result);
    assertEquals(50, player.getCurrency());
    assertEquals(0, deck.getCardIds().size());
  }

  @Test
  void shouldRejectInsufficientCurrencyWithoutAddingCard() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 10);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.INSUFFICIENT_CURRENCY, result);
    assertEquals(10, player.getCurrency());
    assertEquals(0, deck.getCardIds().size());
  }

  @Test
  void shouldNotChargePlayerWhenDeckRejectsCard() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    deck.setFailAdd(true);
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.CARD_ADD_FAILED, result);
    assertEquals(50, player.getCurrency());
  }

  @Test
  void shouldRemoveAddedCardWhenCurrencyUpdateFails() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    player.failNextCurrencyUpdate();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.CURRENCY_UPDATE_FAILED, result);
    assertEquals(50, player.getCurrency());
    assertEquals(0, deck.getCardIds().size());
  }

  @Test
  void shouldReportRollbackFailure() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    deck.setFailRemove(true);
    player.failNextCurrencyUpdate();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.ROLLBACK_FAILED, result);
    assertEquals(1, deck.getCardCount("card_heal"));
  }

  private IntegratedShopTransactionGateway createGateway(
      MockPlayerStateGateway player, MockDeckGateway deck) {
    return new IntegratedShopTransactionGateway(
        player, new MockCardCatalogGateway("card_heal"), deck);
  }
}
