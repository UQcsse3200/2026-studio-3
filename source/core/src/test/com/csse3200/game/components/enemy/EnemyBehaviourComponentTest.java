package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyBehaviourComponentTest {

  // rollIntent 应该真正生成一个新意图（不再是构造时的 unknown 占位值）
  @Test
  void rollIntentShouldProduceRealIntentWhenStatsPresent() {
    Entity entity = new Entity();
    entity.addComponent(new EnemyStatsComponent(20, 6, 0));
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    entity.addComponent(behaviour);
    entity.create();

    EnemyIntent intent = behaviour.rollIntent();

    assertNotEquals(IntentType.UNKNOWN, intent.getType());
    assertEquals(intent, behaviour.getCurrentIntent());
  }

  // 广播的事件应该带上新决定的意图，而不是旧值
  @Test
  void rollIntentShouldTriggerIntentChangedWithNewIntent() {
    Entity entity = new Entity();
    entity.addComponent(new EnemyStatsComponent(20, 6, 0));
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    entity.addComponent(behaviour);
    entity.create();

    @SuppressWarnings("unchecked")
    EventListener1<EnemyIntent> listener = (EventListener1<EnemyIntent>) mock(EventListener1.class);
    entity.getEvents().addListener("intentChanged", listener);

    EnemyIntent intent = behaviour.rollIntent();

    verify(listener).handle(intent);
  }

  // 没有属性组件时不应该抛异常，意图保持 unknown
  @Test
  void rollIntentShouldNotThrowWhenStatsMissing() {
    Entity entity = new Entity();
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    entity.addComponent(behaviour);
    entity.create();

    EnemyIntent intent = behaviour.rollIntent();

    assertEquals(IntentType.UNKNOWN, intent.getType());
  }
}
