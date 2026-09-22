package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps an {@link EffectType} to the {@link EffectVisualStyle} used to represent it on screen.
 *
 * <p>Each teammate registers their own effect types from their own file, so no one has to edit a
 * shared switch statement. An unregistered type falls back to a plain default glow rather than
 * failing, so a newly added effect type never breaks the battle.
 */
public class EffectVisualRegistry {
  private static final EffectVisualStyle DEFAULT_STYLE =
      new EffectVisualStyle(null, Color.WHITE, 0.4f, 0.4f, 1.2f, 0f);

  private final Map<EffectType, EffectVisualStyle> styles = new HashMap<>();

  /**
   * Registers (or replaces) the visual style for an effect type.
   *
   * @param type the effect type this style represents
   * @param style the visual style to use for it
   */
  public void register(EffectType type, EffectVisualStyle style) {
    if (type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    if (style == null) {
      throw new IllegalArgumentException("Visual style cannot be null");
    }
    styles.put(type, style);
  }

  /**
   * Looks up the visual style for an effect type.
   *
   * @param type the effect type, may be null
   * @return the registered style, or a plain default glow if none is registered (or type is null)
   */
  public EffectVisualStyle lookup(EffectType type) {
    if (type == null) {
      return DEFAULT_STYLE;
    }
    return styles.getOrDefault(type, DEFAULT_STYLE);
  }
}
