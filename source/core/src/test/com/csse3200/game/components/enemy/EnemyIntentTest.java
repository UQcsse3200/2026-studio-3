package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnemyIntentTest {

  @Test
  void shouldCreateAnAttackIntent() {
    EnemyIntent intent = EnemyIntent.attack(7);

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(7, intent.getValue());
  }

  @Test
  void shouldCreateADefendIntent() {
    EnemyIntent intent = EnemyIntent.defend(3);

    assertEquals(IntentType.DEFEND, intent.getType());
    assertEquals(3, intent.getValue());
  }

  @Test
  void shouldCreateAnUnknownIntentWithNoValue() {
    EnemyIntent intent = EnemyIntent.unknown();

    assertEquals(IntentType.UNKNOWN, intent.getType());
    assertEquals(0, intent.getValue());
  }

  @Test
  void shouldEqualItself() {
    EnemyIntent intent = EnemyIntent.attack(5);

    assertEquals(intent, intent);
  }

  @Test
  void shouldEqualAnIntentWithTheSameTypeAndValue() {
    assertEquals(EnemyIntent.attack(5), EnemyIntent.attack(5));
  }

  @Test
  void shouldNotEqualAnIntentWithADifferentValue() {
    assertNotEquals(EnemyIntent.attack(5), EnemyIntent.attack(6));
  }

  @Test
  void shouldNotEqualAnIntentWithADifferentType() {
    assertNotEquals(EnemyIntent.attack(5), EnemyIntent.defend(5));
  }

  @Test
  void shouldNotEqualAnObjectOfAnotherType() {
    assertNotEquals(EnemyIntent.attack(5), "ATTACK(5)");
  }

  @Test
  void shouldNotEqualNull() {
    assertNotEquals(null, EnemyIntent.attack(5));
  }

  @Test
  void shouldShareAHashCodeWithAnEqualIntent() {
    assertEquals(EnemyIntent.attack(5).hashCode(), EnemyIntent.attack(5).hashCode());
  }

  @Test
  void shouldDescribeItsTypeAndValue() {
    assertTrue(EnemyIntent.attack(5).toString().contains("ATTACK"));
    assertTrue(EnemyIntent.attack(5).toString().contains("5"));
  }
}
