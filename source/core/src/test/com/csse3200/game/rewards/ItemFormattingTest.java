package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ItemFormattingTest {

  @Test
  void formatsLuckyCoin() {
    assertEquals("Shard of Fortune", ItemFormatting.formatItemName(ItemType.LUCKY_COIN));
  }

  @Test
  void formatsEnergyCrystal() {
    assertEquals("Ember of the Divine", ItemFormatting.formatItemName(ItemType.ENERGY_CRYSTAL));
  }

  @Test
  void formatsMerchantsFavor() {
    assertEquals("Relic of Commerce", ItemFormatting.formatItemName(ItemType.MERCHANTS_FAVOR));
  }

  @Test
  void formatsIronAegis() {
    assertEquals("Aegis Fragment", ItemFormatting.formatItemName(ItemType.IRON_AEGIS));
  }

  @Test
  void formatsWarriorsCrest() {
    assertEquals("Blessed Crest", ItemFormatting.formatItemName(ItemType.WARRIORS_CREST));
  }
}
