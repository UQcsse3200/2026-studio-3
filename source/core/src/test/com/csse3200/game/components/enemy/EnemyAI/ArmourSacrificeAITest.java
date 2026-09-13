package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class ArmourSacrificeAITest {
  private final ArmourSacrificeAI ai = new ArmourSacrificeAI();

  @Test
  void shouldDefendOnOddTurns() {
    EnemyIntent intent = ai.decide(createContext(1, 0));

    assertEquals(IntentType.DEFEND, intent.getType());
  }

  // 攻击回合的伤害应该是基础攻击力加上当前护甲值——"用防御换输出"
  @Test
  void shouldAttackWithArmourBonusOnEvenTurns() {
    EnemyIntent intent = ai.decide(createContext(2, 4));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(7 + 4, intent.getValue());
  }

  @Test
  void shouldAttackWithNoBonusWhenNoArmourBanked() {
    EnemyIntent intent = ai.decide(createContext(2, 0));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(7, intent.getValue());
  }

  private EnemyAIContext createContext(int turnNumber, int armor) {
    return new EnemyAIContext(30, 34, 34, 7, armor, EnemyIntent.unknown(), turnNumber);
  }
}
