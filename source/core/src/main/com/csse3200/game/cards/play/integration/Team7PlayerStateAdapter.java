package com.csse3200.game.cards.play.integration;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.PlayerStateView;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.Comparator;
import java.util.List;

/**
 * Adapts Team 7's player components to Team 5's read-only resolution view and result consumer.
 *
 * <p>Team 5 uses the read methods before resolving a card. The battle flow may call {@link
 * #applyPlayerEffects(List)} only after receiving a successful card-play result.
 */
public final class Team7PlayerStateAdapter implements PlayerStateView, PlayerEffectConsumer {
  private final EnergyComponent energy;
  private final CombatStatsComponent combatStats;

  /** Creates an adapter from a player entity containing Team 7's required components. */
  public Team7PlayerStateAdapter(Entity player) {
    this(
        requireComponent(player, EnergyComponent.class),
        requireComponent(player, CombatStatsComponent.class));
  }

  /** Creates an adapter from explicit Team 7 components, useful for assembly and testing. */
  public Team7PlayerStateAdapter(EnergyComponent energy, CombatStatsComponent combatStats) {
    if (energy == null) {
      throw new IllegalArgumentException("Player energy component cannot be null");
    }
    if (combatStats == null) {
      throw new IllegalArgumentException("Player combat stats component cannot be null");
    }
    this.energy = energy;
    this.combatStats = combatStats;
  }

  @Override
  public int currentEnergy() {
    return energy.getCurrentEnergy();
  }

  @Override
  public int statusValue(EffectType type) {
    if (type == null) {
      throw new IllegalArgumentException("Status effect type cannot be null");
    }
    StatusEffect status = combatStats.getStatusEffect(type.name());
    return status == null ? 0 : Math.max(status.getValue(), 0);
  }

  @Override
  public void applyPlayerEffects(List<ResolvedCardEffect> effects) {
    List<ResolvedCardEffect> ordered = validatedPlayerEffects(effects);
    for (ResolvedCardEffect effect : ordered) {
      switch (effect.type()) {
        case BLOCK -> combatStats.addBlock(effect.value());
        case HEAL -> combatStats.heal(effect.value());
        case STRENGTH ->
            combatStats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration());
        default -> throw unsupportedPlayerEffect(effect.type());
      }
    }
  }

  private static List<ResolvedCardEffect> validatedPlayerEffects(List<ResolvedCardEffect> effects) {
    if (effects == null) {
      throw new IllegalArgumentException("Player effects cannot be null");
    }
    for (ResolvedCardEffect effect : effects) {
      if (effect == null) {
        throw new IllegalArgumentException("Player effects cannot contain null");
      }
      if (effect.target() != TargetType.SELF) {
        throw new IllegalArgumentException("Player effects must target SELF");
      }
      if (effect.type() != EffectType.BLOCK
          && effect.type() != EffectType.HEAL
          && effect.type() != EffectType.STRENGTH) {
        throw unsupportedPlayerEffect(effect.type());
      }
    }
    return effects.stream().sorted(Comparator.comparingInt(ResolvedCardEffect::sequence)).toList();
  }

  private static IllegalArgumentException unsupportedPlayerEffect(EffectType type) {
    return new IllegalArgumentException("Unsupported player card effect: " + type);
  }

  private static <T extends com.csse3200.game.components.Component> T requireComponent(
      Entity entity, Class<T> type) {
    if (entity == null) {
      throw new IllegalArgumentException("Player entity cannot be null");
    }
    T component = entity.getComponent(type);
    if (component == null) {
      throw new IllegalArgumentException(
          "Player entity is missing required component: " + type.getSimpleName());
    }
    return component;
  }
}
