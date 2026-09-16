package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentEffectType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Boss AI controlled by a phase state machine, per-phase action weights and a global pressure
 * level.
 */
public class BossAI implements EnemyAI {
  private static final int HIGH_ARMOR_THRESHOLD = 8;

  /** Magnitude passed with a SILENCE effect; the effect itself is a simple on/off block. */
  private static final int SILENCE_VALUE = 1;

  /** Player turns a single SILENCE lasts for. */
  private static final int SILENCE_DURATION = 2;

  /** Health the player loses for each card played while the effect is active. */
  private static final int CARD_PLAY_DAMAGE = 3;

  /** Player turns a single damage-on-card-play effect lasts for. */
  private static final int CARD_PLAY_DAMAGE_DURATION = 3;

  /**
   * Turns the Boss must wait between debuffs.
   *
   * <p>{@code CombatStatsComponent.applyStatusEffect} overwrites an effect of the same type, so
   * re-applying every turn would refresh the duration indefinitely and lock the player out of
   * playing cards for the rest of the fight. The Boss cannot read the player's active effects from
   * {@link EnemyAIContext}, so the cooldown is tracked here instead.
   */
  private static final int DEBUFF_COOLDOWN_TURNS = 3;

  private final Random random;

  private BossPhase currentPhase = BossPhase.PHASE_ONE;
  private BossMove previousMove;
  private int consecutiveAttacks;
  private int debuffCooldownRemaining;

  /** Creates a Boss AI using normal runtime randomness. */
  public BossAI() {
    this(new Random());
  }

  /**
   * Creates a Boss AI with an injected random source.
   *
   * <p>This constructor allows unit tests to use a fixed random seed.
   *
   * @param random random source used for weighted decisions
   */
  BossAI(Random random) {
    this.random = Objects.requireNonNull(random, "random cannot be null");
  }

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    updatePhase(context);
    tickDebuffCooldown();

    PressureLevel pressure = calculatePressure(context);
    EnumMap<BossMove, Integer> weights = getBaseWeights(currentPhase);

    applyPressure(weights, pressure);
    applyConstraints(weights, context);

    BossMove selectedMove = selectWeightedMove(weights);
    recordMove(selectedMove);

    return createIntent(selectedMove, context);
  }

  /**
   * Updates the high-level phase using the Boss's remaining health.
   *
   * <p>Phase transitions only move forward. Healing the Boss does not return it to an earlier
   * phase.
   */
  private void updatePhase(EnemyAIContext context) {
    float healthRatio = context.getEnemyHealthRatio();

    BossPhase desiredPhase;
    if (healthRatio <= 0.30f) {
      desiredPhase = BossPhase.ENRAGED;
    } else if (healthRatio <= 0.65f) {
      desiredPhase = BossPhase.PHASE_TWO;
    } else {
      desiredPhase = BossPhase.PHASE_ONE;
    }

    if (desiredPhase.ordinal() > currentPhase.ordinal()) {
      currentPhase = desiredPhase;
    }
  }

  /** Counts down the debuff cooldown once per decision. */
  private void tickDebuffCooldown() {
    if (debuffCooldownRemaining > 0) {
      debuffCooldownRemaining--;
    }
  }

  /** Calculates the global pressure level using the current battle state. */
  private PressureLevel calculatePressure(EnemyAIContext context) {
    int pressure = 0;

    // Long battles gradually make the Boss more aggressive.
    pressure += Math.min(30, Math.max(0, context.getTurnNumber() - 1) * 3);

    // Losing health increases the Boss's pressure.
    pressure += Math.round((1f - context.getEnemyHealthRatio()) * 40f);

    // A weakened player encourages the Boss to attack.
    if (context.getPlayerHealth() > 0) {
      if (context.getPlayerHealth() <= context.getEnemyAttack()) {
        pressure += 30;
      } else if (context.getPlayerHealth() <= context.getEnemyAttack() * 2) {
        pressure += 15;
      }
    }

    if (pressure >= 70) {
      return PressureLevel.CRITICAL;
    }
    if (pressure >= 45) {
      return PressureLevel.HIGH;
    }
    if (pressure >= 20) {
      return PressureLevel.MEDIUM;
    }
    return PressureLevel.LOW;
  }

  /** Returns a fresh attack/defend weight table for the given Boss phase. */
  private EnumMap<BossMove, Integer> getBaseWeights(BossPhase phase) {
    EnumMap<BossMove, Integer> weights = new EnumMap<>(BossMove.class);

    switch (phase) {
      case PHASE_ONE -> {
        weights.put(BossMove.ATTACK, 45);
        weights.put(BossMove.DEFEND, 55);
        weights.put(BossMove.SILENCE, 0);
        weights.put(BossMove.DAMAGE_ON_CARD_PLAY, 0);
      }

      case PHASE_TWO -> {
        weights.put(BossMove.ATTACK, 65);
        weights.put(BossMove.DEFEND, 35);
        weights.put(BossMove.SILENCE, 20);
        weights.put(BossMove.DAMAGE_ON_CARD_PLAY, 10);
      }

      case ENRAGED -> {
        weights.put(BossMove.ATTACK, 90);
        weights.put(BossMove.DEFEND, 10);
        weights.put(BossMove.SILENCE, 15);
        weights.put(BossMove.DAMAGE_ON_CARD_PLAY, 25);
      }
    }

    return weights;
  }

  /**
   * Modifies the current phase table according to the global pressure level.
   *
   * <p>Pressure moves weight between attacking and defending only. Debuff weights are set by the
   * phase and by {@link #applyConstraints}, so that rising pressure cannot turn the Boss into a
   * permanent debuff machine.
   */
  private void applyPressure(EnumMap<BossMove, Integer> weights, PressureLevel pressure) {
    switch (pressure) {
      case LOW -> {
        adjustWeight(weights, BossMove.ATTACK, -10);
        adjustWeight(weights, BossMove.DEFEND, 10);
      }

      case MEDIUM -> {
        // Medium pressure uses the phase's original weights.
      }

      case HIGH -> {
        adjustWeight(weights, BossMove.ATTACK, 15);
        adjustWeight(weights, BossMove.DEFEND, -15);
      }

      case CRITICAL -> {
        adjustWeight(weights, BossMove.ATTACK, 25);
        adjustWeight(weights, BossMove.DEFEND, -25);
      }
    }
  }

  /** Applies restrictions that override undesirable random behaviour. */
  private void applyConstraints(EnumMap<BossMove, Integer> weights, EnemyAIContext context) {
    // Do not keep defending when the Boss already has substantial armor.
    if (context.getEnemyArmor() >= HIGH_ARMOR_THRESHOLD) {
      weights.put(BossMove.DEFEND, 0);
    }

    // Reduce the chance of performing two defence actions in a row.
    if (previousMove == BossMove.DEFEND) {
      int defendWeight = weights.getOrDefault(BossMove.DEFEND, 0);
      weights.put(BossMove.DEFEND, defendWeight / 4);
    }

    // After repeated attacks, slightly encourage the Boss to vary its action.
    if (consecutiveAttacks >= 2) {
      adjustWeight(weights, BossMove.ATTACK, -20);
      adjustWeight(weights, BossMove.DEFEND, 20);
    }
    // Hold off on a second debuff until the first has had time to expire.
    if (debuffCooldownRemaining > 0) {
      for (BossMove move : BossMove.values()) {
        if (move.isDebuff()) {
          weights.put(move, 0);
        }
      }
    }
    ensureAvailableMove(weights);
  }

  /** Selects one move using the final calculated weights. */
  private BossMove selectWeightedMove(EnumMap<BossMove, Integer> weights) {
    int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();

    if (totalWeight <= 0) {
      return BossMove.ATTACK;
    }

    int roll = random.nextInt(totalWeight);

    for (Map.Entry<BossMove, Integer> entry : weights.entrySet()) {
      roll -= entry.getValue();

      if (roll < 0) {
        return entry.getKey();
      }
    }

    return BossMove.ATTACK;
  }

  /** Converts an internal Boss move into an intent understood by the existing battle system. */
  private EnemyIntent createIntent(BossMove move, EnemyAIContext context) {
    return switch (move) {
      case ATTACK -> EnemyIntent.attack(context.getEnemyAttack());
      case DEFEND -> EnemyIntent.defend(getDefendAmount(currentPhase));
      case SILENCE -> EnemyIntent.debuff(IntentEffectType.SILENCE, SILENCE_VALUE, SILENCE_DURATION);
      case DAMAGE_ON_CARD_PLAY ->
          EnemyIntent.debuff(
              IntentEffectType.DAMAGE_ON_CARD_PLAY, CARD_PLAY_DAMAGE, CARD_PLAY_DAMAGE_DURATION);
    };
  }

  /** Returns stronger defence values in later phases. */
  private int getDefendAmount(BossPhase phase) {
    return switch (phase) {
      case PHASE_ONE -> 4;
      case PHASE_TWO -> 6;
      case ENRAGED -> 8;
    };
  }

  /** Records the selected move for the next decision. */
  private void recordMove(BossMove selectedMove) {
    previousMove = selectedMove;

    if (selectedMove == BossMove.ATTACK) {
      consecutiveAttacks++;
    } else {
      consecutiveAttacks = 0;
    }
    if (selectedMove.isDebuff()) {
      debuffCooldownRemaining = DEBUFF_COOLDOWN_TURNS;
    }
  }

  /** Safely increases or decreases one action's weight. */
  private void adjustWeight(EnumMap<BossMove, Integer> weights, BossMove move, int adjustment) {
    int currentWeight = weights.getOrDefault(move, 0);
    weights.put(move, Math.max(0, currentWeight + adjustment));
  }

  /** Guarantees that malformed weights cannot leave the Boss without an action. */
  private void ensureAvailableMove(EnumMap<BossMove, Integer> weights) {
    boolean hasAvailableMove = weights.values().stream().anyMatch(weight -> weight > 0);

    if (!hasAvailableMove) {
      weights.put(BossMove.ATTACK, 1);
    }
  }

  public BossPhase getCurrentPhase() {
    return currentPhase;
  }

  public BossMove getPreviousMove() {
    return previousMove;
  }

  public int getConsecutiveAttacks() {
    return consecutiveAttacks;
  }

  /**
   * @return turns remaining before the Boss may apply another debuff
   */
  public int getDebuffCooldownRemaining() {
    return debuffCooldownRemaining;
  }

  /** High-level states of the Boss fight. */
  public enum BossPhase {
    PHASE_ONE,
    PHASE_TWO,
    ENRAGED
  }

  /** Actions currently supported by the battle execution system. */
  public enum BossMove {
    ATTACK,
    DEFEND,
    SILENCE,
    DAMAGE_ON_CARD_PLAY;

    /**
     * @return true if this move inflicts a status effect on the player
     */
    boolean isDebuff() {
      return this == SILENCE || this == DAMAGE_ON_CARD_PLAY;
    }
  }

  /** Global aggression level applied on top of the current phase table. */
  public enum PressureLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}
