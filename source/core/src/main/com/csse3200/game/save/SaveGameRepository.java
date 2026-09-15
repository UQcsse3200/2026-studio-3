package com.csse3200.game.save;

/** Storage boundary for versioned save-slot files. */
public interface SaveGameRepository {
  SaveResult save(int slotId, SaveGameData data);

  LoadResult load(int slotId);

  SaveSlotListResult listSaveSlots();

  DeleteSaveResult delete(int slotId);
}
