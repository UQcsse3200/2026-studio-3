package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.configs.EncounterConfig;
import com.csse3200.game.entities.configs.EncounterConfigs;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RoomType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EncounterComposerTest {

  private static EncounterConfig encounter(String id, int weight, String... enemies) {
    EncounterConfig config = new EncounterConfig();
    config.id = id;
    config.roomType = RoomType.COMBAT;
    config.weight = weight;
    config.enemies = enemies;
    return config;
  }

  private static EncounterConfigs table(EncounterConfig... encounters) {
    EncounterConfigs configs = new EncounterConfigs();
    configs.encounters = encounters;
    return configs;
  }

  private static EncounterConfigs threeCandidates() {
    return table(encounter("x", 10, "a"), encounter("y", 8, "b"), encounter("z", 10, "c"));
  }

  @Test
  void shouldReturnOnlyCandidateEnemiesInSpawnOrder() {
    EncounterComposer composer =
        new EncounterComposer(table(encounter("pair", 1, "a", "b")), id -> true);

    assertEquals(List.of("a", "b"), composer.compose(RoomType.COMBAT, 0, 42L));
  }

  @Test
  void shouldFallBackWhenTableIsMissing() {
    EncounterComposer composer = new EncounterComposer(null, id -> true);

    assertEquals(EncounterComposer.DEFAULT_ENEMIES, composer.compose(RoomType.COMBAT, 0, 42L));
  }

  @Test
  void shouldFallBackWhenNothingMatches() {
    EncounterComposer composer =
        new EncounterComposer(table(encounter("combat", 1, "a")), id -> true);

    assertEquals(EncounterComposer.DEFAULT_ENEMIES, composer.compose(RoomType.SHOP, 0, 42L));
  }

  @Test
  void shouldFallBackWhenOnlyCandidateHasUnknownEnemy() {
    EncounterComposer composer =
        new EncounterComposer(table(encounter("typo", 1, "bone_crawer")), Set.of("a")::contains);

    assertEquals(EncounterComposer.DEFAULT_ENEMIES, composer.compose(RoomType.COMBAT, 0, 42L));
  }

  @Test
  void shouldNeverPickCandidateWithUnknownEnemy() {
    EncounterComposer composer =
        new EncounterComposer(
            table(encounter("broken", 100, "a", "missing"), encounter("good", 1, "a")),
            Set.of("a")::contains);

    for (long seed = 0; seed < 50; seed++) {
      assertEquals(List.of("a"), composer.compose(RoomType.COMBAT, 0, seed));
    }
  }

  @Test
  void shouldReturnUnmodifiableLineUp() {
    EncounterComposer composer =
        new EncounterComposer(table(encounter("pair", 1, "a", "b")), id -> true);

    List<String> lineUp = composer.compose(RoomType.COMBAT, 0, 42L);

    assertThrows(UnsupportedOperationException.class, () -> lineUp.add("c"));
  }

  @Test
  void shouldRejectMissingEnemyCheck() {
    EncounterConfigs configs = table(encounter("pair", 1, "a"));

    assertThrows(NullPointerException.class, () -> new EncounterComposer(configs, null));
  }

  @Test
  void shouldReturnSameLineUpForSameSeed() {
    EncounterConfigs configs = threeCandidates();

    List<String> first = new EncounterComposer(configs, id -> true).compose(RoomType.COMBAT, 0, 7L);
    List<String> afterReload =
        new EncounterComposer(configs, id -> true).compose(RoomType.COMBAT, 0, 7L);

    assertEquals(first, afterReload);
  }

  @Test
  void shouldNotDependOnEarlierCalls() {
    EncounterComposer composer = new EncounterComposer(threeCandidates(), id -> true);

    List<String> before = composer.compose(RoomType.COMBAT, 0, 7L);
    for (long seed = 0; seed < 100; seed++) {
      composer.compose(RoomType.COMBAT, 0, seed);
    }

    assertEquals(before, composer.compose(RoomType.COMBAT, 0, 7L));
  }

  @Test
  void shouldEventuallyPickEveryCandidate() {
    EncounterComposer composer = new EncounterComposer(threeCandidates(), id -> true);

    Set<List<String>> seen = new HashSet<>();
    for (long seed = 0; seed < 200; seed++) {
      seen.add(composer.compose(RoomType.COMBAT, 0, seed));
    }

    assertEquals(3, seen.size());
  }

  @Test
  void shouldSpreadConsecutiveSeedsAcrossEqualWeights() {
    EncounterComposer composer =
        new EncounterComposer(
            table(encounter("left", 1, "a"), encounter("right", 1, "b")), id -> true);

    int left = 0;
    for (long seed = 0; seed < 100; seed++) {
      if (composer.compose(RoomType.COMBAT, 0, seed).equals(List.of("a"))) {
        left++;
      }
    }

    assertTrue(left >= 30 && left <= 70, "left was picked " + left + " times out of 100");
  }

  @Test
  void shouldFollowWeights() {
    EncounterComposer composer =
        new EncounterComposer(
            table(encounter("rare", 1, "a"), encounter("common", 9, "b")), id -> true);

    int common = 0;
    for (long seed = 0; seed < 1000; seed++) {
      if (composer.compose(RoomType.COMBAT, 0, seed).equals(List.of("b"))) {
        common++;
      }
    }

    assertTrue(common >= 850 && common <= 950, "common was picked " + common + " times");
  }
}
