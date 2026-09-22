package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;

/**
 * Describes how one effect's visual looks and behaves: which texture to draw (or null for a
 * generated glow), its tint, how long it lasts, and how it grows/drifts over its lifetime.
 *
 * <p>Purely data — no rendering or game logic here. Each teammate creates their own instances of
 * this and registers them against an {@link EffectType} in {@link EffectVisualRegistry}.
 *
 * @param iconPath texture path to draw, or null to use a generated glow shape
 * @param color tint applied to the texture or glow
 * @param duration lifetime in seconds; must be positive
 * @param startScale size at spawn, relative to the target's own size
 * @param endScale size at the end of the lifetime, relative to the target's own size
 * @param rise upward drift over the lifetime, relative to the target's own size (0 = no drift)
 */
public record EffectVisualStyle(
        String iconPath, Color color, float duration, float startScale, float endScale, float rise) {

    public EffectVisualStyle {
        if (color == null) {
            throw new IllegalArgumentException("Visual color cannot be null");
        }
        if (duration <= 0f) {
            throw new IllegalArgumentException("Visual duration must be positive");
        }
        if (startScale <= 0f || endScale <= 0f) {
            throw new IllegalArgumentException("Visual scales must be positive");
        }
    }
}