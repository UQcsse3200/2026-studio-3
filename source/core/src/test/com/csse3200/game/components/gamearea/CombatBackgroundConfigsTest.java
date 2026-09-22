package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CombatBackgroundConfigsTest {
  @Test
  void shouldFindBackgroundById() {
    CombatBackgroundConfigs configs = new CombatBackgroundConfigs();
    CombatBackgroundConfig forest = new CombatBackgroundConfig();
    forest.id = "forest";
    CombatBackgroundConfig temple = new CombatBackgroundConfig();
    temple.id = "temple";
    configs.backgrounds.add(forest);
    configs.backgrounds.add(temple);

    assertSame(temple, configs.get("temple"));
  }

  @Test
  void shouldReturnNullForUnknownId() {
    CombatBackgroundConfigs configs = new CombatBackgroundConfigs();
    CombatBackgroundConfig forest = new CombatBackgroundConfig();
    forest.id = "forest";
    configs.backgrounds.add(forest);

    assertNull(configs.get("unknown"));
  }

  @Test
  void shouldReturnNullForNullId() {
    CombatBackgroundConfigs configs = new CombatBackgroundConfigs();
    CombatBackgroundConfig forest = new CombatBackgroundConfig();
    forest.id = "forest";
    configs.backgrounds.add(forest);

    assertNull(configs.get(null));
  }

  @Test
  void shouldReturnNullForEmptyCollection() {
    CombatBackgroundConfigs configs = new CombatBackgroundConfigs();

    assertNull(configs.get("forest"));
  }
}
