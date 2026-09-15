package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;
import java.util.Random;

/**
 * Unpredictable behaviour for Lesser Shade: each round randomly attacks or defends.
 *
 * <p>暗影小怪（Lesser Shade）的打法：每回合像抛硬币一样随机决定攻击还是防御，让玩家猜不到下一步。
 */
public class RandomStanceAI implements EnemyAI {
  private static final double ATTACK_CHANCE = 0.6;
  private static final int DEFEND_AMOUNT = 3;

  private final Random random;

  /** Creates a Random Stance AI using normal runtime randomness. */
  public RandomStanceAI() {
    this(new Random());
  }

  /**
   * Creates a Random Stance AI with an injected random source.
   *
   * <p>This constructor allows unit tests to use a fixed random seed or a mock.
   *
   * @param random random source used for the attack/defend roll
   */
  RandomStanceAI(Random random) {
    this.random = Objects.requireNonNull(random, "random cannot be null");
  }

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    return (random.nextDouble() < ATTACK_CHANCE)
        ? EnemyIntent.attack(context.getEnemyAttack())
        : EnemyIntent.defend(DEFEND_AMOUNT);
  }
}
