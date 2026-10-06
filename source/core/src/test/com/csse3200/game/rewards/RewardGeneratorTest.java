package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardGeneratorTest {

  @Test
  void generatedGoldShouldAlwaysBeWithinRange() {
    RewardGenerator generator = new RewardGenerator(new Random(42));
    for (int i = 0; i < 100; i++) {
      int base = generator.generateGoldOption().getBaseAmount();
      assertTrue(base >= 20 && base <= 30);
    }
  }

  @Test
  void sameSeedShouldProduceReproducibleResults() {
    RewardGenerator generatorA = new RewardGenerator(new Random(123));
    RewardGenerator generatorB = new RewardGenerator(new Random(123));

    int amountA = generatorA.generateGoldOption().getBaseAmount();
    int amountB = generatorB.generateGoldOption().getBaseAmount();

    assertTrue(amountA == amountB);
  }

  @Test
  void shouldCreateConsistentTypedGoldAndItemOptions() {
    RewardGenerator generator = new RewardGenerator(new Random(7));

    RewardOption gold = generator.generateGoldRewardOption(0f);
    RewardOption item = generator.generateItemRewardOption();

    assertEquals(RewardType.GOLD, gold.type);
    assertTrue(gold.goldAmount >= 20 && gold.goldAmount <= 30);
    assertNull(gold.itemId);
    assertNull(gold.cardSelection);
    assertEquals(RewardType.ITEM, item.type);
    assertNotNull(item.itemId);
    assertNull(item.cardSelection);
  }

  @Test
  void canOfferEveryConfiguredItemTypeAsAnItemReward() {
    RewardGenerator generator = new RewardGenerator(new Random(17));
    Set<ItemType> offeredItems = EnumSet.noneOf(ItemType.class);

    for (int i = 0; i < 100; i++) {
      offeredItems.add(generator.generateItemRewardOption().itemId);
    }

    assertEquals(EnumSet.allOf(ItemType.class), offeredItems);
  }
}
