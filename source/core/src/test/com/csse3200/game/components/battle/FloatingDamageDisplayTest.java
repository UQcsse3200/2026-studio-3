package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.combat.DamageTakenEvent;
import com.csse3200.game.components.combat.DamageType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FloatingDamageDisplayTest {
    private Entity entity;
    private FloatingDamageDisplay display;

    @BeforeEach
    void setUp() {
        ServiceLocator.registerRenderService(new RenderService());

        entity = new Entity();
        display = new FloatingDamageDisplay();

        entity.addComponent(display);
        display.create();
    }

    @AfterEach
    void tearDown() {
        display.dispose();
    }

    @Test
    void shouldQueueNumberWhenDamageEventReceived() {
        entity.getEvents().trigger("damageTaken", new DamageTakenEvent(5, DamageType.NORMAL));

        assertEquals(1, display.activeNumberCount());
    }

    @Test
    void shouldIgnoreZeroDamageEvent() {
        entity.getEvents().trigger("damageTaken", new DamageTakenEvent(0, DamageType.NORMAL));

        assertEquals(0, display.activeNumberCount());
    }

    @Test
    void shouldRemoveNumberWhenAnimationExpires() {
        entity.getEvents().trigger("damageTaken", new DamageTakenEvent(5, DamageType.NORMAL));

        display.advance(FloatingDamageDisplay.DISPLAY_DURATION - 0.01f);
        assertEquals(1, display.activeNumberCount());

        display.advance(0.01f);
        assertEquals(0, display.activeNumberCount());
    }

    @Test
    void shouldMapDamageTypesToDistinctColours() {
        assertEquals(Color.WHITE, FloatingDamageDisplay.colorFor(DamageType.NORMAL));
        assertEquals(
                new Color(1f, 0.72f, 0.2f, 1f), FloatingDamageDisplay.colorFor(DamageType.PIERCE));
        assertEquals(
                new Color(0.35f, 0.9f, 0.35f, 1f), FloatingDamageDisplay.colorFor(DamageType.POISON));
    }

    @Test
    void shouldMoveNumbersUpwardOverTheirLifetime() {
        assertEquals(0f, FloatingDamageDisplay.verticalOffset(0f));
        assertEquals(
                0.35f,
                FloatingDamageDisplay.verticalOffset(FloatingDamageDisplay.DISPLAY_DURATION / 2f));
        assertEquals(0.7f, FloatingDamageDisplay.verticalOffset(FloatingDamageDisplay.DISPLAY_DURATION));
    }

    @Test
    void shouldTreatLifetimeBoundaryAsExpired() {
        assertEquals(false, FloatingDamageDisplay.isExpired(0.79f));
        assertEquals(true, FloatingDamageDisplay.isExpired(FloatingDamageDisplay.DISPLAY_DURATION));
    }
}