package com.csse3200.game.components.enemy;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/**
 * Decides and telegraphs an enemy's action each round, then resolves it.
 *
 * <p>实际的决策逻辑委托给 {@link EnemyAI}（由 {@link EnemyAIFactory} 根据 behaviourId 创建），
 * 本类只负责在每回合调用它、保存结果并广播事件。
 */
public class EnemyBehaviourComponent extends Component {
  private final String behaviourId;
  private final EnemyAI ai;
  private EnemyIntent currentIntent = EnemyIntent.unknown();

  public EnemyBehaviourComponent(String behaviourId) {
    this.behaviourId = behaviourId;
    this.ai = EnemyAIFactory.create(behaviourId);
  }

  public String getBehaviourId() {
    return behaviourId;
  }

  /**
   * @return the intent telegraphed for the coming round
   */
  public EnemyIntent getCurrentIntent() {
    return currentIntent;
  }

  /**
   * Decides the action for the coming round and telegraphs it.
   *
   * <p>没有自身属性组件时安全跳过（保持 unknown 意图），不会抛异常。
   *
   * @return the newly decided intent
   */
  public EnemyIntent rollIntent() {
    EnemyStatsComponent stats = entity.getComponent(EnemyStatsComponent.class);
    if (stats != null) {
      currentIntent = ai.decideIntent(stats);
    }

    entity.getEvents().trigger("intentChanged", currentIntent);
    return currentIntent;
  }

  /**
   * Resolves the current intent against the given target.
   *
   * @param target the entity the intent acts on, typically the player
   */
  public void executeIntent(Entity target) {
    switch (currentIntent.getType()) {
      case ATTACK -> attack(target);
      case DEFEND -> defend();
      default -> {
        // BUFF, DEBUFF and UNKNOWN are handled in #20.
      }
    }
  }

  private void attack(Entity target) {
    if (target == null) {
      return;
    }
    EnemyStatsComponent targetStats = target.getComponent(EnemyStatsComponent.class);
    if (targetStats != null) {
      targetStats.takeDamage(currentIntent.getValue());
    }
  }

  private void defend() {
    EnemyStatsComponent stats = entity.getComponent(EnemyStatsComponent.class);
    if (stats != null) {
      stats.addArmour(currentIntent.getValue());
    }
  }
}
