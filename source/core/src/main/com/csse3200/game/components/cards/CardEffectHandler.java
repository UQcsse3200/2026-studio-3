package com.csse3200.game.components.cards;

import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
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
          case DAMAGE -> {
            AudioService.playSound(SoundId.SWORD_SWING, 0.6f);
            stats.takeDamage(effect.value());
          }
          case PIERCE -> {
            AudioService.playSound(SoundId.ARMOUR_BREAK, 0.5f);
            stats.takePiercingDamage(effect.value());
          }
          case SUNDER -> {
            AudioService.playSound(SoundId.ARMOUR_BREAK, 0.5f);
            stats.setArmour(stats.getArmour() - effect.value());
          }
          case VULNERABLE, FEEBLE -> {
            AudioService.playSound(SoundId.ARMOUR_BREAK, 0.5f);
            stats.applyStatusEffect(
                new StatusEffect(effect.type().name(), effect.value(), effect.duration()));
          }
          case POISON -> {
            AudioService.playSound(SoundId.BOTTLE_CORK, 0.5f);
            stats.applyStatusEffect(
                new StatusEffect(effect.type().name(), effect.value(), effect.duration()));
          }
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
        case BLOCK -> {
          AudioService.playSound(SoundId.SHIELD_GUARD, 0.3f);
          stats.addBlock(effect.value());
        }
        case FORTIFY -> {
          AudioService.playSound(SoundId.SHIELD_GUARD, 0.3f);
          stats.addArmour(effect.value());
        }
        case HEAL -> {
          AudioService.playSound(SoundId.BANDAGE, 0.4f);
          if (effect.duration() > 0) {
            stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
          } else {
            stats.heal(effect.value());
          }
        }
        case STRENGTH -> {
          AudioService.playSound(SoundId.MAGIC_CHIME, 0.5f);
          stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
        }
        case ENERGY_GAIN -> {
          AudioService.playSound(SoundId.MAGIC_CHIME, 0.5f);
          EnergyComponent energy = player.getComponent(EnergyComponent.class);
          if (energy != null) {
            energy.restoreEnergy(effect.value());
          }
        }
        case CLEANSE -> {
          AudioService.playSound(SoundId.MAGIC_CHIME, 0.5f);
          stats.clearNegativeStatusEffects();
        }
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
