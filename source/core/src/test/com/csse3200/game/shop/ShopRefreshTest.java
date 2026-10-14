package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.encounters.integration.IntegratedShopTransactionGateway;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShopRefreshTest {
  private final MockPlayerStateGateway player = new MockPlayerStateGateway(100, 500);
  private final MockDeckGateway deck = new MockDeckGateway();
  private final IntegratedShopTransactionGateway gateway =
      new IntegratedShopTransactionGateway(
          player, new MockCardCatalogGateway("strike", "defend", "bandage"), deck);

  private ShopService stock(String prefix) {
    return new ShopService(
        new ShopItem[] {
          new ShopItem(prefix + "s", "strike", "Strike", 49, 2),
          new ShopItem(prefix + "d", "defend", "Defend", 20, 1),
          new ShopItem(prefix + "b", "bandage", "Bandage", 30, 1)
        });
  }

  private ShopEncounter shop() {
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(new ShopRefreshPolicy(10), () -> stock("new"));
    return shop;
  }

  @Test
  void invalidConfigurationDoesNotPartiallyEnableRefresh() {
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    assertThrows(
        NullPointerException.class, () -> shop.enableRefresh(new ShopRefreshPolicy(10), null));
    assertTrue(shop.getNextRefreshCost().isEmpty());
    shop.enableRefresh(new ShopRefreshPolicy(10), () -> stock("new"));
    assertTrue(shop.refresh(0, "free").isSuccess());
    assertEquals(ShopRefreshResult.Status.INVALID_REQUEST, shop.refresh(1, " ").status());
    assertEquals(
        PurchaseResult.Status.INVALID_REQUEST, shop.purchase("news", 1, null).result().getStatus());
  }

  @Test
  void rejectsUnregisteredCardsBeforeUsingTheFreeRefreshOrChargingGold() {
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(
        new ShopRefreshPolicy(10),
        () ->
            new ShopService(
                new ShopItem[] {
                  new ShopItem("a", "not-in-library", "Unknown", 20, 1),
                  new ShopItem("b", "defend", "Defend", 20, 1),
                  new ShopItem("c", "bandage", "Bandage", 30, 1)
                }));
    ShopSnapshot before = shop.snapshot();
    assertEquals(ShopRefreshResult.Status.GENERATION_FAILED, shop.refresh(0, "invalid").status());
    assertEquals(before, shop.snapshot());
    assertEquals(500, player.getCurrency());
  }

  @Test
  void replacesAllOffersForFreeThenChargesTenTwentyAndForty() {
    ShopEncounter shop = shop();
    for (int index = 0; index < 4; index++) {
      int expected = index == 0 ? 0 : 10 << (index - 1);
      ShopRefreshResult result = shop.refresh(shop.snapshot().revision(), "refresh" + index);
      assertTrue(result.isSuccess());
      assertEquals(expected, result.chargedGold());
      assertEquals(3, shop.snapshot().offers().size());
      assertTrue(shop.snapshot().offers().stream().allMatch(offer -> offer.id().startsWith("new")));
    }
    assertEquals(430, player.getCurrency());
    assertEquals(4, shop.snapshot().successfulRefreshes());
    assertEquals(80, shop.getNextRefreshCost().orElseThrow());
  }

  @Test
  void onlyInitialStockIsDiscountedWithoutRemovingPermanentPlayerDiscount() {
    player.setShopDiscount(0.1f);
    ShopEncounter shop = shop();
    assertEquals(44, shop.snapshot().offers().getFirst().price());
    assertTrue(shop.purchase("olds", 0, "buy-initial").result().isSuccess());
    assertEquals(456, player.getCurrency());
    shop.refresh(shop.snapshot().revision(), "free");
    assertEquals(0f, shop.getShopDiscount());
    assertEquals(0.1f, player.getShopDiscount());
    assertEquals(49, shop.snapshot().offers().getFirst().price());
    assertEquals(1, deck.getCardCount("strike"));
    ShopPurchaseReceipt receipt = shop.purchase("news", shop.snapshot().revision(), "buy-new");
    assertEquals(49, receipt.chargedGold());
    assertEquals(407, player.getCurrency());
    assertEquals(2, deck.getCardCount("strike"));
    assertEquals(1, receipt.acquisition().entries().getFirst().quantity());
  }

  @Test
  void refreshFeeIsNeverDiscounted() {
    player.setShopDiscount(0.5f);
    ShopEncounter shop = shop();
    shop.refresh(0, "free");
    assertEquals(10, shop.refresh(1, "paid").chargedGold());
    assertEquals(490, player.getCurrency());
  }

  @Test
  void refreshAndPurchaseReplaysReturnTheOriginalReceiptWithoutMutation() {
    ShopEncounter shop = shop();
    ShopRefreshResult refreshed = shop.refresh(0, "free");
    assertSame(refreshed, shop.refresh(0, "free"));
    ShopPurchaseReceipt purchased = shop.purchase("news", 1, "buy");
    assertSame(purchased, shop.purchase("news", 1, "buy"));
    assertEquals(451, player.getCurrency());
    assertEquals(1, deck.getCardCount("strike"));
    assertEquals(1, shop.snapshot().successfulRefreshes());
    assertEquals(1, shop.snapshot().offers().getFirst().stock());
    assertEquals(ShopRefreshResult.Status.INVALID_REQUEST, shop.refresh(2, "free").status());
    assertEquals(
        PurchaseResult.Status.INVALID_REQUEST,
        shop.purchase("newd", 1, "buy").result().getStatus());
    assertEquals(ShopRefreshResult.Status.INVALID_REQUEST, shop.refresh(2, "buy").status());
    assertEquals(
        PurchaseResult.Status.INVALID_REQUEST,
        shop.purchase("news", 2, "free").result().getStatus());
  }

  @Test
  void rejectsStaleStockEvenWhenNewStockReusesTheSameProductId() {
    ShopEncounter shop = new ShopEncounter(7, stock("same"), gateway, null);
    shop.enableRefresh(new ShopRefreshPolicy(10), () -> stock("same"));
    shop.refresh(0, "free");
    assertEquals(
        PurchaseResult.Status.STALE_STOCK,
        shop.purchase("sames", 0, "stale-buy").result().getStatus());
    assertEquals(ShopRefreshResult.Status.STALE_STOCK, shop.refresh(0, "stale-refresh").status());
    assertEquals(500, player.getCurrency());
    assertTrue(deck.getCardIds().isEmpty());
  }

  @Test
  void generationFailurePreservesOriginalStockFreeChanceAndDiscount() {
    player.setShopDiscount(0.2f);
    AtomicInteger attempts = new AtomicInteger();
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(
        new ShopRefreshPolicy(10),
        () -> {
          if (attempts.getAndIncrement() == 0) {
            throw new IllegalStateException("No catalogue");
          }
          return stock("new");
        });
    ShopSnapshot before = shop.snapshot();
    assertEquals(ShopRefreshResult.Status.GENERATION_FAILED, shop.refresh(0, "fail").status());
    assertEquals(before, shop.snapshot());
    assertEquals(500, player.getCurrency());
    assertTrue(shop.refresh(0, "retry").isSuccess());
    assertEquals(500, player.getCurrency());
  }

  @Test
  void rejectsEmptyInvalidAndDuplicateCardBatchesBeforeCharging() {
    List<ShopService> invalid =
        List.of(
            new ShopService(new ShopItem[0]),
            new ShopService(new ShopItem[] {new ShopItem("a", "strike", "Strike", 20, 1)}),
            new ShopService(
                new ShopItem[] {
                  new ShopItem("a", "strike", "Strike", 20, 1),
                  new ShopItem("b", "strike", "Strike", 20, 1),
                  new ShopItem("c", "bandage", "Bandage", 30, 1)
                }),
            new ShopService(
                new ShopItem[] {
                  new ShopItem("a", "strike", "Strike", -1, 1),
                  new ShopItem("b", "defend", "Defend", 20, 1),
                  new ShopItem("c", "bandage", "Bandage", 30, 1)
                }));
    for (ShopService generated : invalid) {
      ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
      shop.enableRefresh(new ShopRefreshPolicy(10), () -> generated);
      ShopSnapshot before = shop.snapshot();
      assertEquals(ShopRefreshResult.Status.GENERATION_FAILED, shop.refresh(0, "invalid").status());
      assertEquals(before, shop.snapshot());
    }
    assertEquals(500, player.getCurrency());
  }

  @Test
  void insufficientGoldAndPaymentFailureDoNotConsumeRefreshCountOrReplaceStock() {
    ShopEncounter shop = shop();
    shop.refresh(0, "free");
    player.setCurrency(5);
    ShopSnapshot before = shop.snapshot();
    assertEquals(ShopRefreshResult.Status.INSUFFICIENT_GOLD, shop.refresh(1, "poor").status());
    assertEquals(before, shop.snapshot());
    player.setCurrency(500);
    before = shop.snapshot();
    player.failNextCurrencyUpdate();
    assertEquals(ShopRefreshResult.Status.PAYMENT_FAILED, shop.refresh(1, "fail-pay").status());
    assertEquals(before, shop.snapshot());
    assertTrue(shop.refresh(1, "retry-pay").isSuccess());
    assertEquals(490, player.getCurrency());
  }

  @Test
  void failedRollbackBlocksFurtherTransactionsRatherThanSilentlyContinuing() {
    ShopEncounter shop = shop();
    shop.refresh(0, "free");
    player.failAllCurrencyUpdates();
    assertEquals(ShopRefreshResult.Status.ROLLBACK_FAILED, shop.refresh(1, "fail-pay").status());
    assertEquals(ShopRefreshResult.Status.BUSY, shop.refresh(1, "retry").status());
    assertEquals(PurchaseResult.Status.BUSY, shop.purchase("news").getStatus());
    assertEquals(1, shop.snapshot().successfulRefreshes());
  }

  @Test
  void confirmingFeeDoesNotRestoreTheFreeRefreshAndInvalidatesOldQuotes() {
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(ShopRefreshPolicy.pendingFee(), () -> stock("new"));
    assertTrue(shop.refresh(0, "free").isSuccess());
    assertEquals(ShopRefreshResult.Status.NOT_CONFIGURED, shop.refresh(1, "pending").status());
    shop.setFirstPaidRefreshFee(15);
    assertEquals(15, shop.getNextRefreshCost().orElseThrow());
    assertEquals(ShopRefreshResult.Status.STALE_STOCK, shop.refresh(1, "old-quote").status());
    assertEquals(15, shop.refresh(2, "paid").chargedGold());
    assertThrows(IllegalStateException.class, () -> shop.setFirstPaidRefreshFee(20));
  }

  @Test
  void feesThatCannotBeRepresentedNeverWrapAround() {
    player.setCurrency(Integer.MAX_VALUE);
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(new ShopRefreshPolicy(Integer.MAX_VALUE), () -> stock("new"));
    shop.refresh(0, "free");
    shop.refresh(1, "max");
    assertEquals(ShopRefreshResult.Status.COST_OVERFLOW, shop.refresh(2, "overflow").status());
    assertEquals(0, player.getCurrency());
    assertEquals(2, shop.snapshot().successfulRefreshes());
  }

  @Test
  void snapshotsAndGeneratedStockAreDetachedFromLaterExternalMutations() {
    ShopService generated = stock("new");
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(new ShopRefreshPolicy(10), () -> generated);
    shop.refresh(0, "free");
    ShopSnapshot snapshot = shop.snapshot();
    generated.getItem("news").price = 999;
    generated.getItem("news").stock = 999;
    assertEquals(49, shop.snapshot().offers().getFirst().price());
    shop.purchase("news", 1, "buy");
    assertEquals(2, snapshot.offers().getFirst().stock());
    assertThrows(UnsupportedOperationException.class, () -> snapshot.offers().clear());
  }

  @Test
  void closedShopsDoNotRegenerateChargeOrReplayAcquisitions() {
    ShopEncounter shop = shop();
    shop.complete(true);
    assertEquals(ShopRefreshResult.Status.SHOP_CLOSED, shop.refresh(0, "free").status());
    assertEquals(
        PurchaseResult.Status.SHOP_CLOSED, shop.purchase("olds", 0, "buy").result().getStatus());
    assertTrue(shop.getNextRefreshCost().isEmpty());
    assertEquals(500, player.getCurrency());
  }

  @Test
  void rejectsReentrantActionsWhileGenerationIsRunning() {
    ShopEncounter shop = new ShopEncounter(7, stock("old"), gateway, null);
    shop.enableRefresh(
        new ShopRefreshPolicy(10),
        () -> {
          assertEquals(ShopRefreshResult.Status.BUSY, shop.refresh(0, "nested").status());
          assertEquals(PurchaseResult.Status.BUSY, shop.purchase("olds").getStatus());
          return stock("new");
        });
    assertTrue(shop.refresh(0, "free").isSuccess());
    assertEquals(1, shop.snapshot().successfulRefreshes());
  }

  @Test
  void simultaneousRequestsForTheSameRevisionCannotRefreshTwice() throws Exception {
    ShopEncounter shop = shop();
    CountDownLatch start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                start.await();
                return shop.refresh(0, "first");
              });
      var second =
          workers.submit(
              () -> {
                start.await();
                return shop.refresh(0, "second");
              });
      start.countDown();
      var results = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
      assertEquals(1, results.stream().filter(ShopRefreshResult::isSuccess).count());
      assertEquals(1, shop.snapshot().successfulRefreshes());
      assertEquals(500, player.getCurrency());
    }
  }
}
