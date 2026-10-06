package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class RewardOptionTest {
  @Test
  void factoriesCreateOnlyConsistentRewardShapes() {
    RewardOption gold = RewardOption.gold(25);
    RewardOption item = RewardOption.item(ItemType.ENERGY_CRYSTAL);
    CardRewardSelection selection = new CardRewardSelection(List.of("strike", "defend"));
    RewardOption cards = RewardOption.cards(selection);

    assertEquals(RewardType.GOLD, gold.type);
    assertEquals(25, gold.goldAmount);
    assertNull(gold.itemId);
    assertNull(gold.cardSelection);

    assertEquals(RewardType.ITEM, item.type);
    assertEquals(ItemType.ENERGY_CRYSTAL, item.itemId);
    assertEquals(0, item.goldAmount);
    assertNull(item.cardSelection);

    assertEquals(RewardType.CARD, cards.type);
    assertEquals(selection, cards.cardSelection);
    assertEquals(0, cards.goldAmount);
    assertNull(cards.itemId);
  }

  @Test
  void factoriesRejectMissingOrInvalidPayloads() {
    assertThrows(IllegalArgumentException.class, () -> RewardOption.gold(-1));
    assertThrows(IllegalArgumentException.class, () -> RewardOption.item(null));
    assertThrows(IllegalArgumentException.class, () -> RewardOption.cards(null));
  }
}
