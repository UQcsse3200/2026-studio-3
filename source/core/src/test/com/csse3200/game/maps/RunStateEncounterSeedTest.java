package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.extensions.GameExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RunStateEncounterSeedTest {

  private static MapGraph oneNodeMap() {
    return new MapGraph(Map.of(0, new MapNode(0, RoomType.START)), false);
  }

  @Test
  void shouldHaveNoSeedBeforeRunStarts() {
    assertNull(new RunState().getEncounterSeed());
  }

  @Test
  void shouldCreateSeedWhenRunStarts() {
    RunState runState = new RunState();

    runState.startRun(oneNodeMap(), 0);

    assertNotNull(runState.getEncounterSeed());
  }

  @Test
  void shouldCreateNewSeedForEachRun() {
    RunState runState = new RunState();
    runState.startRun(oneNodeMap(), 0);
    Long firstRun = runState.getEncounterSeed();

    runState.endRun();
    runState.startRun(oneNodeMap(), 0);

    assertNotEquals(firstRun, runState.getEncounterSeed());
  }

  @Test
  void shouldClearSeedWhenRunEnds() {
    RunState runState = new RunState();
    runState.startRun(oneNodeMap(), 0);

    runState.endRun();

    assertNull(runState.getEncounterSeed());
  }

  @Test
  void shouldGiveRestoredRunAFallbackSeed() {
    RunState runState = new RunState();

    runState.restoreRun(oneNodeMap(), null);

    assertNotNull(runState.getEncounterSeed());
  }

  @Test
  void shouldPutBackSavedSeed() {
    RunState runState = new RunState();
    runState.restoreRun(oneNodeMap(), null);

    runState.restoreEncounterSeed(42L);

    assertEquals(42L, runState.getEncounterSeed());
  }

  @Test
  void shouldKeepFallbackSeedForOldSaves() {
    RunState runState = new RunState();
    runState.restoreRun(oneNodeMap(), null);
    Long fallback = runState.getEncounterSeed();

    runState.restoreEncounterSeed(null);

    assertEquals(fallback, runState.getEncounterSeed());
  }

  @Test
  void shouldNotCreateSeedWhenOnlySettingMap() {
    RunState runState = new RunState();

    runState.setMapGraph(oneNodeMap());

    assertNull(runState.getEncounterSeed());
  }
}
