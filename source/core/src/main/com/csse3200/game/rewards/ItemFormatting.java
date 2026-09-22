package com.csse3200.game.rewards;

/**
 * Shared display-name formatting for {@link ItemType}, used anywhere an item name is shown to the
 * player (reward selection, inventory display, etc). Converts an enum constant like
 * {@code LUCKY_COIN} into a readable "Lucky Coin".
 */
public final class ItemFormatting {

    private ItemFormatting() {
        // Utility class — not instantiable.
    }

    /**
     * Formats an item type's enum name into a human-readable display name, e.g. {@code
     * ENERGY_CRYSTAL} becomes {@code "Energy Crystal"}.
     *
     * @param itemId the item type to format
     * @return a readable, title-cased name with underscores replaced by spaces
     */
    public static String formatItemName(ItemType itemId) {
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