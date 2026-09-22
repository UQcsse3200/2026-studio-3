package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

/**
 * Registers the visuals for offensive (damage-related) card effects.
 *
 * <p>Each teammate has one file like this for their own effect group, so registering a visual
 * never requires editing a shared file. Call {@link #registerAll(EffectVisualRegistry)} once, from
 * wherever the registry is created.
 */
public final class OffensiveEffectVisuals {

    private OffensiveEffectVisuals() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Registers every offensive effect's visual style into the given registry.
     *
     * @param registry the shared registry to register into
     */
    public static void registerAll(EffectVisualRegistry registry) {
        registry.register(
                EffectType.DAMAGE,
                new EffectVisualStyle(
                        "images/enemies/intents/attack.png", Color.WHITE, 0.4f, 0.5f, 1.3f, 0f));
    }
}