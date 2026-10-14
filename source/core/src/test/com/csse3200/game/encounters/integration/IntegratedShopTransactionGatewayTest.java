package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
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

  @Test
  void shouldChargeThePassedPriceWithoutDiscountingAgain() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    player.setShopDiscount(0.05f);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    // ShopService has already applied the discount, so the gateway receives 19 and must charge
    // exactly 19. A second 5% discount would charge round(19 * 0.95) = 18 and leave 32.
    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 19);

    assertEquals(ShopTransactionStatus.SUCCESS, result);
    assertEquals(31, player.getCurrency());
    assertEquals(1, deck.getCardCount("card_heal"));
  }

  @Test
  void shouldAllowPurchaseWhenPlayerCanAffordExactlyThePassedPrice() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 19);
    player.setShopDiscount(0.05f);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    // The passed price is 19 and the player has exactly 19. With a hidden second discount the
    // charge would be 18 and the player would keep 1.
    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 19);

    assertEquals(ShopTransactionStatus.SUCCESS, result);
    assertEquals(0, player.getCurrency());
  }

  @Test
  void shouldRejectInsufficientCurrencyEvenWithDiscountApplied() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 5);
    player.setShopDiscount(0.05f);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    // price 20, player has 5, so the purchase is rejected.
    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.INSUFFICIENT_CURRENCY, result);
    assertEquals(5, player.getCurrency());
    assertEquals(0, deck.getCardIds().size());
  }

  @Test
  void shouldNotDiscountWhenPlayerHasNoShopDiscount() {
    // MockPlayerStateGateway defaults shopDiscount to 0f, so no special setup needed —
    // this locks in that full price is charged by default.
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.SUCCESS, result);
    assertEquals(30, player.getCurrency());
  }

  @Test
  void shouldNotDiscountAgainWhenPlayerHasFiftyPercentOff() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    player.setShopDiscount(0.5f);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    // The gateway charges the price it is given, whatever the player's discount is.
    ShopTransactionStatus result = transactions.purchaseCard("card_heal", 20);

    assertEquals(ShopTransactionStatus.SUCCESS, result);
    assertEquals(30, player.getCurrency());
  }

  @Test
  void getShopDiscountShouldExposePlayersCurrentDiscount() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    player.setShopDiscount(0.1f);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);

    assertEquals(0.1f, transactions.getShopDiscount(), 0.0001f);
  }

  private IntegratedShopTransactionGateway createGateway(
      MockPlayerStateGateway player, MockDeckGateway deck) {
    return new IntegratedShopTransactionGateway(
        player, new MockCardCatalogGateway("card_heal"), deck);
  }

  @Test
  void shopServiceAppliesTheDiscountExactlyOnceThroughTheGateway() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    player.setShopDiscount(0.1f);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway transactions = createGateway(player, deck);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 49, 1)});

    PurchaseResult result = shop.purchaseWithGateway("heal", transactions);

    assertTrue(result.isSuccess());
    // 49 at 10% off is round(44.1) = 44, charged once. A double discount would charge
    // round(44 * 0.9) = 40 and leave 60.
    assertEquals(56, player.getCurrency());
    assertEquals(1, deck.getCardCount("card_heal"));
  }
}
