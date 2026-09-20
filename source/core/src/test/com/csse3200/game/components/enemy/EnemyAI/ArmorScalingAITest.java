package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class ArmorScalingAITest {
  private static final int BASE_ATTACK = 7;

  private final ArmorScalingAI ai = new ArmorScalingAI();

  @Test
  void shouldDefendOnOddTurns() {
    EnemyIntent intent = ai.decide(createContext(1, 0));

    assertEquals(IntentType.DEFEND, intent.getType());
  }

  @Test
  void shouldAttackForBaseDamagePlusHeldArmorOnEvenTurns() {
    EnemyIntent intent = ai.decide(createContext(2, 4));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BASE_ATTACK + 4, intent.getValue());
  }

  @Test
  void shouldAttackForBaseDamageWhenNoArmorIsBanked() {
    EnemyIntent intent = ai.decide(createContext(2, 0));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BASE_ATTACK, intent.getValue());
  }

  @Test
  void shouldRepeatTheCycleOnLaterTurns() {
    assertEquals(IntentType.DEFEND, ai.decide(createContext(3, 4)).getType());
    assertEquals(IntentType.ATTACK, ai.decide(createContext(4, 8)).getType());
  }

  @Test
  void shouldHitHarderAsArmorAccumulates() {
    int earlyDamage = ai.decide(createContext(2, 4)).getValue();
    int laterDamage = ai.decide(createContext(4, 8)).getValue();

    assertEquals(BASE_ATTACK + 4, earlyDamage);
    assertEquals(BASE_ATTACK + 8, laterDamage);
  }

  @Test
  void shouldRejectNullContext() {
    assertThrows(NullPointerException.class, () -> ai.decide(null));
  }

  private EnemyAIContext createContext(int turnNumber, int armor) {
    return new EnemyAIContext(30, 34, 34, BASE_ATTACK, armor, EnemyIntent.unknown(), turnNumber);
  }
}
