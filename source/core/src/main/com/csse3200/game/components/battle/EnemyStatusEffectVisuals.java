package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

/**
 * Registers visual styles for enemy-facing status effects.
 *
 * <p>The textures are four-frame sheets, read by {@link EnemyStatusEffectVisualComponent}. Register
 * these only alongside that renderer's coordinator integration, never as single-image visuals.
 */
public final class EnemyStatusEffectVisuals {

  private static final String DIRECTORY = "images/effects/enemy-status/";

  private EnemyStatusEffectVisuals() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Registers all enemy status-effect visuals into the shared registry.
   *
   * @param registry the shared effect visual registry
   */
  public static void registerAll(EffectVisualRegistry registry) {
    registry.register(
        EffectType.POISON,
        new EffectVisualStyle(
            DIRECTORY + "poison.png", new Color(1f, 1f, 1f, 0.85f), 0.5f, 0.45f, 0.65f, 0.12f));

    registry.register(
        EffectType.VULNERABLE,
        new EffectVisualStyle(
            DIRECTORY + "vulnerable.png", new Color(1f, 1f, 1f, 0.8f), 0.5f, 0.55f, 0.7f, 0f));

    registry.register(
        EffectType.FEEBLE,
        new EffectVisualStyle(
            DIRECTORY + "feeble.png", new Color(1f, 1f, 1f, 0.85f), 0.5f, 0.62f, 0.48f, -0.12f));
  }

  /**
   * Runtime sheets to load once with the battle assets, and unload after its visuals are disposed.
   */
  public static String[] texturePaths() {
    return new String[] {
      DIRECTORY + "poison.png", DIRECTORY + "vulnerable.png", DIRECTORY + "feeble.png"
    };
  }

  /** Whether this effect needs the enemy status sheet renderer. */
  public static boolean supports(EffectType type) {
    return type == EffectType.POISON || type == EffectType.VULNERABLE || type == EffectType.FEEBLE;
  }
}
