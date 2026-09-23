package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;

/**
 * Registers visual styles for enemy-facing status effects.
 *
 * <p>These effects share the generic debuff icon but use different colours and motion so they are
 * visually distinguishable without adding new shared rendering logic.
 */
public final class EnemyStatusEffectVisuals {

    private static final String DEBUFF_ICON = "images/enemies/intents/debuff.png";

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
                        DEBUFF_ICON,
                        new Color(0.35f, 0.9f, 0.3f, 1f),
                        0.55f,
                        0.45f,
                        1.15f,
                        0.25f));

        registry.register(
                EffectType.VULNERABLE,
                new EffectVisualStyle(
                        DEBUFF_ICON,
                        new Color(0.85f, 0.3f, 0.85f, 1f),
                        0.5f,
                        0.45f,
                        1.25f,
                        0.1f));

        registry.register(
                EffectType.FEEBLE,
                new EffectVisualStyle(
                        DEBUFF_ICON,
                        new Color(0.65f, 0.75f, 0.9f, 1f),
                        0.5f,
                        0.5f,
                        1.1f,
                        0.15f));
    }
}