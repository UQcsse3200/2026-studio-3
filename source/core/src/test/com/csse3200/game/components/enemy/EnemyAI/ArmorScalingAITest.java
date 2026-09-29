package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import com.csse3200.game.components.enemy.PlayerMemory;
import org.junit.jupiter.api.Test;

class ArmourScalingAITest {
  private static final int BASE_ATTACK = 7;

  private final ArmourScalingAI ai = new ArmourScalingAI();

  @Test
  void shouldDefendOnOddTurns() {
    EnemyIntent intent = ai.decide(createContext(1, 0));

    assertEquals(IntentType.DEFEND, intent.getType());
  }

  @Test
  void shouldAttackForBaseDamagePlusHeldArmourOnEvenTurns() {
    EnemyIntent intent = ai.decide(createContext(2, 4));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BASE_ATTACK + 4, intent.getValue());
  }

  @Test
  void shouldAttackForBaseDamageWhenNoArmourIsBanked() {
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
  void shouldHitHarderAsArmourAccumulates() {
    int earlyDamage = ai.decide(createContext(2, 4)).getValue();
    int laterDamage = ai.decide(createContext(4, 8)).getValue();

    assertEquals(BASE_ATTACK + 4, earlyDamage);
    assertEquals(BASE_ATTACK + 8, laterDamage);
  }

  @Test
  void shouldRejectNullContext() {
    assertThrows(NullPointerException.class, () -> ai.decide(null));
  }

  private EnemyAIContext createContext(int turnNumber, int armour) {
    return createContext(turnNumber, armour, PlayerMemory.empty());
  }

  private EnemyAIContext createContext(int turnNumber, int armour, PlayerMemory playerMemory) {
    return new EnemyAIContext(
        30, 34, 34, BASE_ATTACK, armour, EnemyIntent.unknown(), turnNumber, playerMemory);
  }

  @Test
  void shouldDefendMoreOftenWhenPlayerFavoursAttack() {
    PlayerMemory aggressiveMemory = new PlayerMemory(4, 0, 0, 0);

    EnemyIntent secondTurnIntent = ai.decide(createContext(2, 4, aggressiveMemory));

    assertEquals(IntentType.DEFEND, secondTurnIntent.getType());
  }

  @Test
  void shouldAttackOnThirdTurnWhenPlayerFavoursAttack() {
    PlayerMemory aggressiveMemory = new PlayerMemory(4, 0, 0, 0);

    EnemyIntent thirdTurnIntent = ai.decide(createContext(3, 8, aggressiveMemory));

    assertEquals(IntentType.ATTACK, thirdTurnIntent.getType());

    assertEquals(BASE_ATTACK + 8, thirdTurnIntent.getValue());
  }

  @Test
  void shouldRepeatReactiveThreeTurnCycle() {
    PlayerMemory aggressiveMemory = new PlayerMemory(6, 1, 0, 0);

    assertEquals(IntentType.DEFEND, ai.decide(createContext(4, 8, aggressiveMemory)).getType());

    assertEquals(IntentType.DEFEND, ai.decide(createContext(5, 12, aggressiveMemory)).getType());

    assertEquals(IntentType.ATTACK, ai.decide(createContext(6, 12, aggressiveMemory)).getType());
  }

  @Test
  void shouldKeepNormalCycleBelowAttackPreferenceThreshold() {
    PlayerMemory insufficientMemory = new PlayerMemory(3, 0, 0, 0);

    EnemyIntent intent = ai.decide(createContext(2, 4, insufficientMemory));

    assertEquals(IntentType.ATTACK, intent.getType());
  }
}
