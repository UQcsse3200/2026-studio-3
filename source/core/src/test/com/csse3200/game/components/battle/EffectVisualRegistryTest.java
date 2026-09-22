package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;
import org.junit.jupiter.api.Test;

class EffectVisualRegistryTest {

    private final EffectVisualStyle damageStyle =
            new EffectVisualStyle("images/enemies/intents/attack.png", Color.WHITE, 0.4f, 0.5f, 1.2f, 0f);

    @Test
    void lookupReturnsWhatWasRegistered() {
        EffectVisualRegistry registry = new EffectVisualRegistry();

        registry.register(EffectType.DAMAGE, damageStyle);

        assertSame(damageStyle, registry.lookup(EffectType.DAMAGE));
    }

    @Test
    void lookupOnUnregisteredTypeReturnsADefaultInsteadOfNull() {
        EffectVisualRegistry registry = new EffectVisualRegistry();

        EffectVisualStyle result = registry.lookup(EffectType.HEAL);

        assertNotNull(result, "unregistered type should fall back to a default, not null");
    }

    @Test
    void lookupOnNullTypeReturnsTheDefault() {
        EffectVisualRegistry registry = new EffectVisualRegistry();

        assertNotNull(registry.lookup(null));
    }

    @Test
    void laterRegisterForSameTypeReplacesTheEarlierOne() {
        EffectVisualRegistry registry = new EffectVisualRegistry();
        EffectVisualStyle firstAttempt =
                new EffectVisualStyle(null, Color.RED, 0.3f, 0.4f, 1f, 0f);
        EffectVisualStyle finalVersion = damageStyle;

        registry.register(EffectType.DAMAGE, firstAttempt);
        registry.register(EffectType.DAMAGE, finalVersion);

        assertSame(finalVersion, registry.lookup(EffectType.DAMAGE));
    }

    @Test
    void rejectsNullType() {
        EffectVisualRegistry registry = new EffectVisualRegistry();

        assertThrows(
                IllegalArgumentException.class, () -> registry.register(null, damageStyle));
    }

    @Test
    void rejectsNullStyle() {
        EffectVisualRegistry registry = new EffectVisualRegistry();

        assertThrows(
                IllegalArgumentException.class, () -> registry.register(EffectType.DAMAGE, null));
    }
}