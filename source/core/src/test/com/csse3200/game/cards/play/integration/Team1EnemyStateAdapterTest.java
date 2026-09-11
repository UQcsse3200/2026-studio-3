package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Team1EnemyStateAdapterTest {
  @Test
  void shouldReadAvailabilityAndStatusesFromTeamOneEnemyState() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.applyStatusEffect(EffectType.VULNERABLE.name(), 2, 3);
    Team1EnemyStateAdapter adapter =
        new Team1EnemyStateAdapter(Map.of("enemy-1", enemyWith(stats)));

    assertTrue(adapter.isTargetAvailable("enemy-1"));
    assertFalse(adapter.isTargetAvailable("missing"));
    assertEquals(2, adapter.statusValue("enemy-1", EffectType.VULNERABLE));
    assertEquals(0, adapter.statusValue("enemy-1", EffectType.FEEBLE));
    assertEquals(0, adapter.statusValue("missing", EffectType.VULNERABLE));
  }

  @Test
  void shouldApplyResolvedDamageBeforeTeamOneBlockArmorAndHealthHandling() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.addBlock(3);
    stats.addArmor(2);
    Team1EnemyStateAdapter adapter =
        new Team1EnemyStateAdapter(Map.of("enemy-1", enemyWith(stats)));

    adapter.applyEnemyEffects(
        CardPlayTarget.singleEnemy("enemy-1"),
        List.of(enemyEffect(EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 10, 0, 0)));

    assertEquals(5, stats.getHealth());
    assertEquals(0, stats.getBlock());
    assertEquals(0, stats.getArmor());
  }

  @Test
  void shouldApplyStatusesToEveryAvailableEnemy() {
    CombatStatsComponent first = new CombatStatsComponent(10, 1);
    CombatStatsComponent second = new CombatStatsComponent(10, 1);
    Team1EnemyStateAdapter adapter =
        new Team1EnemyStateAdapter(
            Map.of("enemy-1", enemyWith(first), "enemy-2", enemyWith(second)));

    adapter.applyEnemyEffects(
        CardPlayTarget.allEnemies(),
        List.of(enemyEffect(EffectType.VULNERABLE, TargetType.ALL_ENEMIES, 1, 2, 0)));

    assertEquals(1, first.getStatusEffect(EffectType.VULNERABLE.name()).getValue());
    assertEquals(1, second.getStatusEffect(EffectType.VULNERABLE.name()).getValue());
  }

  @Test
  void shouldRejectUnavailableSingleEnemyWithoutMutatingAnotherEnemy() {
    CombatStatsComponent available = new CombatStatsComponent(10, 1);
    Team1EnemyStateAdapter adapter =
        new Team1EnemyStateAdapter(Map.of("enemy-1", enemyWith(available)));

    List<ResolvedCardEffect> effects =
        List.of(enemyEffect(EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 4, 0, 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> adapter.applyEnemyEffects(CardPlayTarget.singleEnemy("missing"), effects));
    assertEquals(10, available.getHealth());
  }

  private static Entity enemyWith(CombatStatsComponent stats) {
    return new Entity().addComponent(stats);
  }

  private static ResolvedCardEffect enemyEffect(
      EffectType type, TargetType target, int value, int duration, int sequence) {
    return new ResolvedCardEffect("enemy_card", type, target, value, duration, sequence);
  }
}
