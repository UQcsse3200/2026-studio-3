package com.csse3200.game.rewards;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared display-name formatting for {@link ItemType}, used anywhere an item name is shown to the
 * player (reward selection, inventory display, item library, etc). Maps each item type to its
 * mythical display name, confirmed by the design committee.
 */
public final class ItemFormatting {

  private static final Map<ItemType, String> DISPLAY_NAMES = new EnumMap<>(ItemType.class);

  static {
    DISPLAY_NAMES.put(ItemType.LUCKY_COIN, "Shard of Fortune");
    DISPLAY_NAMES.put(ItemType.ENERGY_CRYSTAL, "Ember of the Divine");
    DISPLAY_NAMES.put(ItemType.MERCHANTS_FAVOR, "Relic of Commerce");
    DISPLAY_NAMES.put(ItemType.IRON_AEGIS, "Aegis Fragment");
    DISPLAY_NAMES.put(ItemType.WARRIORS_CREST, "Blessed Crest");
    DISPLAY_NAMES.put(ItemType.ASHEN_EMBER, "Ashen Ember");
    DISPLAY_NAMES.put(ItemType.BRAND_OF_THE_DAMNED, "Brand of the Damned");
    DISPLAY_NAMES.put(ItemType.SHATTERED_AEGIS, "Shattered Aegis");
    DISPLAY_NAMES.put(ItemType.AMBROSIA_FLASK, "Draught of Ambrosia");
    DISPLAY_NAMES.put(ItemType.BLOOD_OATH_SIGIL, "Blood-Oath Sigil");
    DISPLAY_NAMES.put(ItemType.ETERNAL_VITALITY, "Eternal Vitality");
  }

  private ItemFormatting() {
    // Utility class — not instantiable.
  }

  /**
   * Returns the confirmed mythical display name for an item type.
   *
   * @param itemId the item type to format
   * @return the display name shown to the player
   */
  public static String formatItemName(ItemType itemId) {
    return DISPLAY_NAMES.getOrDefault(itemId, fallbackName(itemId));
  }

  private static final String BATTLE_REWARDS_SOURCE = "Battle rewards";
  private static final String CHANCE_EVENTS_SOURCE = "Chance Events";
  private static final Map<ItemType, String> SOURCES = new EnumMap<>(ItemType.class);

  static {
    SOURCES.put(ItemType.LUCKY_COIN, BATTLE_REWARDS_SOURCE);
    SOURCES.put(ItemType.ENERGY_CRYSTAL, BATTLE_REWARDS_SOURCE);
    SOURCES.put(ItemType.MERCHANTS_FAVOR, BATTLE_REWARDS_SOURCE);
    SOURCES.put(ItemType.IRON_AEGIS, BATTLE_REWARDS_SOURCE);
    SOURCES.put(ItemType.WARRIORS_CREST, BATTLE_REWARDS_SOURCE);
    SOURCES.put(ItemType.ASHEN_EMBER, CHANCE_EVENTS_SOURCE);
    SOURCES.put(ItemType.BRAND_OF_THE_DAMNED, CHANCE_EVENTS_SOURCE);
    SOURCES.put(ItemType.SHATTERED_AEGIS, CHANCE_EVENTS_SOURCE);
    SOURCES.put(ItemType.AMBROSIA_FLASK, CHANCE_EVENTS_SOURCE);
    SOURCES.put(ItemType.BLOOD_OATH_SIGIL, CHANCE_EVENTS_SOURCE);
    SOURCES.put(ItemType.ETERNAL_VITALITY, "Elite Portal - Ancient Temple");
  }

  /** Returns where the player can obtain the given item, for display in the Item Library. */
  public static String formatItemSource(ItemType item) {
    return SOURCES.getOrDefault(item, "Unknown");
  }

  /** Falls back to the old enum-splitting behaviour if a new item type is added without a name. */
  private static String fallbackName(ItemType itemId) {
    String[] words = itemId.name().split("_");
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (!result.isEmpty()) {
        result.append(' ');
      }
      result.append(word.charAt(0)).append(word.substring(1).toLowerCase());
    }
    return result.toString();
  }
}
