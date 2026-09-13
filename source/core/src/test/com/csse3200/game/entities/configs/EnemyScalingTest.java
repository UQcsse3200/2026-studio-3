package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnemyScalingTest {

  @Test
  void shouldNotScaleAtZeroProgression() {
    EnemyConfig base = new EnemyConfig();
    base.health = 100;
    base.baseAttack = 20;
    base.tier = EnemyTier.NORMAL;

    EnemyConfig scaled = EnemyScaling.scale(base, 0);

    assertEquals(100, scaled.health);
    assertEquals(20, scaled.baseAttack);
  }

  @Test
  void shouldScaleNormalEnemy() {
    EnemyConfig base = new EnemyConfig();
    base.health = 100;
    base.baseAttack = 20;
    base.tier = EnemyTier.NORMAL;

    EnemyConfig scaled = EnemyScaling.scale(base, 3);

    assertEquals(124, scaled.health);
    assertEquals(23, scaled.baseAttack);
  }

  @Test
  void shouldScaleEliteEnemy() {
    EnemyConfig base = new EnemyConfig();
    base.health = 100;
    base.baseAttack = 100;
    base.tier = EnemyTier.ELITE;

    EnemyConfig scaled = EnemyScaling.scale(base, 3);

    assertEquals(130, scaled.health);
    assertEquals(121, scaled.baseAttack);
  }

  @Test
  void shouldTreatNegativeProgressionAsZero() {
    EnemyConfig base = new EnemyConfig();
    base.health = 100;
    base.baseAttack = 20;
    base.tier = EnemyTier.NORMAL;

    EnemyConfig scaled = EnemyScaling.scale(base, -3);

    assertEquals(100, scaled.health);
    assertEquals(20, scaled.baseAttack);
  }

  @Test
  void shouldPreserveNonScaledFields() {
    EnemyConfig base = new EnemyConfig();
    base.id = "test_enemy";
    base.name = "Test Enemy";
    base.health = 100;
    base.baseAttack = 20;
    base.armour = 4;
    base.tier = EnemyTier.ELITE;
    base.behaviour = "custom_behaviour";
    base.sprite = "images/enemies/custom.atlas";

    EnemyConfig scaled = EnemyScaling.scale(base, 5);

    assertEquals("test_enemy", scaled.id);
    assertEquals("Test Enemy", scaled.name);
    assertEquals(4, scaled.armour);
    assertEquals(EnemyTier.ELITE, scaled.tier);
    assertEquals("custom_behaviour", scaled.behaviour);
    assertEquals("images/enemies/custom.atlas", scaled.sprite);
  }
}
