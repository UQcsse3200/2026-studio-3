package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ItemFormattingTest {

    @Test
    void formatsMultiWordNameWithUnderscores() {
        assertEquals("Energy Crystal", ItemFormatting.formatItemName(ItemType.ENERGY_CRYSTAL));
    }

    @Test
    void formatsThreeOrMoreUnderscoreSeparatedWords() {
        assertEquals("Lucky Coin", ItemFormatting.formatItemName(ItemType.LUCKY_COIN));
    }

    @Test
    void formatsMerchantsFavor() {
        assertEquals("Merchants Favor", ItemFormatting.formatItemName(ItemType.MERCHANTS_FAVOR));
    }
}