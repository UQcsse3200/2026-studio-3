package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopInventoryGenerator;
import com.csse3200.game.shop.ShopRefreshPolicy;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopRefreshIntegrationTest {
  @Test
  void realCardDeckAndPlayerApisRetainPurchasedCardsItemsAndGoldAfterRefresh() {
    CardLibrary cards = new CardLibrary(CardConfigLoader.loadCards());
    RunState run = new RunState();
    var persistent = run.getOrCreatePlayerState();
    persistent.restore(100, 100, 1000);
    persistent.addOwnedItem(ItemType.MERCHANTS_FAVOR);
    Entity player = newPlayer();
    persistent.applyTo(player);
    PlayerDeck deck = run.getOrCreatePlayerDeck(cards);
    int deckSize = deck.size();
    var transactions =
        new IntegratedShopTransactionGateway(
            new ComponentPlayerStateAdapter(
                player.getComponent(CombatStatsComponent.class),
                player.getComponent(InventoryComponent.class)),
            new CardServiceCatalogAdapter(cards),
            new PlayerDeckAdapter(deck));
    ShopInventoryGenerator generator = new ShopInventoryGenerator(cards, new Random(439));
    ShopEncounter shop = new ShopEncounter(7, generator.createShop(), transactions, null);
    shop.enableRefresh(ShopRefreshPolicy.standard(), generator::createShop);
    var initial = shop.snapshot();
    var initialOffer = initial.offers().getFirst();
    assertEquals(Math.round(initialOffer.basePrice() * 0.9f), initialOffer.price());
    var bought = shop.purchase(initialOffer.id(), initial.revision(), "buy");
    assertTrue(bought.result().isSuccess());
    assertTrue(shop.refresh(shop.snapshot().revision(), "free").isSuccess());
    assertTrue(shop.refresh(shop.snapshot().revision(), "paid").isSuccess());
    assertTrue(
        shop.snapshot().offers().stream().allMatch(offer -> offer.price() == offer.basePrice()));
    assertEquals(deckSize + 1, deck.size());
    persistent.captureFrom(player);
    Entity nextPlayer = newPlayer();
    persistent.applyTo(nextPlayer);
    assertEquals(
        1000 - bought.chargedGold() - 10,
        nextPlayer.getComponent(InventoryComponent.class).getGold());
    assertEquals(1, persistent.getOwnedItemCount(ItemType.MERCHANTS_FAVOR));
    assertEquals(
        0.1f, nextPlayer.getComponent(InventoryComponent.class).getShopDiscount(), 0.0001f);
    assertSame(deck, run.getOrCreatePlayerDeck(cards));
  }

  @Test
  void serviceFeeNeverGrantsACardOrAppliesThePlayerDiscountAgain() {
    var player = new MockPlayerStateGateway(100, 50);
    player.setShopDiscount(0.5f);
    var deck = new MockDeckGateway();
    var gateway =
        new IntegratedShopTransactionGateway(player, new MockCardCatalogGateway("strike"), deck);
    assertEquals(ShopTransactionStatus.SUCCESS, gateway.chargeServiceFee(10));
    assertEquals(40, player.getCurrency());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(ShopTransactionStatus.SUCCESS, gateway.chargeServiceFee(0));
    assertEquals(ShopTransactionStatus.INVALID_PRICE, gateway.chargeServiceFee(-1));
    assertEquals(ShopTransactionStatus.INSUFFICIENT_CURRENCY, gateway.chargeServiceFee(41));
    assertEquals(40, player.getCurrency());
  }

  @Test
  void serviceFeeRollsBackAndReportsBothRecoverableAndUnrecoverableFailures() {
    var player = new MockPlayerStateGateway(100, 50);
    var gateway =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("strike"), new MockDeckGateway());
    player.failNextCurrencyUpdate();
    assertEquals(ShopTransactionStatus.CURRENCY_UPDATE_FAILED, gateway.chargeServiceFee(10));
    assertEquals(50, player.getCurrency());
    player.failAllCurrencyUpdates();
    assertEquals(ShopTransactionStatus.ROLLBACK_FAILED, gateway.chargeServiceFee(10));
  }

  @Test
  void legacyInventoryAdapterSupportsUndiscountedRefreshFees() {
    InventoryComponent inventory = new InventoryComponent(50);
    inventory.addShopDiscount(0.5f);
    var gateway = new InventoryShopTransactionAdapter(inventory);
    assertEquals(ShopTransactionStatus.SUCCESS, gateway.chargeServiceFee(10));
    assertEquals(40, inventory.getGold());
    assertEquals(ShopTransactionStatus.INVALID_PRICE, gateway.chargeServiceFee(-1));
    assertEquals(ShopTransactionStatus.INSUFFICIENT_CURRENCY, gateway.chargeServiceFee(41));
    assertEquals(40, inventory.getGold());
  }

  private Entity newPlayer() {
    return new Entity()
        .addComponent(new CombatStatsComponent(100, 10, 100))
        .addComponent(new InventoryComponent(0));
  }
}
