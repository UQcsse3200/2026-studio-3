package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

public final class OffensiveEffectVisuals {

  private OffensiveEffectVisuals() {
    throw new IllegalStateException("Utility class");
  }

  /** Registers every offensive effect's visual style into the given registry. */
  public static void registerAll(EffectVisualRegistry registry) {
    registry.register(
        EffectType.DAMAGE,
        new EffectVisualStyle(null, new Color(1f, 0.35f, 0.15f, 1f), 0.35f, 1f, 0.4f, 0f));
    registry.register(
        EffectType.PIERCE,
        new EffectVisualStyle(null, new Color(1f, 1f, 1f, 1f), 0.18f, 1.4f, 1f, 0f));
    registry.register(
        EffectType.SUNDER,
        new EffectVisualStyle(null, new Color(0.55f, 0.55f, 0.6f, 1f), 0.7f, 2f, 1.3f, 0f));
  }

  public static boolean usesBurst(EffectType type) {
    return type == EffectType.DAMAGE || type == EffectType.SUNDER;
  }

  public static boolean usesProjectile(EffectType type) {
    return type == EffectType.PIERCE;
  }

  /** Builds the burst entity for Damage (even scatter) or Sunder (falling debris). */
  public static EffectBurstComponent createBurstComponent(
      EffectType type, EffectVisualStyle style, float baseSize, float startDelay) {
    boolean hasGravity = type == EffectType.SUNDER;
    EffectBurstComponent.ParticleShape shape =
        type == EffectType.SUNDER
            ? EffectBurstComponent.ParticleShape.SHARD
            : EffectBurstComponent.ParticleShape.DOT;
    return new EffectBurstComponent(style, baseSize, startDelay, hasGravity, shape);
  }

  /** Builds the Pierce slash entity. Tune direction/reach here — not in the coordinator. */
  public static EffectProjectileComponent createPierceComponent(
      EffectVisualStyle style, float baseSize, float startDelay) {
    return new EffectProjectileComponent(style, baseSize, startDelay, 225f, 2.2f);
  }
}
