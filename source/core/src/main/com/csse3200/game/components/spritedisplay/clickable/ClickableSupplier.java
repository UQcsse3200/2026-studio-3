package com.csse3200.game.components.spritedisplay.clickable;

@FunctionalInterface
public interface ClickableSupplier {
  Clickable create(ClickableRecord rec);
}
