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
    // TODO: Sunder — added separately.
  }

  public static boolean usesBurst(EffectType type) {
    return type == EffectType.DAMAGE;
  }

  public static boolean usesProjectile(EffectType type) {
    return type == EffectType.PIERCE;
  }

  /** Builds the Pierce slash entity. Tune direction/reach here — not in the coordinator. */
  public static EffectProjectileComponent createPierceComponent(
      EffectVisualStyle style, float baseSize, float startDelay) {
    return new EffectProjectileComponent(style, baseSize, startDelay, 225f, 2.2f);
  }
}
