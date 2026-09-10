package com.csse3200.game.save;

/** Captures the live run as serializable save data. Implemented by the state-integration layer. */
@FunctionalInterface
public interface SaveGameSnapshotProvider {
  SaveGameData capture();
}
