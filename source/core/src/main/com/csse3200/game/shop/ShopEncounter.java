package com.csse3200.game.shop;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.encounters.integration.InventoryShopTransactionAdapter;
import com.csse3200.game.encounters.integration.ShopTransactionGateway;
import com.csse3200.game.encounters.integration.ShopTransactionStatus;
import com.csse3200.game.encounters.results.AcquisitionResult;
import com.csse3200.game.maps.EncounterCallback;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Supplier;

/** Bridges shop purchase logic with the map encounter completion callback. */
public class ShopEncounter {
  public static final Integer DEFAULT_NODE_ID = -1;

  private final Integer nodeId;
  private final ShopTransactionGateway transactions;
  private ShopService shopService;
  private final EncounterCallback callback;
  private boolean completed;
  private boolean completedSuccessfully;
  private long stockRevision;
  private int successfulRefreshes;
  private boolean busy;
  private boolean paymentBlocked;
  private ShopRefreshPolicy refreshPolicy;
  private Supplier<ShopService> stockFactory;
  private final String resultPrefix = UUID.randomUUID().toString();
  private final Map<String, RefreshCommand> refreshCommands = new HashMap<>();
  private final Map<String, PurchaseCommand> purchaseCommands = new HashMap<>();

  private record RefreshCommand(long revision, ShopRefreshResult result) {}

  private record PurchaseCommand(String itemId, long revision, ShopPurchaseReceipt receipt) {}

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

  public synchronized PurchaseResult purchase(String itemId) {
    if (completed) {
      return PurchaseResult.failure(PurchaseResult.Status.SHOP_CLOSED, null);
    }

    if (busy || paymentBlocked) {
      return PurchaseResult.failure(PurchaseResult.Status.BUSY, null);
    }
    busy = true;
    try {
      PurchaseResult result = shopService.purchaseWithGateway(itemId, transactions);
      if (result.isSuccess()) {
        stockRevision++;
      }
      return result;
    } finally {
      busy = false;
    }
  }

  /**
   * Versioned, idempotent UI command. A new click uses a new action ID and the displayed revision.
   */
  public synchronized ShopPurchaseReceipt purchase(
      String itemId, long expectedRevision, String actionId) {
    if (completed) {
      return purchaseFailure(PurchaseResult.Status.SHOP_CLOSED);
    }
    if (actionId == null
        || actionId.isBlank()
        || itemId == null
        || itemId.isBlank()
        || refreshCommands.containsKey(actionId)) {
      return purchaseFailure(PurchaseResult.Status.INVALID_REQUEST);
    }
    PurchaseCommand previous = purchaseCommands.get(actionId);
    if (previous != null) {
      return previous.revision() == expectedRevision && previous.itemId().equals(itemId)
          ? previous.receipt()
          : purchaseFailure(PurchaseResult.Status.INVALID_REQUEST);
    }
    if (busy || paymentBlocked) {
      return purchaseFailure(PurchaseResult.Status.BUSY);
    }
    if (expectedRevision != stockRevision) {
      return purchaseFailure(PurchaseResult.Status.STALE_STOCK);
    }
    ShopItem item = shopService.getItem(itemId);
    int price = item == null ? 0 : shopService.getPrice(itemId, transactions);
    PurchaseResult result = purchase(itemId);
    AcquisitionResult acquisition =
        result.isSuccess()
            ? new AcquisitionResult(
                "shop:" + resultPrefix + ":" + actionId,
                List.of(
                    new AcquisitionResult.Entry(
                        AcquisitionResult.Kind.CARD,
                        item.cardId,
                        1,
                        item.getDisplayName(),
                        item.getDescription(),
                        null)))
            : null;
    ShopPurchaseReceipt receipt =
        new ShopPurchaseReceipt(result, result.isSuccess() ? price : 0, stockRevision, acquisition);
    purchaseCommands.put(actionId, new PurchaseCommand(itemId, expectedRevision, receipt));
    return receipt;
  }

  private ShopPurchaseReceipt purchaseFailure(PurchaseResult.Status status) {
    return new ShopPurchaseReceipt(PurchaseResult.failure(status, null), 0, stockRevision, null);
  }

  /** Installs the generator once, without replacing the discounted initial inventory. */
  public synchronized void enableRefresh(ShopRefreshPolicy policy, Supplier<ShopService> factory) {
    if (refreshPolicy != null || completed || busy) {
      throw new IllegalStateException("Refresh is already configured or the shop is inactive");
    }
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(factory, "factory");
    refreshPolicy = policy;
    stockFactory = factory;
  }

  /** Supplies the approved paid fee later; it never resets the already-used free refresh. */
  public synchronized void setFirstPaidRefreshFee(int fee) {
    ShopRefreshPolicy approved = new ShopRefreshPolicy(fee);
    if (refreshPolicy == null || refreshPolicy.firstPaidFee() != null || completed || busy) {
      throw new IllegalStateException("Paid fee can only be confirmed once on an active shop");
    }
    refreshPolicy = approved;
    stockRevision++;
  }

  public synchronized OptionalInt getNextRefreshCost() {
    return refreshPolicy == null || completed || paymentBlocked
        ? OptionalInt.empty()
        : refreshPolicy.costAfter(successfulRefreshes);
  }

  /** Generates and validates all three offers before charging an undiscounted fee. */
  public synchronized ShopRefreshResult refresh(long expectedRevision, String actionId) {
    if (completed) {
      return refreshFailure(ShopRefreshResult.Status.SHOP_CLOSED);
    }
    if (actionId == null || actionId.isBlank() || purchaseCommands.containsKey(actionId)) {
      return refreshFailure(ShopRefreshResult.Status.INVALID_REQUEST);
    }
    RefreshCommand previous = refreshCommands.get(actionId);
    if (previous != null) {
      return previous.revision() == expectedRevision
          ? previous.result()
          : refreshFailure(ShopRefreshResult.Status.INVALID_REQUEST);
    }
    if (busy || paymentBlocked) {
      return refreshFailure(ShopRefreshResult.Status.BUSY);
    }
    if (expectedRevision != stockRevision) {
      return refreshFailure(ShopRefreshResult.Status.STALE_STOCK);
    }
    if (refreshPolicy == null) {
      return refreshFailure(ShopRefreshResult.Status.NOT_CONFIGURED);
    }
    OptionalInt quote = refreshPolicy.costAfter(successfulRefreshes);
    if (quote.isEmpty()) {
      return refreshFailure(
          refreshPolicy.firstPaidFee() == null
              ? ShopRefreshResult.Status.NOT_CONFIGURED
              : ShopRefreshResult.Status.COST_OVERFLOW);
    }
    busy = true;
    try {
      ShopRefreshResult result = performRefresh(quote.getAsInt());
      refreshCommands.put(actionId, new RefreshCommand(expectedRevision, result));
      return result;
    } finally {
      busy = false;
    }
  }

  private ShopRefreshResult performRefresh(int cost) {
    if (transactions == null) {
      return refreshFailure(ShopRefreshResult.Status.PAYMENT_FAILED);
    }
    if (transactions.getCurrency() < cost) {
      return refreshFailure(ShopRefreshResult.Status.INSUFFICIENT_GOLD);
    }
    ShopService replacement;
    try {
      ShopService generated = Objects.requireNonNull(stockFactory.get(), "generated stock");
      if (!generated.getValidationErrors().isEmpty() || generated.getItems().size() != 3) {
        throw new IllegalStateException("Refresh requires three valid offers");
      }
      HashSet<String> cards = new HashSet<>();
      ShopItem[] copies =
          generated.getItems().stream()
              .map(
                  item -> {
                    if (!item.isPurchasable()
                        || !cards.add(item.cardId)
                        || transactions.validatePurchase(item.cardId, 0)
                            != ShopTransactionStatus.READY) {
                      throw new IllegalStateException("Refresh requires distinct stocked cards");
                    }
                    return new ShopItem(
                        item.id,
                        item.cardId,
                        item.name,
                        item.getDescription(),
                        item.price,
                        item.stock);
                  })
              .toArray(ShopItem[]::new);
      replacement = new ShopService(copies);
      replacement.disableDiscount();
    } catch (RuntimeException exception) {
      return refreshFailure(ShopRefreshResult.Status.GENERATION_FAILED);
    }
    ShopTransactionStatus payment =
        cost == 0 ? ShopTransactionStatus.SUCCESS : transactions.chargeServiceFee(cost);
    if (payment != ShopTransactionStatus.SUCCESS) {
      if (payment == ShopTransactionStatus.ROLLBACK_FAILED) {
        paymentBlocked = true;
        return refreshFailure(ShopRefreshResult.Status.ROLLBACK_FAILED);
      }
      return refreshFailure(
          payment == ShopTransactionStatus.INSUFFICIENT_CURRENCY
              ? ShopRefreshResult.Status.INSUFFICIENT_GOLD
              : ShopRefreshResult.Status.PAYMENT_FAILED);
    }
    shopService = replacement;
    successfulRefreshes++;
    stockRevision++;
    return new ShopRefreshResult(ShopRefreshResult.Status.SUCCESS, cost, stockRevision);
  }

  private ShopRefreshResult refreshFailure(ShopRefreshResult.Status status) {
    return new ShopRefreshResult(status, 0, stockRevision);
  }

  public synchronized ShopSnapshot snapshot() {
    List<ShopSnapshot.Offer> offers =
        shopService.getItems().stream()
            .map(
                item ->
                    new ShopSnapshot.Offer(
                        item.id,
                        item.cardId,
                        item.getDisplayName(),
                        item.getDescription(),
                        item.price,
                        shopService.getPrice(item.id, transactions),
                        item.stock))
            .toList();
    return new ShopSnapshot(
        stockRevision,
        getCurrency(),
        shopService.getEffectiveDiscount(transactions),
        successfulRefreshes,
        getNextRefreshCost(),
        completed,
        offers);
  }

  public synchronized PurchaseResult canPurchase(String itemId) {
    if (completed) {
      return PurchaseResult.failure(PurchaseResult.Status.SHOP_CLOSED, null);
    }

    if (busy || paymentBlocked) {
      return PurchaseResult.failure(PurchaseResult.Status.BUSY, null);
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

  /**
   * Returns this stock batch's effective discount, or null when no Player integration is available.
   * Refreshed batches return zero without removing the player's permanent discount items.
   *
   * @return current shop discount, or null
   */
  public Float getShopDiscount() {
    return transactions == null ? null : shopService.getEffectiveDiscount(transactions);
  }

  public boolean isCompleted() {
    return completed;
  }

  public boolean completedSuccessfully() {
    return completedSuccessfully;
  }

  /**
   * Leaves the shop normally.
   *
   * <p>A normal player exit counts as successful encounter completion.
   */
  public void leave() {
    complete(true);
  }

  /** Completes the shop encounter and reports completion to the map framework. */
  public synchronized void complete(boolean success) {
    if (busy) {
      return;
    }
    if (completed) {
      return;
    }
    completed = true;
    completedSuccessfully = success;
    refreshCommands.clear();
    purchaseCommands.clear();
    if (callback != null) {
      callback.onEncounterComplete(nodeId, success);
    }
  }
}
