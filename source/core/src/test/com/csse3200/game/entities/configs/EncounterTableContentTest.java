package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.battle.EncounterComposer;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.MapGenerationConfig;
import com.csse3200.game.maps.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Guards the real {@code configs/encounters.json} against content mistakes. The loader skips bad
 * entries silently at runtime, so these checks are what catch a typo before it ships.
 */
@ExtendWith(GameExtension.class)
class EncounterTableContentTest {
  private static final RoomType[] BATTLE_ROOMS = {RoomType.COMBAT, RoomType.ELITE, RoomType.FINAL};

  private EncounterConfigs encounters;
  private EnemyConfigs roster;

  @BeforeEach
  void loadTables() {
    encounters = FileLoader.readClass(EncounterConfigs.class, "configs/encounters.json");
    roster = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");
  }

  @Test
  void shouldLoadEncounterTable() {
    assertNotNull(encounters, "configs/encounters.json failed to parse");
    assertNotNull(roster, "configs/enemies.json failed to parse");
  }

  @Test
  void shouldNotSkipAnyEntry() {
    assertEquals(
        encounters.encounters.length,
        encounters.all().size(),
        "An entry in encounters.json is invalid and was skipped; check the log for which one");
  }

  @Test
  void shouldOnlyReferenceKnownEnemies() {
    for (EncounterConfig encounter : encounters.all()) {
      for (String enemyId : encounter.enemies) {
        assertTrue(
            roster.contains(enemyId),
            "Encounter '" + encounter.id + "' references unknown enemy '" + enemyId + "'");
      }
    }
  }

  @Test
  void shouldCoverEveryBattleRoomAtEveryProgression() {
    for (RoomType room : BATTLE_ROOMS) {
      for (int progression = 0; progression < MapGenerationConfig.MAP_HEIGHT; progression++) {
        assertFalse(
            encounters.matching(room, progression).isEmpty(),
            "No encounter for " + room + " at progression " + progression);
      }
    }
  }

  @Test
  void shouldKeepBossOutOfOrdinaryRooms() {
    for (EncounterConfig encounter : encounters.all()) {
      if (encounter.roomType == RoomType.FINAL) {
        continue;
      }
      for (String enemyId : encounter.enemies) {
        assertFalse(
            roster.get(enemyId).tier == EnemyTier.BOSS,
            "Boss enemy '" + enemyId + "' appears in non-final encounter '" + encounter.id + "'");
      }
    }
  }

  @Test
  void shouldOnlyUseKnownEnemiesInDefaultLineUp() {
    for (String enemyId : EncounterComposer.DEFAULT_ENEMIES) {
      assertTrue(
          roster.contains(enemyId), "Default line-up references unknown enemy '" + enemyId + "'");
    }
  }
}
