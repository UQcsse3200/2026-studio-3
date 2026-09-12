package com.csse3200.game.shop;

/** Result object returned by shop purchase attempts. */
public class PurchaseResult {
  public enum Status {
    SUCCESS,
    ITEM_NOT_FOUND,
    OUT_OF_STOCK,
    INSUFFICIENT_GOLD,
    CARD_NOT_FOUND,
    INVALID_ITEM,
    INVALID_INVENTORY,
    SHOP_CLOSED,
    TRANSACTION_FAILED
  }

  private final Status status;
  private final ShopItem item;
  private final String message;

  private PurchaseResult(Status status, ShopItem item, String message) {
    this.status = status;
    this.item = item;
    this.message = message;
  }

  public static PurchaseResult success(ShopItem item) {
    String name = item == null ? "item" : item.getDisplayName();
    return new PurchaseResult(Status.SUCCESS, item, String.format("Bought %s.", name));
  }

  public static PurchaseResult available(ShopItem item) {
    return new PurchaseResult(Status.SUCCESS, item, "Item can be purchased.");
  }

  public static PurchaseResult failure(Status status, ShopItem item) {
    Status safeStatus =
        status == null || status == Status.SUCCESS ? Status.TRANSACTION_FAILED : status;
    return new PurchaseResult(safeStatus, item, defaultMessage(safeStatus));
  }

  public static PurchaseResult failure(Status status, ShopItem item, String message) {
    Status safeStatus =
        status == null || status == Status.SUCCESS ? Status.TRANSACTION_FAILED : status;
    String safeMessage =
        message == null || message.isBlank() ? defaultMessage(safeStatus) : message;
    return new PurchaseResult(safeStatus, item, safeMessage);
  }

  public boolean isSuccess() {
    return status == Status.SUCCESS;
  }

  public Status getStatus() {
    return status;
  }

  public ShopItem getItem() {
    return item;
  }

  public String getMessage() {
    return message;
  }

  private static String defaultMessage(Status status) {
    switch (status) {
      case ITEM_NOT_FOUND:
        return "Item not found.";
      case OUT_OF_STOCK:
        return "Item is out of stock.";
      case INSUFFICIENT_GOLD:
        return "Not enough gold.";
      case CARD_NOT_FOUND:
        return "Card definition is unavailable.";
      case INVALID_ITEM:
        return "Item cannot be purchased.";
      case INVALID_INVENTORY:
        return "Player inventory is unavailable.";
      case SHOP_CLOSED:
        return "Shop encounter has already ended.";
      case TRANSACTION_FAILED:
        return "Purchase failed.";
      case SUCCESS:
      default:
        return "Purchase successful.";
    }
  }
}
