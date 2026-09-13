package com.csse3200.game.components.enemy;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an {@link EnemyAI} instance from an {@link com.csse3200.game.entities.configs.EnemyConfig}
 * behaviour id.
 *
 * <p>打法工厂：根据 {@code enemies.json} 里配置的 {@code behaviour} 字符串，
 * 造出对应的"打法脑子"实例。未识别或缺失的 id 会记录警告并回退到基础的攻防交替打法，
 * 保证游戏不会因为配置错误而崩溃。
 */
public final class EnemyAIFactory {
  private static final Logger logger = LoggerFactory.getLogger(EnemyAIFactory.class);

  public static final String CYCLE_ATTACK_DEFEND = "cycle_attack_defend";
  public static final String CYCLE_FOUR_STANCE = "cycle_four_stance";
  public static final String ARMOUR_SACRIFICE = "armour_sacrifice";
  public static final String ERRATIC = "erratic";

  /**
   * Resolves a behaviour id to a fresh {@link EnemyAI} instance.
   *
   * @param behaviourId the behaviour id from {@link com.csse3200.game.entities.configs.EnemyConfig}
   * @return a new, independent AI instance for a single enemy
   */
  public static EnemyAI create(String behaviourId) {
    if (behaviourId == null) {
      logger.warn("Null enemy behaviour id, falling back to {}", CYCLE_ATTACK_DEFEND);
      return new CycleAttackDefendAI();
    }

    return switch (behaviourId) {
      case CYCLE_FOUR_STANCE -> new CycleFourStanceAI();
      case ARMOUR_SACRIFICE -> new ArmourSacrificeAI();
      case ERRATIC -> new RandomStanceAI(new Random());
      case CYCLE_ATTACK_DEFEND -> new CycleAttackDefendAI();
      default -> {
        logger.warn("Unknown enemy behaviour id: {}, falling back to {}", behaviourId, CYCLE_ATTACK_DEFEND);
        yield new CycleAttackDefendAI();
      }
    };
  }

  private EnemyAIFactory() {
    throw new IllegalStateException("Instantiating utility class");
  }
}
