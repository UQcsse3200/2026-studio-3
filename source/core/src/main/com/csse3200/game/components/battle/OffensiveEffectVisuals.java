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
    // TODO: Pierce, Sunder — different rendering style, added separately.
  }

  /** Whether this effect type uses the burst renderer instead of the icon renderer. */
  public static boolean usesBurst(EffectType type) {
    return type == EffectType.DAMAGE;
  }
}
