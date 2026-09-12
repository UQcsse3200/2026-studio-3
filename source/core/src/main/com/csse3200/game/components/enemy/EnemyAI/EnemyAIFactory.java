package com.csse3200.game.components.enemy.EnemyAI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates enemy AI implementations from behaviour identifiers. */
public final class EnemyAIFactory {
  private static final Logger logger = LoggerFactory.getLogger(EnemyAIFactory.class);
  public static final String CYCLE_ATTACK_DEFEND = "cycle_attack_defend";
  public static final String CYCLE_FOUR_STANCE = "cycle_four_stance";

  /** Behaviour used when a configuration names an unknown behaviour. */
  public static final String DEFAULT_BEHAVIOUR = CYCLE_ATTACK_DEFEND;

  /**
   * Creates a new enemy AI from its behaviour identifier.
   *
   * <p>An unknown or blank identifier falls back to {@link #DEFAULT_BEHAVIOUR} so that a
   * misconfigured enemy still fights instead of breaking the battle.
   *
   * @param behaviourId behaviour identifier loaded from enemy configuration
   * @return a new enemy AI instance, never null
   */
  public static EnemyAI create(String behaviourId) {
    if (behaviourId == null || behaviourId.isBlank()) {
      logger.warn("Blank enemy behaviour id, falling back to {}", DEFAULT_BEHAVIOUR);
      return new CycleAttackDefendAI();
    }

    return switch (behaviourId) {
      case CYCLE_ATTACK_DEFEND -> new CycleAttackDefendAI();
      case CYCLE_FOUR_STANCE -> new CycleFourStanceAI();
      default -> {
        logger.warn(
            "Unknown enemy behaviour id '{}', falling back to {}", behaviourId, DEFAULT_BEHAVIOUR);
        yield new CycleAttackDefendAI();
      }
    };
  }

  private EnemyAIFactory() {
    throw new IllegalStateException("Instantiating utility class");
  }
}
