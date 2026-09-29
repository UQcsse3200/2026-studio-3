package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves the encounter seed survives a real save file and a relaunch, which is what stops a player
 * from rerolling a fight by quitting and loading.
 */
@ExtendWith(GameExtension.class)
class EncounterSeedSaveTest {

  @TempDir Path temporaryDirectory;

  private RunState runState;
  private SaveGameService saveGameService;

  @BeforeEach
  void setUp() {
    runState = new RunState();
    runState.startRun(oneNodeMap(), 0);
    GameStateSnapshotProvider provider =
        new GameStateSnapshotProvider(
            new PlayerRunState(50, 80, 0),
            PlayerDeckFactory.createStarterDeck(),
            runState,
            BestiaryService.loadDefault());
    saveGameService = new SaveGameService(repository(), provider);
  }

  @Test
  void shouldWriteSeedIntoSave() {
    SaveGameData loaded = saveAndLoad();

    assertEquals(runState.getEncounterSeed(), loaded.progress.encounterSeed);
  }

  @Test
  void shouldRestoreSameSeedAfterRelaunch() {
    RunState restored = relaunchAndRestore(saveAndLoad());

    assertEquals(runState.getEncounterSeed(), restored.getEncounterSeed());
  }

  @Test
  void shouldGiveOldSavesAFreshSeed() {
    SaveGameData loaded = saveAndLoad();
    loaded.progress.encounterSeed = null;

    RunState restored = relaunchAndRestore(loaded);

    assertNotNull(restored.getEncounterSeed());
  }

  private JsonSaveGameRepository repository() {
    return new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile()));
  }

  private SaveGameData saveAndLoad() {
    assertTrue(saveGameService.saveGame(1).success());
    // A fresh service simulates closing the game before loading.
    LoadResult loaded = new SaveGameService(repository()).loadGame(1);
    assertTrue(loaded.success());
    return loaded.data();
  }

  private RunState relaunchAndRestore(SaveGameData data) {
    RunState restored = new RunState();
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    deck.clear();
    RestoreResult result =
        new SaveGameRestoreService(
                new PlayerRunState(1, 10, 0), deck, restored, BestiaryService.loadDefault())
            .restore(data);
    assertTrue(result.success());
    return restored;
  }

  private static MapGraph oneNodeMap() {
    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(0, new MapNode(0, RoomType.COMBAT));
    return new MapGraph(nodes, false);
  }
}
