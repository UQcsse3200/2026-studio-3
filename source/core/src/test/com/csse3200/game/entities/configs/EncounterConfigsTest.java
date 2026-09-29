package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.RoomType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EncounterConfigsTest {

  private static EncounterConfig encounter(String id, RoomType type, String... enemies) {
    EncounterConfig config = new EncounterConfig();
    config.id = id;
    config.roomType = type;
    config.enemies = enemies;
    return config;
  }

  private static EncounterConfigs table(EncounterConfig... encounters) {
    EncounterConfigs configs = new EncounterConfigs();
    configs.encounters = encounters;
    return configs;
  }

  @Test
  void shouldIndexValidEncounter() {
    EncounterConfig config = encounter("pair", RoomType.COMBAT, "lesser_shade", "lesser_shade");

    EncounterConfigs configs = table(config);

    assertSame(config, configs.get("pair"));
    assertEquals(1, configs.all().size());
  }

  @Test
  void shouldKeepFileOrder() {
    EncounterConfigs configs =
        table(
            encounter("first", RoomType.COMBAT, "a"),
            encounter("second", RoomType.COMBAT, "b"),
            encounter("third", RoomType.COMBAT, "c"));

    List<EncounterConfig> all = configs.all();

    assertEquals("first", all.get(0).id);
    assertEquals("second", all.get(1).id);
    assertEquals("third", all.get(2).id);
  }

  @Test
  void shouldSkipNullEntry() {
    EncounterConfigs configs = table(null, encounter("valid", RoomType.COMBAT, "a"));

    assertEquals(1, configs.all().size());
  }

  @Test
  void shouldSkipMissingId() {
    EncounterConfig blank = encounter("", RoomType.COMBAT, "a");
    EncounterConfig nullId = encounter(null, RoomType.COMBAT, "a");
    EncounterConfig defaultId = encounter("unknown", RoomType.COMBAT, "a");

    assertTrue(table(blank, nullId, defaultId).all().isEmpty());
  }

  @Test
  void shouldKeepFirstOfDuplicateIds() {
    EncounterConfig first = encounter("dup", RoomType.COMBAT, "a");
    EncounterConfig second = encounter("dup", RoomType.ELITE, "b");

    EncounterConfigs configs = table(first, second);

    assertEquals(1, configs.all().size());
    assertSame(first, configs.get("dup"));
  }

  @Test
  void shouldSkipMissingRoomType() {
    assertTrue(table(encounter("no_room", null, "a")).all().isEmpty());
  }

  @Test
  void shouldSkipNonPositiveWeight() {
    EncounterConfig zero = encounter("zero", RoomType.COMBAT, "a");
    zero.weight = 0;
    EncounterConfig negative = encounter("negative", RoomType.COMBAT, "a");
    negative.weight = -3;

    assertTrue(table(zero, negative).all().isEmpty());
  }

  @Test
  void shouldSkipInvalidProgressionRange() {
    EncounterConfig negative = encounter("negative", RoomType.COMBAT, "a");
    negative.minProgression = -1;
    EncounterConfig inverted = encounter("inverted", RoomType.COMBAT, "a");
    inverted.minProgression = 5;
    inverted.maxProgression = 2;

    assertTrue(table(negative, inverted).all().isEmpty());
  }

  @Test
  void shouldAcceptSingleProgressionRange() {
    EncounterConfig exact = encounter("exact", RoomType.COMBAT, "a");
    exact.minProgression = 3;
    exact.maxProgression = 3;

    assertEquals(1, table(exact).all().size());
  }

  @Test
  void shouldSkipMissingOrEmptyEnemies() {
    EncounterConfig empty = encounter("empty", RoomType.COMBAT);
    EncounterConfig nullArray = encounter("null_array", RoomType.COMBAT);
    nullArray.enemies = null;

    assertTrue(table(empty, nullArray).all().isEmpty());
  }

  @Test
  void shouldSkipBlankEnemyId() {
    EncounterConfig blank = encounter("blank", RoomType.COMBAT, "a", " ");
    EncounterConfig nullEnemy = encounter("null_enemy", RoomType.COMBAT, "a", null);

    assertTrue(table(blank, nullEnemy).all().isEmpty());
  }

  @Test
  void shouldReturnNullForUnknownId() {
    assertNull(table(encounter("known", RoomType.COMBAT, "a")).get("missing"));
  }

  @Test
  void shouldMatchOnlySameRoomType() {
    EncounterConfig combat = encounter("combat", RoomType.COMBAT, "a");
    EncounterConfig elite = encounter("elite", RoomType.ELITE, "b");

    List<EncounterConfig> matches = table(combat, elite).matching(RoomType.ELITE, 0);

    assertEquals(1, matches.size());
    assertSame(elite, matches.get(0));
  }

  @Test
  void shouldMatchInclusiveProgressionBounds() {
    EncounterConfig ranged = encounter("ranged", RoomType.COMBAT, "a");
    ranged.minProgression = 2;
    ranged.maxProgression = 4;
    EncounterConfigs configs = table(ranged);

    assertTrue(configs.matching(RoomType.COMBAT, 1).isEmpty());
    assertEquals(1, configs.matching(RoomType.COMBAT, 2).size());
    assertEquals(1, configs.matching(RoomType.COMBAT, 4).size());
    assertTrue(configs.matching(RoomType.COMBAT, 5).isEmpty());
  }

  @Test
  void shouldTreatOmittedMaxProgressionAsOpenEnded() {
    EncounterConfig open = encounter("open", RoomType.COMBAT, "a");
    open.minProgression = 6;

    assertTrue(open.matches(RoomType.COMBAT, 1000));
    assertFalse(open.matches(RoomType.COMBAT, 5));
  }

  @Test
  void shouldReturnEmptyListWhenNothingMatches() {
    EncounterConfigs configs = table(encounter("combat", RoomType.COMBAT, "a"));

    assertTrue(configs.matching(RoomType.FINAL, 0).isEmpty());
  }

  @Test
  void shouldLoadFromJson() {
    EncounterConfigs configs =
        FileLoader.readClass(EncounterConfigs.class, "test/encounters/valid.json");

    assertNotNull(configs);
    EncounterConfig config = configs.get("test_pair");
    assertNotNull(config);
    assertEquals(RoomType.ELITE, config.roomType);
    assertEquals(1, config.minProgression);
    assertEquals(3, config.maxProgression);
    assertEquals(7, config.weight);
    assertArrayEquals(new String[] {"enemy_a", "enemy_b"}, config.enemies);
  }

  @Test
  void shouldApplyDefaultsForOmittedJsonFields() {
    EncounterConfigs configs =
        FileLoader.readClass(EncounterConfigs.class, "test/encounters/valid.json");

    EncounterConfig config = configs.get("test_defaults");
    assertNotNull(config);
    assertEquals(RoomType.COMBAT, config.roomType);
    assertEquals(0, config.minProgression);
    assertEquals(Integer.MAX_VALUE, config.maxProgression);
    assertEquals(1, config.weight);
  }
}
