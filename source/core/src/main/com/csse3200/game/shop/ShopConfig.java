package com.csse3200.game.shop;

/** Defines a list of shop items loaded from JSON. */
public class ShopConfig {
  public ShopItem[] items = new ShopItem[0];

  public ShopItem[] getItems() {
    return items == null ? new ShopItem[0] : items;
  }
}
