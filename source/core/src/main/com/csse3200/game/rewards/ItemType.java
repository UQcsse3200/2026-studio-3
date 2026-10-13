package com.csse3200.game.rewards;

public enum ItemType {
  LUCKY_COIN,
  ENERGY_CRYSTAL,
  MERCHANTS_FAVOR,
  IRON_AEGIS,
  WARRIORS_CREST,
  ASHEN_EMBER,
  BRAND_OF_THE_DAMNED,
  SHATTERED_AEGIS;

  /** Returns whether this item is consumed to apply an effect during the player's turn. */
  public boolean isBattleConsumable() {
    return this == IRON_AEGIS || this == WARRIORS_CREST;
  }

  /** Returns whether this item is a curse with a negative passive effect. */
  public boolean isCurse() {
    return this == ASHEN_EMBER || this == BRAND_OF_THE_DAMNED || this == SHATTERED_AEGIS;
  }
}
