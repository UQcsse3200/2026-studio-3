package com.csse3200.game.components.battle;

/**
 * Registers the visuals for offensive (damage-related) card effects.
 *
 * <p>Each teammate has one file like this for their own effect group, so registering a visual never
 * requires editing a shared file. Call {@link #registerAll(EffectVisualRegistry)} once, from
 * wherever the registry is created.
 */
public final class OffensiveEffectVisuals {

  private OffensiveEffectVisuals() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Registers every offensive effect's visual style into the given registry.
   *
   * <p>TODO: register Damage, Pierce and Sunder once the visuals are finalised.
   *
   * @param registry the shared registry to register into
   */
  public static void registerAll(EffectVisualRegistry registry) {
    // Intentionally empty for now — offensive visuals are still being designed.
  }
}
