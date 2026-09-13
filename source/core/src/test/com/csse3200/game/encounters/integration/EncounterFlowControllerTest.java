package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.maps.*;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import java.util.List;
import org.junit.jupiter.api.Test;

class EncounterFlowControllerTest {
  @Test
  void shouldCompleteChanceAndShopRoundTrip() {
    MapGraph map = createMap();
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    MockDeckGateway deck = new MockDeckGateway();
    IntegratedShopTransactionGateway shopTransactions =
        new IntegratedShopTransactionGateway(player, new MockCardCatalogGateway("card_heal"), deck);
    EncounterFlowController flow = new EncounterFlowController(player, shopTransactions, map);

    ChanceEncounterSession chance = flow.startChance(1, createChanceEncounter());
    ChanceResolution chanceResult = chance.resolveChoice("risk");
    assertTrue(chanceResult.isSuccess());
    assertTrue(chance.complete());

    assertEquals(90, player.getHealth());
    assertEquals(125, player.getCurrency());
    assertEquals(NodeState.COMPLETED, map.getNode(1).getState());
    assertEquals(NodeState.AVAILABLE, map.getNode(2).getState());
    assertFalse(flow.isEncounterActive());

    map.getNode(2).setState(NodeState.CURRENT);
    ShopService shopService =
        new ShopService(new ShopItem[] {new ShopItem("heal-offer", "card_heal", "Heal", 30, 1)});
    ShopEncounter shop = flow.startShop(2, shopService);
    PurchaseResult purchase = shop.purchase("heal-offer");
    shop.complete(true);

    assertTrue(purchase.isSuccess());
    assertEquals(95, player.getCurrency());
    assertEquals(1, deck.getCardCount("card_heal"));
    assertEquals(0, shopService.getItem("heal-offer").stock);
    assertEquals(NodeState.COMPLETED, map.getNode(1).getState());
    assertEquals(NodeState.COMPLETED, map.getNode(2).getState());
    assertEquals(NodeState.AVAILABLE, map.getNode(3).getState());
    assertFalse(flow.isEncounterActive());
  }

  @Test
  void shouldPreventTwoConcurrentEncounters() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("card_heal"), new MockDeckGateway());
    EncounterFlowController flow =
        new EncounterFlowController(player, transactions, (nodeId, success) -> {});

    flow.startChance(1, createChanceEncounter());

    assertThrows(
        IllegalStateException.class, () -> flow.startShop(2, new ShopService(new ShopItem[0])));
  }

  @Test
  void shouldNotAdvanceMapWhenChanceIsCancelled() {
    MapGraph map = createMap();
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("card_heal"), new MockDeckGateway());
    EncounterFlowController flow = new EncounterFlowController(player, transactions, map);

    ChanceEncounterSession chance = flow.startChance(1, createChanceEncounter());
    assertTrue(chance.cancel());

    assertEquals(NodeState.CURRENT, map.getNode(1).getState());
    assertEquals(NodeState.LOCKED, map.getNode(2).getState());
    assertFalse(flow.isEncounterActive());
  }

  @Test
  void shouldRejectInvalidNodeIdsWithoutActivatingEncounter() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("card_heal"), new MockDeckGateway());
    EncounterFlowController flow =
        new EncounterFlowController(player, transactions, (nodeId, success) -> {});

    assertThrows(
        IllegalArgumentException.class, () -> flow.startChance(null, createChanceEncounter()));
    assertThrows(
        IllegalArgumentException.class,
        () -> flow.startShop(null, new ShopService(new ShopItem[0])));

    assertFalse(flow.isEncounterActive());
    assertNull(flow.getActiveNodeId());
    assertNull(flow.getActiveType());
  }

  @Test
  void shouldRecoverWhenChanceSessionConstructionFails() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("card_heal"), new MockDeckGateway());
    EncounterFlowController flow =
        new EncounterFlowController(player, transactions, (nodeId, success) -> {});

    assertThrows(NullPointerException.class, () -> flow.startChance(3, null));
    assertFalse(flow.isEncounterActive());

    ShopEncounter shop = flow.startShop(2, new ShopService(new ShopItem[0]));
    assertTrue(flow.isEncounterActive());
    assertEquals(2, flow.getActiveNodeId());
    shop.complete(true);
    assertFalse(flow.isEncounterActive());
  }

  @Test
  void shouldIgnoreStaleAndDuplicateCompletionCallbacks() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("card_heal"), new MockDeckGateway());
    RecordingCallback callback = new RecordingCallback();
    EncounterFlowController flow = new EncounterFlowController(player, transactions, callback);

    flow.startShop(2, new ShopService(new ShopItem[0]));
    flow.onEncounterComplete(999, true);

    assertTrue(flow.isEncounterActive());
    assertEquals(0, callback.count);

    flow.onEncounterComplete(2, true);
    flow.onEncounterComplete(2, true);

    assertFalse(flow.isEncounterActive());
    assertEquals(1, callback.count);
    assertEquals(2, callback.nodeId);
    assertTrue(callback.success);
  }

  @Test
  void shouldClearCurrentEncounterBeforeMapCallbackStartsNextEncounter() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 100);
    IntegratedShopTransactionGateway transactions =
        new IntegratedShopTransactionGateway(
            player, new MockCardCatalogGateway("card_heal"), new MockDeckGateway());
    EncounterFlowController[] flowHolder = new EncounterFlowController[1];
    flowHolder[0] =
        new EncounterFlowController(
            player,
            transactions,
            (nodeId, success) -> flowHolder[0].startShop(4, new ShopService(new ShopItem[0])));

    ShopEncounter firstShop = flowHolder[0].startShop(5, new ShopService(new ShopItem[0]));
    firstShop.complete(true);

    assertTrue(flowHolder[0].isEncounterActive());
    assertEquals(4, flowHolder[0].getActiveNodeId());
    assertEquals(EncounterFlowController.EncounterType.SHOP, flowHolder[0].getActiveType());
  }

  private MapGraph createMap() {
    RoomDistributionConfig config = new RoomDistributionConfig(MapGraph.MAX_NODE_COUNT, 60, 30, 10);
    MapGraph map = new MapGraph(NodePoolGenerator.generate(config));
    MapNode chance = new MapNode(1, RoomType.EVENT);
    MapNode shop = new MapNode(2, RoomType.SHOP);
    MapNode returnNode = new MapNode(3, RoomType.EVENT);
    chance.setState(NodeState.CURRENT);
    map.addNode(chance);
    map.addNode(shop);
    map.addNode(returnNode);
    map.connectNodes(1, 2);
    map.connectNodes(2, 3);
    return map;
  }

  private ChanceEncounter createChanceEncounter() {
    return new ChanceEncounter(
        "shrine",
        "A shrine offers a risky bargain.",
        List.of(new ChanceChoice("risk", "Lose health for gold.", new ChanceOutcome(-10, 25))));
  }

  private static final class RecordingCallback implements com.csse3200.game.maps.EncounterCallback {
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
