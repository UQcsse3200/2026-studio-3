package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;

/** * Determines the next intent for an enemy based on the current battle state. */
public interface EnemyAI {
  /**
   * * Selects the enemy's next intent. * * @param context read-only information about the current
   * battle state * @return the selected enemy intent
   */
  EnemyIntent decide(EnemyAIContext context);
}
