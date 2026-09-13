package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class EnemyAIFactoryTest {

  // 每个已注册的 behaviour id 都应该造出对应的实现类
  @Test
  void shouldCreateCycleAttackDefendAI() {
    assertInstanceOf(CycleAttackDefendAI.class, EnemyAIFactory.create(EnemyAIFactory.CYCLE_ATTACK_DEFEND));
  }

  @Test
  void shouldCreateCycleFourStanceAI() {
    assertInstanceOf(CycleFourStanceAI.class, EnemyAIFactory.create(EnemyAIFactory.CYCLE_FOUR_STANCE));
  }

  @Test
  void shouldCreateArmourSacrificeAI() {
    assertInstanceOf(ArmourSacrificeAI.class, EnemyAIFactory.create(EnemyAIFactory.ARMOUR_SACRIFICE));
  }

  @Test
  void shouldCreateRandomStanceAI() {
    assertInstanceOf(RandomStanceAI.class, EnemyAIFactory.create(EnemyAIFactory.ERRATIC));
  }

  // 未知或缺失的 id 都应该安全回退，而不是抛异常
  @Test
  void shouldFallBackToCycleAttackDefendForUnknownId() {
    assertInstanceOf(CycleAttackDefendAI.class, EnemyAIFactory.create("not_a_real_behaviour"));
  }

  @Test
  void shouldFallBackToCycleAttackDefendForNullId() {
    assertInstanceOf(CycleAttackDefendAI.class, EnemyAIFactory.create(null));
  }
}
