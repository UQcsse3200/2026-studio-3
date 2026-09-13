package com.csse3200.game.components.enemy;

/**
 * A pluggable behaviour strategy that decides what an enemy does each round.
 *
 * <p>实现类只依据敌人自身的属性和内部回合状态做决策，不感知目标（玩家）的状态；对目标造成的实际 效果由 {@link
 * EnemyBehaviourComponent#executeIntent} 处理。每个敌人实例应持有独立的 {@link EnemyAI} 实例，避免多个敌人共享内部状态（如回合计数）。
 */
public interface EnemyAI {

  /**
   * Decides the intent for the coming round.
   *
   * @param self the deciding enemy's own stats
   * @return the intent to telegraph and later resolve
   */
  EnemyIntent decideIntent(EnemyStatsComponent self);
}
