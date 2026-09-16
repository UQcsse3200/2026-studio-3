package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardGeneratorTest {

  @Test
  void generatedGoldShouldAlwaysBeWithinRange() {
    // 白盒测试：固定seed确保可复现，验证内部随机路径的边界
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
}
