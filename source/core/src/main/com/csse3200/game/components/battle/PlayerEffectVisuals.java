package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

public final class PlayerEffectVisuals {
  private static final String BLOCK_ICON = "images/effects/shield.png";
  private static final String FORTIFY_ICON = "images/effects/fortify.png";
  private static final String HEAL_ICON = "images/effects/heal.png";
  private static final String STRENGTH_ICON =
      "images/enemies/intents/buff.png"; // placeholder pic since the intent icon are close enough

  // to strength

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

    registry.register(
        EffectType.HEAL,
        new EffectVisualStyle(HEAL_ICON, new Color(Color.WHITE), 0.7f, 0.4f, 1.0f, 0.8f));

    registry.register(
        EffectType.STRENGTH,
        new EffectVisualStyle(STRENGTH_ICON, new Color(Color.WHITE), 0.5f, 0.5f, 1.5f, 0.0f));

    registry.register(
        EffectType.ENERGY_GAIN,
        new EffectVisualStyle(null, new Color(Color.YELLOW), 0.6f, 1.6f, 0.4f, 0f));

    registry.register(
        EffectType.CLEANSE,
        new EffectVisualStyle(null, new Color(Color.PURPLE), 0.75f, 2f, 1.3f, 0f));
  }

  /**
   * Check if the effect type need to use Burst
   *
   * @return true if the effect type are either energy gain or cleanse. Else false
   */
  public static boolean usesBurst(EffectType type) {
    return type == EffectType.ENERGY_GAIN || type == EffectType.CLEANSE;
  }

  /**
   * Check which type effect it is and change the component accordingly.
   *
   * @return EffectBurstComponent
   */
  public static EffectBurstComponent createBurstComponent(
      EffectType type, EffectVisualStyle style, float baseSize, float startDelay) {
    boolean hasGravity = false;
    EffectBurstComponent.ParticleShape shape = EffectBurstComponent.ParticleShape.DOT;
    if (type == EffectType.CLEANSE) {
      hasGravity = true;
      shape = EffectBurstComponent.ParticleShape.SHARD;
    }
    return new EffectBurstComponent(style, baseSize, startDelay, hasGravity, shape);
  }
}
