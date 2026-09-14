package com.csse3200.game.components.cards;

import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;

/** Handles card effects on the player and enemy */
public class CardEffectHandler {

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
          case POISON, VULNERABLE ->
              stats.applyStatusEffect(
                  new StatusEffect(effect.type().name(), effect.value(), effect.duration()));
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
        case BLOCK -> stats.addArmor(effect.value());
        case HEAL -> {
          if (effect.duration() > 0) {
            stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
          } else {
            stats.heal(effect.value());
          }
        }
        case STRENGTH ->
            stats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
        default -> {
          // Enemy-facing effects are handled separately.
        }
      }
    }
  }

  /**
   * Chooses which enemies a card's enemy effects hit. Self-targeting cards hit nothing; everything
   * else hits every living enemy, which covers both the single-enemy encounter and ALL_ENEMIES
   * cards. Precise single-target selection can be layered on when encounters have several enemies.
   */
  public List<Entity> getLivingEnemyTargets(CardPlayRequest request, List<Entity> enemies) {
    if (request.target().type() == TargetType.SELF) {
      return List.of();
    }
    List<Entity> targets = new ArrayList<>();
    for (Entity enemy : enemies) {
      if (!enemy.getComponent(CombatStatsComponent.class).isDead()) {
        targets.add(enemy);
      }
    }
    return targets;
  }
}
