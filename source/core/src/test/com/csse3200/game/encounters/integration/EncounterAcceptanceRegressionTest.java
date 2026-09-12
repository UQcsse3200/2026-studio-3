package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** End-to-end regression coverage for the final Sprint 2 encounter integration boundaries. */
@ExtendWith(GameExtension.class)
class EncounterAcceptanceRegressionTest {
  @Test
  void shouldCompleteChanceAndShopRoundTripExactlyOnceUsingProductionAdapters() {
    MapGraph map = createLinearMap();
    RecordingMapCallback mapCallback = new RecordingMapCallback(map);
    CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
    InventoryComponent inventory = new InventoryComponent(100);
    ComponentPlayerStateAdapter player =
        new ComponentPlayerStateAdapter(combatStats, inventory);
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    PlayerDeck playerDeck = new PlayerDeck(cardService, List.of("strike", "defend"));
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new CardServiceCatalogAdapter(cardService), new PlayerDeckAdapter(playerDeck));
    EncounterFlowController flow = new EncounterFlowController(player, transactions, mapCallback);

    ChanceEncounterSession chance = flow.startChance(1, createChanceEncounter());
    ChanceResolution chanceResult = chance.resolveChoice("risk");

    assertTrue(chanceResult.isSuccess());
    assertTrue(chance.complete());
    assertFalse(chance.complete());
    flow.onEncounterComplete(1, true);

    assertEquals(90, combatStats.getHealth());
    assertEquals(125, inventory.getGold());
    assertEquals(1, mapCallback.completionCount);
    assertEquals(NodeState.COMPLETED, map.getNode(1).getState());
    assertEquals(NodeState.AVAILABLE, map.getNode(2).getState());
    assertTrue(map.moveToNode(2));

    ShopItem offer = new ShopItem("strike-offer", "strike", "Strike", 20, 1);
    ShopEncounter shop = flow.startShop(2, new ShopService(new ShopItem[] {offer}));
    PurchaseResult purchase = shop.purchase("strike-offer");

    assertTrue(purchase.isSuccess());
    shop.complete(true);
    shop.complete(false);
    flow.onEncounterComplete(2, true);

    assertEquals(105, inventory.getGold());
    assertEquals(List.of("strike", "defend", "strike"), playerDeck.getCardIds());
    assertEquals(0, offer.stock);
    assertEquals(2, mapCallback.completionCount);
    assertEquals(NodeState.COMPLETED, map.getNode(2).getState());
    assertEquals(NodeState.AVAILABLE, map.getNode(3).getState());
    assertFalse(flow.isEncounterActive());

    PurchaseResult latePurchase = shop.purchase("strike-offer");
    assertEquals(PurchaseResult.Status.SHOP_CLOSED, latePurchase.getStatus());
    assertEquals(105, inventory.getGold());
    assertEquals(3, playerDeck.size());
  }

  @Test
  void failedPurchaseShouldNotChangeProductionMoneyDeckStockOrMapProgression() {
    MapGraph map = createFailedShopMap();
    RecordingMapCallback mapCallback = new RecordingMapCallback(map);
    CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
    InventoryComponent inventory = new InventoryComponent(10);
    ComponentPlayerStateAdapter player =
        new ComponentPlayerStateAdapter(combatStats, inventory);
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    PlayerDeck playerDeck = new PlayerDeck(cardService, List.of("strike", "defend"));
    List<String> deckBefore = playerDeck.getCardIds();
    ShopItem offer = new ShopItem("strike-offer", "strike", "Strike", 20, 2);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new CardServiceCatalogAdapter(cardService), new PlayerDeckAdapter(playerDeck));
    EncounterFlowController flow = new EncounterFlowController(player, transactions, mapCallback);
    ShopEncounter shop = flow.startShop(7, new ShopService(new ShopItem[] {offer}));

    PurchaseResult result = shop.purchase("strike-offer");

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.INSUFFICIENT_GOLD, result.getStatus());
    assertEquals(10, inventory.getGold());
    assertEquals(deckBefore, playerDeck.getCardIds());
    assertEquals(2, offer.stock);

    shop.complete(false);
    shop.complete(true);
    flow.onEncounterComplete(7, true);

    assertEquals(1, mapCallback.completionCount);
    assertEquals(7, mapCallback.nodeId);
    assertFalse(mapCallback.success);
    assertEquals(NodeState.CURRENT, map.getNode(7).getState());
    assertEquals(NodeState.LOCKED, map.getNode(8).getState());
    assertFalse(flow.isEncounterActive());
  }

  private MapGraph createLinearMap() {
    MapGraph map = new MapGraph(Map.of(), false);
    MapNode chance = new MapNode(1, RoomType.EVENT);
    MapNode shop = new MapNode(2, RoomType.SHOP);
    MapNode next = new MapNode(3, RoomType.EVENT);
    map.addNode(chance);
    map.addNode(shop);
    map.addNode(next);
    map.connectNodes(1, 2);
    map.connectNodes(2, 3);
    map.startRun(1);
    return map;
  }

  private MapGraph createFailedShopMap() {
    MapGraph map = new MapGraph(Map.of(), false);
    MapNode shop = new MapNode(7, RoomType.SHOP);
    MapNode next = new MapNode(8, RoomType.EVENT);
    shop.setState(NodeState.CURRENT);
    map.addNode(shop);
    map.addNode(next);
    map.connectNodes(7, 8);
    return map;
  }

  private ChanceEncounter createChanceEncounter() {
    return new ChanceEncounter(
        "shrine",
        "A shrine offers a risky bargain.",
        List.of(new ChanceChoice("risk", "Lose health for gold.", new ChanceOutcome(-10, 25))));
  }

  private static final class RecordingMapCallback implements EncounterCallback {
    private final MapGraph map;
    private int completionCount;
    private Integer nodeId;
    private boolean success;

    private RecordingMapCallback(MapGraph map) {
      this.map = map;
    }

    @Override
    public void onEncounterComplete(Integer nodeId, boolean success) {
      completionCount++;
      this.nodeId = nodeId;
      this.success = success;
      map.onEncounterComplete(nodeId, success);
    }
  }
}
