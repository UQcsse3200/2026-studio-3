package com.csse3200.game.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BestiaryTrackingComponentTest {
  @Test
  void shouldTrackEncounterAndDefeatFromEnemyLifecycle() {
    BestiaryService service = serviceWithEnemy("shade");
    CombatStatsComponent stats = new CombatStatsComponent(10, 2);
    Entity enemy =
        new Entity()
            .addComponent(stats)
            .addComponent(new EnemyStatsComponent("Lesser Shade"))
            .addComponent(new BestiaryTrackingComponent("shade", service));

    enemy.create();
    assertEquals(
        BestiaryUnlockState.ENCOUNTERED, service.getEntry("shade").orElseThrow().unlockState());

    stats.takeDamage(10);
    assertEquals(
        BestiaryUnlockState.DEFEATED, service.getEntry("shade").orElseThrow().unlockState());
  }

  @Test
  void shouldExposeTrackedEnemyId() {
    BestiaryTrackingComponent tracker =
        new BestiaryTrackingComponent("shade", serviceWithEnemy("shade"));

    assertEquals("shade", tracker.getEnemyId());
  }

  @Test
  void shouldRejectInvalidDependencies() {
    BestiaryService service = serviceWithEnemy("shade");

    assertThrows(IllegalArgumentException.class, () -> new BestiaryTrackingComponent(" ", service));
    assertThrows(
        IllegalArgumentException.class, () -> new BestiaryTrackingComponent("shade", null));
  }

  private static BestiaryService serviceWithEnemy(String id) {
    EnemyConfig config = new EnemyConfig();
    config.id = id;
    config.name = "Lesser Shade";
    config.health = 10;

    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[] {config};
    return new BestiaryService(configs);
  }
}
