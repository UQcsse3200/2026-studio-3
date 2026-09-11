package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class EnemyAIFactoryTest {
  @Test
  void shouldCreateCycleAttackDefendAI() {
    EnemyAI ai = EnemyAIFactory.create(EnemyAIFactory.CYCLE_ATTACK_DEFEND);

    assertInstanceOf(CycleAttackDefendAI.class, ai);
  }

  @Test
  void shouldCreateCycleFourStanceAI() {
    EnemyAI ai = EnemyAIFactory.create(EnemyAIFactory.CYCLE_FOUR_STANCE);

    assertInstanceOf(CycleFourStanceAI.class, ai);
  }

  @Test
  void shouldCreateSeparateInstances() {
    EnemyAI first = EnemyAIFactory.create(EnemyAIFactory.CYCLE_ATTACK_DEFEND);

    EnemyAI second = EnemyAIFactory.create(EnemyAIFactory.CYCLE_ATTACK_DEFEND);

    assertNotSame(first, second);
  }

  @Test
  void shouldFallBackForUnknownBehaviour() {
    EnemyAI ai = EnemyAIFactory.create("unknown_ai");

    assertInstanceOf(CycleAttackDefendAI.class, ai);
  }

  @Test
  void shouldFallBackForNullBehaviour() {
    EnemyAI ai = EnemyAIFactory.create(null);

    assertInstanceOf(CycleAttackDefendAI.class, ai);
  }

  @Test
  void shouldFallBackForBlankBehaviour() {
    EnemyAI ai = EnemyAIFactory.create("   ");

    assertInstanceOf(CycleAttackDefendAI.class, ai);
  }

  @Test
  void shouldCreateEnrageLowHealthAI() {
    EnemyAI ai =
            EnemyAIFactory.create(EnemyAIFactory.ENRAGE_LOW_HEALTH);

    assertInstanceOf(EnrageLowHealthAI.class, ai);
  }

  @Test
  void shouldCreateVoidKnightRetaliationAI() {
    EnemyAI ai =
            EnemyAIFactory.create(
                    EnemyAIFactory.VOID_KNIGHT_RETALIATION);

    assertInstanceOf(VoidKnightRetaliationAI.class, ai);
  }

  @Test
  void shouldCreateSeparateVoidKnightAIInstances() {
    EnemyAI first =
            EnemyAIFactory.create(
                    EnemyAIFactory.VOID_KNIGHT_RETALIATION);

    EnemyAI second =
            EnemyAIFactory.create(
                    EnemyAIFactory.VOID_KNIGHT_RETALIATION);

    assertNotSame(first, second);
  }
}
