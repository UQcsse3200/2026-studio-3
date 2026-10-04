package com.csse3200.game.services;

import static junit.framework.TestCase.assertEquals;

import com.csse3200.game.services.audio.SoundId;
import org.junit.Test;

public class AudioTests {

  // SoundId Enum ordinals must match specifically ordered paths in AudioService soundPaths array
  @Test
  public void testSoundIDOrdinals() {

    assertEquals(1, SoundId.MENU_HOVER.ordinal());
    assertEquals(2, SoundId.ITEM_PURCHASE.ordinal());
    assertEquals(3, SoundId.ENTER_COMBAT.ordinal());
    assertEquals(4, SoundId.ENTER_SHOP.ordinal());
    assertEquals(5, SoundId.ERROR.ordinal());
    assertEquals(6, SoundId.ENTER_ENCOUNTER.ordinal());
    assertEquals(7, SoundId.CARD_HOVER.ordinal());
    assertEquals(8, SoundId.CARD_SHUFFLE.ordinal());
    assertEquals(9, SoundId.ENTER_ELITE.ordinal());
    assertEquals(10, SoundId.ITEM_PICKUP.ordinal());
    assertEquals(11, SoundId.SWORD_SWING.ordinal());
    assertEquals(12, SoundId.SHIELD_GUARD.ordinal());
    assertEquals(13, SoundId.BANDAGE.ordinal());
    assertEquals(14, SoundId.BOTTLE_CORK.ordinal());
    assertEquals(15, SoundId.ARMOUR_BREAK.ordinal());
    assertEquals(16, SoundId.MAGIC_CHIME.ordinal());
    assertEquals(17, SoundId.LEAVE_SHOP.ordinal());
  }
}
