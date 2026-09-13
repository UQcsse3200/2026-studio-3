package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ArmourSacrificeAITest {

  private EnemyStatsComponent newStats() {
    Entity entity = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(34, 7, 0);
    entity.addComponent(stats);
    entity.create();
    return stats;
  }

  // 第一回合应该给出"防御，获得4点护甲"的意图。
  // decideIntent 只负责决定意图，不直接改属性——真正加护甲是
  // EnemyBehaviourComponent.executeIntent 按这个意图的数值去做的。
  @Test
  void firstTurnShouldDecideToDefend() {
    ArmourSacrificeAI ai = new ArmourSacrificeAI();
    EnemyStatsComponent stats = newStats();

    EnemyIntent intent = ai.decideIntent(stats);

    assertEquals(IntentType.DEFEND, intent.getType());
    assertEquals(4, intent.getValue());
  }

  // 第二回合攻击应该把攒的护甲清零，全部换成额外伤害。
  // decideIntent 只负责"决定"，真正加护甲是 EnemyBehaviourComponent.executeIntent 做的，
  // 所以这里手动模拟一下第一回合防御被执行后的效果，再测第二回合。
  @Test
  void secondTurnShouldConvertArmourIntoBonusDamage() {
    ArmourSacrificeAI ai = new ArmourSacrificeAI();
    EnemyStatsComponent stats = newStats();

    EnemyIntent defendIntent = ai.decideIntent(stats); // 第一回合：防御
    stats.addArmour(defendIntent.getValue()); // 模拟防御意图被执行，护甲真正加上

    EnemyIntent attackIntent = ai.decideIntent(stats); // 第二回合：攻击

    assertEquals(IntentType.ATTACK, attackIntent.getType());
    assertEquals(7 + 4, attackIntent.getValue());
    assertEquals(0, stats.getArmour());
  }
}
