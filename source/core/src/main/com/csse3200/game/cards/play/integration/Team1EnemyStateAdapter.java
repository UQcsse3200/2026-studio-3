package com.csse3200.game.cards.play.integration;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.cards.play.EnemyStateView;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts Team 1 enemy entities to Team 5's read-only resolution view and result consumer.
 *
 * <p>Target IDs remain owned by the battle flow. The same IDs supplied by Team 3 in {@link
 * CardPlayTarget} must be registered with this adapter.
 */
public final class Team1EnemyStateAdapter implements EnemyStateView, EnemyEffectConsumer {
  private final Map<String, Entity> enemiesByTargetId = new LinkedHashMap<>();

  public Team1EnemyStateAdapter(Map<String, Entity> enemiesByTargetId) {
    if (enemiesByTargetId == null) {
      throw new IllegalArgumentException("Enemy target map cannot be null");
    }
    enemiesByTargetId.forEach(this::registerTarget);
  }

  /** Registers the opaque target ID used by Team 3 for one Team 1 enemy entity. */
  public void registerTarget(String targetId, Entity enemy) {
    validateTargetId(targetId);
    requireCombatStats(enemy, targetId);
    enemiesByTargetId.put(targetId, enemy);
  }

  /** Removes an enemy from future target validation and all-enemy effect application. */
  public void unregisterTarget(String targetId) {
    if (targetId != null) {
      enemiesByTargetId.remove(targetId);
    }
  }

  @Override
  public boolean isTargetAvailable(String targetId) {
    Entity enemy = enemiesByTargetId.get(targetId);
    if (enemy == null) {
      return false;
    }
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    return stats != null && !stats.isDead();
  }

  @Override
  public int statusValue(String targetId, EffectType type) {
    if (type == null) {
      throw new IllegalArgumentException("Status effect type cannot be null");
    }
    Entity enemy = enemiesByTargetId.get(targetId);
    if (enemy == null) {
      return 0;
    }
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    if (stats == null || stats.isDead()) {
      return 0;
    }
    StatusEffect status = stats.getStatusEffect(type.name());
    return status == null ? 0 : Math.max(status.getValue(), 0);
  }

  @Override
  public void applyEnemyEffects(CardPlayTarget target, List<ResolvedCardEffect> effects) {
    List<ResolvedCardEffect> ordered = validatedEnemyEffects(target, effects);
    for (Entity enemy : selectedEnemies(target)) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      for (ResolvedCardEffect effect : ordered) {
        switch (effect.type()) {
          case DAMAGE -> stats.takeDamage(effect.value());
          case POISON, VULNERABLE, FEEBLE ->
              stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
          default -> throw unsupportedEnemyEffect(effect.type());
        }
      }
    }
  }

  private List<Entity> selectedEnemies(CardPlayTarget target) {
    if (target.type() == TargetType.SINGLE_ENEMY) {
      if (!isTargetAvailable(target.targetId())) {
        throw new IllegalArgumentException("Enemy target is not available: " + target.targetId());
      }
      return List.of(enemiesByTargetId.get(target.targetId()));
    }
    if (target.type() == TargetType.ALL_ENEMIES) {
      List<Entity> available = new ArrayList<>();
      enemiesByTargetId.forEach(
          (targetId, enemy) -> {
            if (isTargetAvailable(targetId)) {
              available.add(enemy);
            }
          });
      return List.copyOf(available);
    }
    throw new IllegalArgumentException("Enemy effects require an enemy card-play target");
  }

  private static List<ResolvedCardEffect> validatedEnemyEffects(
      CardPlayTarget target, List<ResolvedCardEffect> effects) {
    if (target == null) {
      throw new IllegalArgumentException("Enemy card-play target cannot be null");
    }
    if (target.type() == TargetType.SELF) {
      throw new IllegalArgumentException("Enemy effects cannot use a SELF card-play target");
    }
    if (effects == null) {
      throw new IllegalArgumentException("Enemy effects cannot be null");
    }
    for (ResolvedCardEffect effect : effects) {
      if (effect == null) {
        throw new IllegalArgumentException("Enemy effects cannot contain null");
      }
      if (effect.target() == TargetType.SELF) {
        throw new IllegalArgumentException("Enemy effects cannot target SELF");
      }
      if (effect.target() != target.type()) {
        throw new IllegalArgumentException(
            "Resolved enemy effect target does not match card-play target");
      }
      if (effect.type() != EffectType.DAMAGE
          && effect.type() != EffectType.POISON
          && effect.type() != EffectType.VULNERABLE
          && effect.type() != EffectType.FEEBLE) {
        throw unsupportedEnemyEffect(effect.type());
      }
    }
    return effects.stream().sorted(Comparator.comparingInt(ResolvedCardEffect::sequence)).toList();
  }

  private static IllegalArgumentException unsupportedEnemyEffect(EffectType type) {
    return new IllegalArgumentException("Unsupported enemy card effect: " + type);
  }

  private static CombatStatsComponent requireCombatStats(Entity enemy, String targetId) {
    if (enemy == null) {
      throw new IllegalArgumentException("Enemy entity cannot be null for target: " + targetId);
    }
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException(
          "Enemy entity is missing CombatStatsComponent for target: " + targetId);
    }
    return stats;
  }

  private static void validateTargetId(String targetId) {
    if (targetId == null || targetId.isBlank() || !targetId.equals(targetId.trim())) {
      throw new IllegalArgumentException("Enemy target ID must be non-blank and trimmed");
    }
  }
}
