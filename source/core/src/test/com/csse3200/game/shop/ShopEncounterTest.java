package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopEncounterTest {
  @Test
  void shouldCompleteMapNodeWhenShopEncounterEnds() {
    RoomDistributionConfig config = new RoomDistributionConfig(MapGraph.MAX_NODE_COUNT, 60, 30, 10);
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));
    MapNode shopNode = new MapNode(1, RoomType.SHOP);
    MapNode nextNode = new MapNode(2, RoomType.EVENT);
    shopNode.setState(NodeState.CURRENT);
    graph.addNode(shopNode);
    graph.addNode(nextNode);
    graph.connectNodes(1, 2);

    ShopEncounter encounter =
        new ShopEncounter(
            1,
            new InventoryComponent(50),
            new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)}),
            graph);

    encounter.complete(true);

    assertTrue(encounter.isCompleted());
    assertTrue(encounter.completedSuccessfully());
    assertEquals(NodeState.COMPLETED, shopNode.getState());
    assertEquals(NodeState.AVAILABLE, nextNode.getState());
  }

  @Test
  void shouldRejectPurchasesAfterShopEncounterEnds() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)});
    ShopEncounter encounter = new ShopEncounter(1, inventory, shop, null);

    encounter.complete(true);
    PurchaseResult result = encounter.purchase("heal");

    assertFalse(result.isSuccess());
    assertEquals(PurchaseResult.Status.SHOP_CLOSED, result.getStatus());
    assertEquals(50, inventory.getGold());
    assertEquals(0, inventory.getCardCount("card_heal"));
    assertEquals(1, shop.getItem("heal").stock);
  }

  @Test
  void shouldReportCompletionOnlyOnce() {
    CountingCallback callback = new CountingCallback();
    ShopEncounter encounter =
        new ShopEncounter(
            1,
            new InventoryComponent(50),
            new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)}),
            callback);

    encounter.complete(false);
    encounter.complete(true);

    assertTrue(encounter.isCompleted());
    assertFalse(encounter.completedSuccessfully());
    assertEquals(1, callback.count);
    assertEquals(1, callback.nodeId);
    assertFalse(callback.success);
  }

  @Test
  void shouldNotUnlockMapNodeWhenShopEncounterFails() {
    RoomDistributionConfig config = new RoomDistributionConfig(MapGraph.MAX_NODE_COUNT, 60, 30, 10);
    MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));
    MapNode shopNode = new MapNode(1, RoomType.SHOP);
    MapNode nextNode = new MapNode(2, RoomType.EVENT);
    shopNode.setState(NodeState.CURRENT);
    graph.addNode(shopNode);
    graph.addNode(nextNode);
    graph.connectNodes(1, 2);

    ShopEncounter encounter =
        new ShopEncounter(
            1,
            new InventoryComponent(50),
            new ShopService(new ShopItem[] {new ShopItem("heal", "card_heal", "Heal", 20, 1)}),
            graph);

    encounter.complete(false);

    assertTrue(encounter.isCompleted());
    assertFalse(encounter.completedSuccessfully());
    assertEquals(NodeState.CURRENT, shopNode.getState());
    assertEquals(NodeState.LOCKED, nextNode.getState());
  }

  private static class CountingCallback implements EncounterCallback {
    private int count;
    private Integer nodeId;
    private boolean success;

    @Override
    public void onEncounterComplete(Integer nodeId, boolean success) {
      count++;
      this.nodeId = nodeId;
      this.success = success;
    }
  }
}
