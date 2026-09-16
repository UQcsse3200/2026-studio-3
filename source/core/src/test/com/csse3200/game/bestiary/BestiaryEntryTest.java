package com.csse3200.game.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyTier;
import org.junit.jupiter.api.Test;

class BestiaryEntryTest {
  @Test
  void shouldCopyEnemyConfigurationIntoImmutableDefinition() {
    EnemyConfig config = enemyConfig();

    BestiaryEntry entry = BestiaryEntry.fromEnemyConfig(config, "  A patient hunter.  ");
    config.name = "Changed later";
    config.health = 1;

    assertEquals("shadow_stalker", entry.enemyId());
    assertEquals("Shadow Stalker", entry.name());
    assertEquals(EnemyTier.ELITE, entry.tier());
    assertEquals(42, entry.health());
    assertEquals(8, entry.baseAttack());
    assertEquals(3, entry.armour());
    assertEquals("cycle_attack_defend", entry.behaviour());
    assertEquals("images/enemies/shadow.atlas", entry.sprite());
    assertEquals("A patient hunter.", entry.description());
  }

  @Test
  void shouldNormaliseOptionalAndFallbackValues() {
    EnemyConfig config = enemyConfig();
    config.name = "   ";
    config.tier = null;
    config.behaviour = null;
    config.sprite = "   ";

    BestiaryEntry entry = BestiaryEntry.fromEnemyConfig(config, null);

    assertEquals("Unknown Enemy", entry.name());
    assertEquals(EnemyTier.NORMAL, entry.tier());
    assertEquals("", entry.behaviour());
    assertEquals("images/enemies/shadow_stalker.atlas", entry.sprite());
    assertEquals("", entry.description());
  }

  @Test
  void shouldRejectInvalidDefinitions() {
    assertThrows(IllegalArgumentException.class, () -> BestiaryEntry.fromEnemyConfig(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry(" unknown ", "Name", null, 1, 0, 0, "", "", ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry("enemy", "Name", null, 0, 0, 0, "", "", ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry("enemy", "Name", null, 1, -1, 0, "", "", ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry("enemy", "Name", null, 1, 0, -1, "", "", ""));
  }

  private static EnemyConfig enemyConfig() {
    EnemyConfig config = new EnemyConfig();
    config.id = "shadow_stalker";
    config.name = "Shadow Stalker";
    config.tier = EnemyTier.ELITE;
    config.health = 42;
    config.baseAttack = 8;
    config.armour = 3;
    config.behaviour = "cycle_attack_defend";
    config.sprite = "images/enemies/shadow.atlas";
    return config;
  }
}
