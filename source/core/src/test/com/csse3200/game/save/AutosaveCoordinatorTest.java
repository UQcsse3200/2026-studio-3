package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class AutosaveCoordinatorTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesOnceOnlyAfterMapIsReady() {
    RunState runState = activeRun();
    SaveGameService service = mock(SaveGameService.class);
    when(service.saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID))
        .thenReturn(SaveResult.success(new SaveSlotMetadata()));
    AutosaveCoordinator coordinator = new AutosaveCoordinator(runState, () -> service);

    coordinator.saveIfPending();
    verify(service, never()).saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);

    runState.enterEncounter(1);
    runState.completeEncounter(true);
    coordinator.requestAfterSuccessfulEncounter();
    runState.enterEncounter(1);
    coordinator.saveIfPending();
    verify(service, never()).saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);

    runState.abandonEncounter();
    coordinator.saveIfPending();
    coordinator.saveIfPending();
    verify(service).saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);
  }

  @Test
  void neverSchedulesWithoutActiveRun() {
    RunState runState = mock(RunState.class);
    SaveGameService service = mock(SaveGameService.class);
    AutosaveCoordinator coordinator = new AutosaveCoordinator(runState, () -> service);

    coordinator.requestAfterSuccessfulEncounter();
    coordinator.saveIfPending();

    verify(service, never()).saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);
  }

  @Test
  void discardsRequestFromPreviousRun() {
    RunState runState = activeRun();
    SaveGameService service = mock(SaveGameService.class);
    AutosaveCoordinator coordinator = new AutosaveCoordinator(runState, () -> service);

    runState.enterEncounter(1);
    runState.completeEncounter(true);
    coordinator.requestAfterSuccessfulEncounter();
    runState.endRun();
    assertTrue(runState.startRun(newMap(), 0));
    coordinator.saveIfPending();
    coordinator.saveIfPending();

    verify(service, never()).saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);
  }

  @Test
  void doesNotRetryFailedSaveOnEveryMapVisit() {
    RunState runState = activeRun();
    SaveGameService service = mock(SaveGameService.class);
    when(service.saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID))
        .thenReturn(SaveResult.failure(SaveError.IO_ERROR, "Disk unavailable"));
    AutosaveCoordinator coordinator = new AutosaveCoordinator(runState, () -> service);

    runState.enterEncounter(1);
    runState.completeEncounter(true);
    coordinator.requestAfterSuccessfulEncounter();
    coordinator.saveIfPending();
    coordinator.saveIfPending();

    verify(service).saveGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);
  }

  @Test
  void autosaveRestoresAfterRelaunchWithoutOverwritingManualSlot() {
    RunState runState = activeRun();

    PlayerRunState player = new PlayerRunState(75, 100, 50);
    SaveGameService service =
        new SaveGameService(
            new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile())),
            new GameStateSnapshotProvider(
                player,
                PlayerDeckFactory.createStarterDeck(),
                runState,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault()));
    assertTrue(service.saveGame(1).success());

    assertTrue(runState.getMapGraph().moveToNode(1));
    runState.enterEncounter(1);
    runState.completeEncounter(true);
    player.restore(43, 100, 28);
    AutosaveCoordinator coordinator = new AutosaveCoordinator(runState, () -> service);
    coordinator.requestAfterSuccessfulEncounter();
    coordinator.saveIfPending();

    // New service and run-scoped objects simulate quitting and launching the game again.
    SaveGameService relaunchedService =
        new SaveGameService(
            new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile())));
    assertEquals(75, relaunchedService.loadGame(1).data().player.currentHealth);
    LoadResult autosave = relaunchedService.loadGame(AutosaveCoordinator.AUTOSAVE_SLOT_ID);
    assertTrue(autosave.success());
    assertEquals(43, autosave.data().player.currentHealth);
    assertEquals(28, autosave.data().player.gold);
    assertEquals(
        NodeState.COMPLETED.name(),
        autosave.data().map.nodes.stream()
            .filter(node -> node.nodeId == 1)
            .findFirst()
            .orElseThrow()
            .state);

    PlayerRunState restoredPlayer = new PlayerRunState(1, 10, 0);
    RunState restoredRun = new RunState();
    RestoreResult restored =
        new SaveGameRestoreService(
                restoredPlayer,
                PlayerDeckFactory.createStarterDeck(),
                restoredRun,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault())
            .restore(autosave.data());
    assertTrue(restored.success());
    assertEquals(43, restoredPlayer.getCurrentHealth());
    assertEquals(28, restoredPlayer.getGold());
    assertEquals(1, restoredRun.getMapGraph().getCurrentNode().getNodeId());
  }

  private RunState activeRun() {
    RunState runState = new RunState();
    assertTrue(runState.startRun(newMap(), 0));
    return runState;
  }

  private MapGraph newMap() {
    MapNode start = new MapNode(0, RoomType.COMBAT);
    MapNode shop = new MapNode(1, RoomType.SHOP);
    start.addConnection(shop);
    return new MapGraph(Map.of(0, start, 1, shop), false);
  }
}
