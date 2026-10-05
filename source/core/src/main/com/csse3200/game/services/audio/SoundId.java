package com.csse3200.game.services.audio;

/**
 * Stores positions of sound effects in the path string and makes function calls easily understood.
 * ID's must be added in the order of position in soundPath in AudioService.
 */
public enum SoundId {
  IMPACT, // TODO: remove
  MENU_HOVER,
  ITEM_PURCHASE,
  ENTER_COMBAT,
  ENTER_SHOP,
  ERROR,
  ENTER_ENCOUNTER,
  CARD_HOVER,
  CARD_SHUFFLE,
  ENTER_ELITE,
  ITEM_PICKUP,
  SWORD_SWING,
  SHIELD_GUARD,
  BANDAGE,
  BOTTLE_CORK,
  ARMOUR_BREAK,
  MAGIC_CHIME,
  LEAVE_SHOP,
  VICTORY,
  BUTTON_CLICK,
  DEFEAT
}
