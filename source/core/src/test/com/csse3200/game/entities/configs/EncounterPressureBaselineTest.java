package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Locks Sprint 4 balance baselines for representative normal, elite and boss encounters.
 *
 * <p>These are config-derived pressure numbers (scaled HP / attack after {@link EnemyScaling}), not
 * full playtest telemetry. Live turn/HP-remaining records live in {@code
 * docs/balance/sprint4-baselines.md}.
 */
@ExtendWith(GameExtension.class)
class EncounterPressureBaselineTest {
  private EnemyConfigs roster;

  @BeforeEach
  void loadRoster() {
    roster = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");
    assertNotNull(roster);
  }

  @Test
  void normalEncounterBaselinesAtProgressionZero() {
    assertAll(
        () -> assertPressure("bone_crawler", 0, 30, 5, 2),
        () -> assertPressure("lesser_shade", 0, 24, 6, 0),
        () -> assertPressure("dark_acolyte", 0, 34, 7, 1),
        () -> assertEquals(48, totalHealth(0, "lesser_shade", "lesser_shade")),
        () -> assertEquals(54, totalHealth(0, "bone_crawler", "lesser_shade")));
  }

  @Test
  void eliteAndBossBaselinesAtProgressionZero() {
    assertAll(
        () -> assertPressure("tomb_guardian", 0, 72, 10, 5),
        () -> assertPressure("void_knight", 0, 40, 16, 0),
        () -> assertPressure("boss_knight", 0, 150, 14, 8),
        () -> assertEquals(96, totalHealth(0, "tomb_guardian", "lesser_shade")),
        () -> assertEquals(40, totalHealth(0, "void_knight")));
  }

  @Test
  void midRunScalingRaisesPressureAtProgressionFive() {
    // NORMAL: +8% HP / +5% ATK per step. ELITE: +10% HP / +7% ATK. Armour unscaled.
    // Boss tier uses the NORMAL rates in EnemyScaling.
    assertAll(
        () -> assertPressure("bone_crawler", 5, 42, 6, 2),
        () -> assertPressure("lesser_shade", 5, 34, 8, 0),
        () -> assertPressure("tomb_guardian", 5, 108, 14, 5),
        () -> assertPressure("void_knight", 5, 60, 22, 0),
        () -> assertPressure("boss_knight", 5, 210, 18, 8));
  }

  private void assertPressure(String enemyId, int progression, int health, int attack, int armour) {
    EnemyConfig scaled = EnemyScaling.scale(roster.get(enemyId), progression);
    assertEquals(health, scaled.health, enemyId + " health @p" + progression);
    assertEquals(attack, scaled.baseAttack, enemyId + " attack @p" + progression);
    assertEquals(armour, scaled.armour, enemyId + " armour @p" + progression);
  }

  private int totalHealth(int progression, String... enemyIds) {
    int total = 0;
    for (String id : enemyIds) {
      total += EnemyScaling.scale(roster.get(id), progression).health;
    }
    return total;
  }
}
