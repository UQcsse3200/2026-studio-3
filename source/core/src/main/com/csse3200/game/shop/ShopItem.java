package com.csse3200.game.shop;

/** Defines one purchasable shop item and its linked card id. */
public class ShopItem {
  public String id = "";
  public String cardId = "";
  public String name = "";
  public String description = "";
  public int price = 0;
  public int stock = 0;

  public ShopItem() {
    // Required for JSON deserialisation.
  }

  public ShopItem(String id, String cardId, String name, int price, int stock) {
    this(id, cardId, name, "", price, stock);
  }

  public ShopItem(String id, String cardId, String name, String description, int price, int stock) {
    this.id = id;
    this.cardId = cardId;
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
  }

  public boolean hasStock() {
    return stock > 0;
  }

  public boolean isValid() {
    return hasText(id) && hasText(cardId) && hasText(name) && price >= 0 && stock >= 0;
  }

  public boolean isPurchasable() {
    return isValid() && hasStock();
  }

  public void decreaseStock() {
    if (stock > 0) {
      stock--;
    }
  }

  public boolean increaseStock(int quantity) {
    if (quantity <= 0 || stock < 0 || quantity > Integer.MAX_VALUE - stock) {
      return false;
    }

    stock += quantity;
    return true;
  }

  public String getDisplayName() {
    return hasText(name) ? name : id;
  }

  public String getDescription() {
    return description == null ? "" : description;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
