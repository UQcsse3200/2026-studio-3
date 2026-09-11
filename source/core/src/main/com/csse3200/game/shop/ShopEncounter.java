package com.csse3200.game.shop;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.encounters.integration.InventoryShopTransactionAdapter;
import com.csse3200.game.encounters.integration.ShopTransactionGateway;
import com.csse3200.game.maps.EncounterCallback;
import java.util.Collection;

/** Bridges shop purchase logic with the map encounter completion callback. */
public class ShopEncounter {
  public static final Integer DEFAULT_NODE_ID = -1;

  private final Integer nodeId;
  private final ShopTransactionGateway transactions;
  private final ShopService shopService;
  private final EncounterCallback callback;
  private boolean completed;
  private boolean completedSuccessfully;

  /**
   * Creates a legacy Sprint 1 Shop Encounter backed by the temporary inventory.
   *
   * @param inventory temporary player inventory
   * @param shopService shop inventory and stock service
   */
  public ShopEncounter(InventoryComponent inventory, ShopService shopService) {
    this(DEFAULT_NODE_ID, inventory, shopService, null);
  }

  /**
   * Creates a legacy Sprint 1 Shop Encounter with Map completion.
   *
   * @param nodeId associated map node
   * @param inventory temporary player inventory
   * @param shopService shop inventory and stock service
   * @param callback map completion callback
   */
  public ShopEncounter(
      Integer nodeId,
      InventoryComponent inventory,
      ShopService shopService,
      EncounterCallback callback) {
    this(
        nodeId,
        shopService,
        inventory == null ? null : new InventoryShopTransactionAdapter(inventory),
        callback);
  }

  /**
   * Creates an encounter connected to the cross-team shop transaction boundary.
   *
   * @param nodeId associated map node
   * @param shopService shop inventory and stock service
   * @param transactions Player/Card/Deck transaction boundary
   * @param callback map completion callback
   */
  public ShopEncounter(
      Integer nodeId,
      ShopService shopService,
      ShopTransactionGateway transactions,
      EncounterCallback callback) {
    this.nodeId = nodeId == null ? DEFAULT_NODE_ID : nodeId;
    this.transactions = transactions;
    this.shopService = shopService == null ? new ShopService((ShopConfig) null) : shopService;
    this.callback = callback;
  }

  public PurchaseResult purchase(String itemId) {
    if (completed) {
      return PurchaseResult.failure(PurchaseResult.Status.SHOP_CLOSED, null);
    }

    return shopService.purchaseWithGateway(itemId, transactions);
  }

  public PurchaseResult canPurchase(String itemId) {
    if (completed) {
      return PurchaseResult.failure(PurchaseResult.Status.SHOP_CLOSED, null);
    }

    return shopService.canPurchaseWithGateway(itemId, transactions);
  }

  public Collection<ShopItem> getItems() {
    return shopService.getItems();
  }

  public Integer getNodeId() {
    return nodeId;
  }

  /**
   * Returns the current currency shown by the Shop UI.
   *
   * @return current player currency, or null when no Player integration is available
   */
  public Integer getCurrency() {
    return transactions == null ? null : transactions.getCurrency();
  }

  public boolean isCompleted() {
    return completed;
  }

  public boolean completedSuccessfully() {
    return completedSuccessfully;
  }

  /** Completes the shop encounter and reports completion to the map framework. */
  public void complete(boolean success) {
    if (completed) {
      return;
    }
    completed = true;
    completedSuccessfully = success;
    if (callback != null) {
      callback.onEncounterComplete(nodeId, success);
    }
  }
}
