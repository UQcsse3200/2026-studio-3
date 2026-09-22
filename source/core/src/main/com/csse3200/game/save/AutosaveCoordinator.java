package com.csse3200.game.save;

import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.RunState;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Saves a completed encounter only after its screen has released the live player state. */
public class AutosaveCoordinator {
  public static final int AUTOSAVE_SLOT_ID = 4;

  private static final Logger logger = LoggerFactory.getLogger(AutosaveCoordinator.class);

  private final RunState runState;
  private final Supplier<SaveGameService> saveServiceFactory;
  private MapGraph pendingRun;

  public AutosaveCoordinator(RunState runState, Supplier<SaveGameService> saveServiceFactory) {
    this.runState = Objects.requireNonNull(runState, "runState must not be null");
    this.saveServiceFactory =
        Objects.requireNonNull(saveServiceFactory, "saveServiceFactory must not be null");
  }

  /** Called after a successful encounter has advanced the map. No file is written yet. */
  public void requestAfterSuccessfulEncounter() {
    if (runState.isRunActive() && runState.getActiveNodeId() == null) {
      pendingRun = runState.getMapGraph();
    }
  }

  /**
   * Called when the map is shown, after the outgoing encounter screen has been disposed. A request
   * belongs to one particular run, so starting or loading another run cannot accidentally save that
   * run. A failed write is reported but is not retried on every map visit.
   */
  public void saveIfPending() {
    if (pendingRun == null) {
      return;
    }
    if (pendingRun != runState.getMapGraph() || !runState.isRunActive()) {
      pendingRun = null;
      return;
    }
    if (runState.getActiveNodeId() != null) {
      return;
    }

    pendingRun = null;
    try {
      // Build the snapshot provider for this write: player/deck objects can change between runs.
      SaveResult result = saveServiceFactory.get().saveGame(AUTOSAVE_SLOT_ID);
      if (!result.success()) {
        logger.warn("Autosave failed: {} ({})", result.error(), result.message());
      } else {
        logger.info("Autosaved completed encounter to slot {}", AUTOSAVE_SLOT_ID);
      }
    } catch (RuntimeException exception) {
      // An autosave must never prevent the player from returning to the map.
      logger.error("Autosave failed unexpectedly", exception);
    }
  }
}
