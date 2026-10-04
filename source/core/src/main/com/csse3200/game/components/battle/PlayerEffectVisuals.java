package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

public final class PlayerEffectVisuals {
  private static final String BLOCK_ICON = "images/effects/shield.png";
  private static final String FORTIFY_ICON = "images/effects/fortify.png";

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
        EffectType.FORTIFY,
        new EffectVisualStyle(FORTIFY_ICON, new Color(Color.WHITE), 0.5f, 0.45f, 1.25f, 0.1f));
  }
}
