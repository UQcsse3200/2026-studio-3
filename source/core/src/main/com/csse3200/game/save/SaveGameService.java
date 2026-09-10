package com.csse3200.game.save;

import java.time.Clock;

/** Public orchestration API for creating, loading, listing and deleting save slots. */
public class SaveGameService {
  private final SaveGameRepository repository;
  private final SaveGameSnapshotProvider snapshotProvider;
  private final Clock clock;

  /** Creates a service whose callers provide {@link SaveGameData} when saving. */
  public SaveGameService(SaveGameRepository repository) {
    this(repository, null, Clock.systemUTC());
  }

  /** Creates a service that can capture the current run through the supplied provider. */
  public SaveGameService(SaveGameRepository repository, SaveGameSnapshotProvider snapshotProvider) {
    this(repository, snapshotProvider, Clock.systemUTC());
  }

  /** Creates a service with an explicit clock for deterministic tests and platform integration. */
  public SaveGameService(
      SaveGameRepository repository, SaveGameSnapshotProvider snapshotProvider, Clock clock) {
    if (repository == null) {
      throw new IllegalArgumentException("repository must not be null");
    }
    if (clock == null) {
      throw new IllegalArgumentException("clock must not be null");
    }
    this.repository = repository;
    this.snapshotProvider = snapshotProvider;
    this.clock = clock;
  }

  /** Captures the live run and writes it to the selected slot. */
  public SaveResult saveGame(int slotId) {
    if (slotId <= 0) {
      return SaveResult.failure(SaveError.INVALID_SLOT, "Save slot ID must be positive: " + slotId);
    }
    if (snapshotProvider == null) {
      return SaveResult.failure(
          SaveError.NO_SAVE_DATA, "No save-game snapshot provider is configured");
    }

    try {
      return saveGame(slotId, snapshotProvider.capture());
    } catch (RuntimeException exception) {
      return SaveResult.failure(SaveError.CAPTURE_FAILED, "Unable to capture the current run");
    }
  }

  /** Writes already-captured state to the selected slot. */
  public SaveResult saveGame(int slotId, SaveGameData data) {
    if (slotId <= 0) {
      return SaveResult.failure(SaveError.INVALID_SLOT, "Save slot ID must be positive: " + slotId);
    }
    if (data == null) {
      return SaveResult.failure(SaveError.NO_SAVE_DATA, "Save data must not be null");
    }

    data.schemaVersion = SaveGameData.CURRENT_SCHEMA_VERSION;
    if (data.metadata == null) {
      data.metadata = new SaveSlotMetadata();
    }
    data.metadata.slotId = slotId;
    data.metadata.savedAtEpochMillis = clock.millis();
    data.metadata.loadable = true;
    data.metadata.statusMessage = "";
    if ((data.metadata.resumeScreen == null || data.metadata.resumeScreen.isBlank())
        && data.progress != null) {
      data.metadata.resumeScreen = safe(data.progress.resumeScreen);
    }

    return repository.save(slotId, data);
  }

  /** Reads and parses a slot without mutating live game state. */
  public LoadResult loadGame(int slotId) {
    if (slotId <= 0) {
      return LoadResult.failure(SaveError.INVALID_SLOT, "Save slot ID must be positive: " + slotId);
    }

    LoadResult result = repository.load(slotId);
    if (!result.success()) {
      return result;
    }

    SaveGameData data = result.data();
    if (data == null || data.metadata == null || data.metadata.slotId != slotId) {
      return LoadResult.failure(
          SaveError.MALFORMED_SAVE, "Save metadata does not match the requested slot");
    }
    if (data.schemaVersion != SaveGameData.CURRENT_SCHEMA_VERSION) {
      return LoadResult.failure(
          SaveError.UNSUPPORTED_VERSION, "Unsupported save version " + data.schemaVersion);
    }
    return result;
  }

  /** Returns metadata for every discovered slot, including unreadable placeholders. */
  public SaveSlotListResult listSaveSlots() {
    return repository.listSaveSlots();
  }

  /** Deletes the selected save slot. */
  public DeleteSaveResult deleteSave(int slotId) {
    if (slotId <= 0) {
      return DeleteSaveResult.failure(
          SaveError.INVALID_SLOT, "Save slot ID must be positive: " + slotId);
    }
    return repository.delete(slotId);
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
