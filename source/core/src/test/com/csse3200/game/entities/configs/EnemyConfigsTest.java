package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyConfigsTest {

  @Test
  void shouldIndexValidEnemy() {
    EnemyConfig enemy = new EnemyConfig();
    enemy.id = "test_enemy";
    enemy.health = 20;

    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[] {enemy};

    assertTrue(configs.contains("test_enemy"));
    assertSame(enemy, configs.get("test_enemy"));
  }

  @Test
  void shouldSkipEnemyWithMissingId() {
    EnemyConfig enemy = new EnemyConfig();
    enemy.id = "";
    enemy.health = 20;

    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[] {enemy};

    assertTrue(configs.ids().isEmpty());
  }

  @Test
  void shouldSkipEnemyWithInvalidHealth() {
    EnemyConfig enemy = new EnemyConfig();
    enemy.id = "broken_enemy";
    enemy.health = 0;

    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[] {enemy};

    assertTrue(configs.ids().isEmpty());
  }

  @Test
  void shouldLoadValidEnemyFromJson() {
    EnemyConfigs configs = FileLoader.readClass(EnemyConfigs.class, "test/enemies/valid.json");

    assertNotNull(configs);
    assertTrue(configs.contains("test_enemy"));

    EnemyConfig enemy = configs.get("test_enemy");
    assertNotNull(enemy);
    assertEquals(20, enemy.health);
    assertEquals(5, enemy.baseAttack);
    assertEquals(2, enemy.armour);
    assertEquals(EnemyTier.NORMAL, enemy.tier);
  }

  @Test
  void shouldSkipMissingIdFromJson() {
    EnemyConfigs configs = FileLoader.readClass(EnemyConfigs.class, "test/enemies/missing-id.json");

    assertNotNull(configs);
    assertTrue(configs.ids().isEmpty());
    assertFalse(configs.contains("unknown"));
  }

  @Test
  void shouldUseDefaultHealthWhenMissingFromJson() {
    EnemyConfigs configs =
        FileLoader.readClass(EnemyConfigs.class, "test/enemies/missing-health.json");

    assertNotNull(configs);

    EnemyConfig enemy = configs.enemies[0];

    assertEquals(1, enemy.health);
  }
}
