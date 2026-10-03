package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CombatStatsStatusQueryTest {
    @Test
    void shouldReturnEmptySnapshotWhenNoStatusesAreActive() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);

        Map<String, Integer> durations = stats.getStatusEffectDurations();

        assertTrue(durations.isEmpty());
    }

    @Test
    void shouldReturnEveryActiveStatusDuration() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);
        stats.applyStatusEffect("SILENCE", 1, 2);
        stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 4);

        Map<String, Integer> durations = stats.getStatusEffectDurations();

        assertEquals(2, durations.size());
        assertEquals(2, durations.get("SILENCE"));
        assertEquals(4, durations.get("DAMAGE_ON_CARD_PLAY"));
    }

    @Test
    void shouldIncludeStatusWithoutFiniteDuration() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);
        stats.applyStatusEffect("STRENGTH", 2, 0);

        Map<String, Integer> durations = stats.getStatusEffectDurations();

        assertEquals(0, durations.get("STRENGTH"));
    }

    @Test
    void shouldReturnUnmodifiableSnapshot() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);
        stats.applyStatusEffect("SILENCE", 1, 2);

        Map<String, Integer> durations = stats.getStatusEffectDurations();

        assertThrows(
                UnsupportedOperationException.class,
                () -> durations.put("DAMAGE_ON_CARD_PLAY", 3));

        assertThrows(
                UnsupportedOperationException.class,
                () -> durations.remove("SILENCE"));

        assertTrue(stats.hasStatusEffect("SILENCE"));
        assertFalse(stats.hasStatusEffect("DAMAGE_ON_CARD_PLAY"));
    }

    @Test
    void shouldDetachSnapshotFromLaterStatusChanges() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);
        stats.applyStatusEffect("SILENCE", 1, 2);

        Map<String, Integer> originalSnapshot = stats.getStatusEffectDurations();

        stats.removeStatusEffect("SILENCE");
        stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 4);

        assertEquals(Map.of("SILENCE", 2), originalSnapshot);

        Map<String, Integer> updatedSnapshot = stats.getStatusEffectDurations();

        assertEquals(Map.of("DAMAGE_ON_CARD_PLAY", 4), updatedSnapshot);
    }

    @Test
    void shouldExcludeExpiredStatusFromNewSnapshot() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);
        stats.applyStatusEffect("SILENCE", 1, 1);

        assertEquals(
                Map.of("SILENCE", 1),
                stats.getStatusEffectDurations());

        stats.updateStatusEffects();

        assertFalse(stats.hasStatusEffect("SILENCE"));
        assertTrue(stats.getStatusEffectDurations().isEmpty());
    }

    @Test
    void shouldReflectUpdatedDurationInNewSnapshot() {
        CombatStatsComponent stats = new CombatStatsComponent(20, 5);
        stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);

        Map<String, Integer> firstSnapshot = stats.getStatusEffectDurations();

        stats.updateStatusEffects();

        Map<String, Integer> secondSnapshot = stats.getStatusEffectDurations();

        assertEquals(2, firstSnapshot.get("DAMAGE_ON_CARD_PLAY"));
        assertEquals(1, secondSnapshot.get("DAMAGE_ON_CARD_PLAY"));
    }
}
