package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CursePenaltiesTest {
  @Test
  void reportsOnlyActualCursedGainReductions() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);
    AtomicReference<CursePenalties.GainReduction> event = new AtomicReference<>();
    player.getEvents().addListener(CursePenalties.GAIN_REDUCED_EVENT, event::set);

    assertEquals(2, CursePenalties.reduce(stats, CursePenalties.STRENGTH_PENALTY, 2));
    assertNull(event.get());

    CursePenalties.addPenalty(stats, CursePenalties.STRENGTH_PENALTY);
    assertEquals(1, CursePenalties.reduce(stats, CursePenalties.STRENGTH_PENALTY, 2));
    assertEquals(
        new CursePenalties.GainReduction(CursePenalties.STRENGTH_PENALTY, 2, 1), event.get());
  }
}
