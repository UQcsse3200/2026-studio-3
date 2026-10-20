package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

public final class PlayerEffectVisuals {
  private static final String BLOCK_ICON = "images/effects/shield.png";

  private PlayerEffectVisuals() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Registers all player status-effect visuals into the shared registry.
   *
   * @param registry the shared effect visual registry
   */
  public static void registerAll(EffectVisualRegistry registry) {
    registry.register(
        EffectType.BLOCK,
        new EffectVisualStyle(BLOCK_ICON, new Color(Color.WHITE), 0.55f, 0.45f, 1.15f, 0.25f));

    registry.register(
        EffectType.FORTIFY, new EffectVisualStyle(null, new Color(Color.BLUE), 0.5f, 2f, 1f, 0f));

    registry.register(
        EffectType.HEAL, new EffectVisualStyle(null, new Color(Color.GREEN), 0.7f, 1.4f, 0.5f, 0f));

    registry.register(
        EffectType.STRENGTH,
        new EffectVisualStyle(null, new Color(Color.RED), 0.5f, 1.8f, 0.6f, 0.0f));

    registry.register(
        EffectType.ENERGY_GAIN,
        new EffectVisualStyle(null, new Color(Color.YELLOW), 0.6f, 1.6f, 0.4f, 0f));

    registry.register(
        EffectType.CLEANSE,
        new EffectVisualStyle(null, new Color(Color.PURPLE), 0.75f, 2f, 1.3f, 0f));
  }

  /**
   * @param type check the type of the effect and return accordingly
   * @return true if the effect type are either energy gain or cleanse. Else false
   */
  public static boolean usesBurst(EffectType type) {
    return type == EffectType.ENERGY_GAIN
        || type == EffectType.CLEANSE
        || type == EffectType.HEAL
        || type == EffectType.FORTIFY
        || type == EffectType.STRENGTH;
  }

  /**
   * Build burst depending on the type of effect. Change only the shape and the motion.
   *
   * @param type type of effect that will use the component
   * @param style the style of the effect
   * @param baseSize The size of the effect
   * @param startDelay delay value before the animation start
   * @return EffectBurstComponent
   */
  public static EffectBurstComponent createBurstComponent(
      EffectType type, EffectVisualStyle style, float baseSize, float startDelay) {
    EffectBurstComponent.ParticleMotion motion = EffectBurstComponent.ParticleMotion.SCATTER;
    EffectBurstComponent.ParticleShape shape = EffectBurstComponent.ParticleShape.DOT;
    switch (type) {
      case CLEANSE -> {
        motion = EffectBurstComponent.ParticleMotion.FALL;
        shape = EffectBurstComponent.ParticleShape.SHARD;
      }

      case HEAL -> {
        motion = EffectBurstComponent.ParticleMotion.RISE;
      }
      case FORTIFY -> {
        shape = EffectBurstComponent.ParticleShape.SHARD;
      }
      case STRENGTH -> {
        motion = EffectBurstComponent.ParticleMotion.RISE;
        shape = EffectBurstComponent.ParticleShape.SHARD;
      }
      default -> {}
    }
    return new EffectBurstComponent(style, baseSize, startDelay, motion, shape);
  }
}
