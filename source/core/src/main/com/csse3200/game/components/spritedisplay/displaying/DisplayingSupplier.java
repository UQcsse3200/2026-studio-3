// DisplayingSupplier.java
package com.csse3200.game.components.spritedisplay.displaying;

@FunctionalInterface
public interface DisplayingSupplier {
  Displaying create(DisplayingRecord rec);
}
