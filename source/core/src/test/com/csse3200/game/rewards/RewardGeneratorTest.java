package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Random;
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

    assertEquals(amountA, amountB);
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
  void itemRewardPoolIncludesOnlyPostBattleItems() {
    RewardGenerator generator = new RewardGenerator(new Random(42));
    EnumSet<ItemType> generated = EnumSet.noneOf(ItemType.class);

    for (int i = 0; i < 200; i++) {
      generated.add(generator.generateItemRewardOption().itemId);
    }

    EnumSet<ItemType> expected =
        EnumSet.of(
            ItemType.LUCKY_COIN,
            ItemType.ENERGY_CRYSTAL,
            ItemType.MERCHANTS_FAVOR,
            ItemType.IRON_AEGIS,
            ItemType.WARRIORS_CREST);

    assertEquals(expected, generated);
    assertFalse(generated.contains(ItemType.AMBROSIA_FLASK));
    assertTrue(generated.stream().noneMatch(ItemType::isCurse));
  }

  @Test
  void energyCrystalIsRarerThanOtherItems() {
    RewardGenerator generator = new RewardGenerator(new Random(42));
    EnumMap<ItemType, Integer> counts = new EnumMap<>(ItemType.class);
    for (ItemType type : ItemType.values()) {
      counts.put(type, 0);
    }

    int trials = 9000;
    for (int i = 0; i < trials; i++) {
      ItemType picked = generator.generateItemRewardOption().itemId;
      counts.merge(picked, 1, Integer::sum);
    }

    // Energy Crystal should land close to 1/9 of trials, noticeably less than the other
    // post-battle items, which should each land close to 2/9.
    int energyCrystalCount = counts.get(ItemType.ENERGY_CRYSTAL);
    for (ItemType type : ItemType.values()) {
      if (type == ItemType.ENERGY_CRYSTAL || type == ItemType.AMBROSIA_FLASK || type.isCurse()) {
        continue;
      }
      assertTrue(
          energyCrystalCount < counts.get(type),
          "Expected ENERGY_CRYSTAL to appear less often than " + type);
    }
  }
}
