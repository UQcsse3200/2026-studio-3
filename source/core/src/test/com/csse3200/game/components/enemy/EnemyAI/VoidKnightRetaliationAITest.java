package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class VoidKnightRetaliationAITest {
  private static final int BASE_ATTACK = 10;
  private static final int RESTORED_ARMOUR = 5;

  @Test
  void shouldAttackNormallyWhileArmourRemains() {
    VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

    EnemyIntent intent = ai.decide(createContext(72, 72, 5, 1));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BASE_ATTACK, intent.getValue());
  }

  @Test
  void shouldRetaliateWhenArmourIsBroken() {
    VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

    EnemyIntent intent = ai.decide(createContext(72, 72, 0, 2));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BASE_ATTACK * 2, intent.getValue());
  }

  @Test
  void shouldRestoreArmourAfterRetaliation() {
    VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

    EnemyIntent retaliation = ai.decide(createContext(72, 72, 0, 2));

    EnemyIntent restoration = ai.decide(createContext(72, 72, 0, 3));

    assertEquals(IntentType.ATTACK, retaliation.getType());
    assertEquals(BASE_ATTACK * 2, retaliation.getValue());

    assertEquals(IntentType.DEFEND, restoration.getType());
    assertEquals(RESTORED_ARMOUR, restoration.getValue());
  }

  @Test
  void shouldReturnToNormalAttackAfterRestoringArmour() {
    VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

    EnemyIntent retaliation = ai.decide(createContext(72, 72, 0, 2));

    EnemyIntent restoration = ai.decide(createContext(72, 72, 0, 3));

    EnemyIntent normalAttack = ai.decide(createContext(72, 72, 5, 4));

    assertEquals(EnemyIntent.attack(BASE_ATTACK * 2), retaliation);

    assertEquals(EnemyIntent.defend(RESTORED_ARMOUR), restoration);

    assertEquals(EnemyIntent.attack(BASE_ATTACK), normalAttack);
  }

  @Test
  void shouldRepeatMechanicAfterArmourIsBrokenAgain() {
    VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

    // 第一次破盾
    assertEquals(EnemyIntent.attack(BASE_ATTACK * 2), ai.decide(createContext(72, 72, 0, 1)));

    // 恢复盾
    assertEquals(EnemyIntent.defend(RESTORED_ARMOUR), ai.decide(createContext(72, 72, 0, 2)));

    // 有盾，正常攻击
    assertEquals(EnemyIntent.attack(BASE_ATTACK), ai.decide(createContext(72, 72, 5, 3)));

    // 再次破盾
    assertEquals(EnemyIntent.attack(BASE_ATTACK * 2), ai.decide(createContext(72, 72, 0, 4)));

    // 再次恢复盾
    assertEquals(EnemyIntent.defend(RESTORED_ARMOUR), ai.decide(createContext(72, 72, 0, 5)));
  }

  private EnemyAIContext createContext(int health, int maxHealth, int armour, int turnNumber) {
    return new EnemyAIContext(
        100, health, maxHealth, BASE_ATTACK, armour, EnemyIntent.unknown(), turnNumber);
  }
}
