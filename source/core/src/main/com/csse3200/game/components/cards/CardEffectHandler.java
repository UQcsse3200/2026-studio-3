package com.csse3200.game.components.cards;

import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Handles card effects on the player and enemy */
public class CardEffectHandler {
  private final Map<String, Entity> enemyTargets;

  public CardEffectHandler() {
    this(Map.of());
  }

  /** Uses battle-owned target IDs without changing the shared enemy factory or drop widgets. */
  public CardEffectHandler(Map<String, Entity> enemyTargets) {
    this.enemyTargets = Map.copyOf(enemyTargets);
  }

  /**
   * Applies the given card effects on the enemy targets
   *
   * @param targets the enemies to apply the card effects to
   * @param effects the card effects to apply to the enemies
   */
  public void applyEnemyEffects(List<Entity> targets, List<ResolvedCardEffect> effects) {
    for (Entity enemy : targets) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats == null) {
        continue;
      }
      for (ResolvedCardEffect effect : effects) {
        switch (effect.type()) {
          case DAMAGE -> stats.takeDamage(effect.value());
          case PIERCE -> stats.takePiercingDamage(effect.value());
          case POISON, VULNERABLE, FEEBLE ->
              stats.applyStatusEffect(
                  new StatusEffect(effect.type().name(), effect.value(), effect.duration()));
          case SUNDER -> stats.reduceArmour(effect.value());
          default -> {
            // BLOCK / HEAL / STRENGTH are not enemy-facing.
          }
        }
      }
    }
  }

  /**
   * Applies the given effects from the enemies on the player
   *
   * @param effects the effects to apply to the player
   * @param player the player instance in the game
   */
  public void applyPlayerEffects(List<ResolvedCardEffect> effects, Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return;
    }
    for (ResolvedCardEffect effect : effects) {
      switch (effect.type()) {
        case BLOCK -> stats.addBlock(effect.value());
        case HEAL -> {
          if (effect.duration() > 0) {
            stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
          } else {
            stats.heal(effect.value());
          }
        }
        case STRENGTH ->
            stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
        case ENERGY_GAIN -> {
          EnergyComponent energy = player.getComponent(EnergyComponent.class);
          if (energy != null) {
            energy.restoreEnergy(effect.value());
          }
        }
        case CLEANSE -> stats.clearNegativeStatusEffects();
        case FORTIFY -> stats.addArmour(effect.value());
        default -> {
          // Enemy-facing effects are handled separately.
        }
      }
    }
  }

  /**
   * Selects living targets using the battle's registered drop-target IDs. Encounters without a
   * registry use entity IDs. Missing or defeated targets never redirect to another enemy.
   */
  public List<Entity> getLivingEnemyTargets(CardPlayRequest request, List<Entity> enemies) {
    if (request.target().type() == TargetType.SELF) {
      return List.of();
    }
    List<Entity> targets = new ArrayList<>();
    for (Entity enemy : enemies) {
      if (!enemy.getComponent(CombatStatsComponent.class).isDead()
          && (request.target().type() == TargetType.ALL_ENEMIES
              || (enemyTargets.isEmpty()
                  ? Integer.toString(enemy.getId()).equals(request.target().targetId())
                  : enemy == enemyTargets.get(request.target().targetId())))) {
        targets.add(enemy);
      }
    }
    return targets;
  }
}
