package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.combat.DamageTakenEvent;
import com.csse3200.game.components.combat.DamageType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CombatDamageEventTest {

    @Test
    void shouldReportActualNormalHealthLossAfterBlockAndArmour() {
        List<DamageTakenEvent> events = new ArrayList<>();
        CombatStatsComponent stats = statsWithDamageListener(events);
        stats.setBlock(2);
        stats.setArmour(3);

        stats.takeDamage(10);

        assertEquals(15, stats.getHealth());
        assertEquals(List.of(new DamageTakenEvent(5, DamageType.NORMAL)), events);
    }

    @Test
    void shouldReportPiercingDamage() {
        List<DamageTakenEvent> events = new ArrayList<>();
        CombatStatsComponent stats = statsWithDamageListener(events);
        stats.setBlock(4);
        stats.setArmour(5);

        stats.takePiercingDamage(6);

        assertEquals(14, stats.getHealth());
        assertEquals(4, stats.getBlock());
        assertEquals(5, stats.getArmour());
        assertEquals(List.of(new DamageTakenEvent(6, DamageType.PIERCE)), events);
    }

    @Test
    void shouldReportPoisonDamage() {
        List<DamageTakenEvent> events = new ArrayList<>();
        CombatStatsComponent stats = statsWithDamageListener(events);

        stats.takePoisonDamage(4);

        assertEquals(16, stats.getHealth());
        assertEquals(List.of(new DamageTakenEvent(4, DamageType.POISON)), events);
    }

    @Test
    void shouldNotReportDamageWhenDefencesAbsorbAllHealthLoss() {
        List<DamageTakenEvent> events = new ArrayList<>();
        CombatStatsComponent stats = statsWithDamageListener(events);
        stats.setBlock(3);
        stats.setArmour(4);

        stats.takeDamage(7);

        assertEquals(20, stats.getHealth());
        assertEquals(List.of(), events);
    }

    @Test
    void shouldClampReportedDamageToRemainingHealth() {
        List<DamageTakenEvent> events = new ArrayList<>();
        CombatStatsComponent stats = statsWithDamageListener(events);

        stats.takeDamage(100);

        assertEquals(0, stats.getHealth());
        assertEquals(List.of(new DamageTakenEvent(20, DamageType.NORMAL)), events);
    }

    private static CombatStatsComponent statsWithDamageListener(List<DamageTakenEvent> events) {
        Entity entity = new Entity();
        CombatStatsComponent stats = new CombatStatsComponent(20, 0);

        entity.addComponent(stats);
        entity.getEvents().addListener("damageTaken", events::add);

        return stats;
    }
}