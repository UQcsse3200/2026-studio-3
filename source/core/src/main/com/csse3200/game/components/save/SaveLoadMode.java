package com.csse3200.game.components.save;

/** Controls which operations a save/load view exposes. */
public enum SaveLoadMode {
  /** Safe screens such as the map may create, replace, load and delete manual saves. */
  SAVE_AND_LOAD(true, true),

  /** The main menu may load and delete saves, but has no active run state to save. */
  LOAD_AND_DELETE(false, true),

  /** Unsafe or pre-run screens may only load an existing save. */
  LOAD_ONLY(false, false);

  private final boolean saveAllowed;
  private final boolean deleteAllowed;

  SaveLoadMode(boolean saveAllowed, boolean deleteAllowed) {
    this.saveAllowed = saveAllowed;
    this.deleteAllowed = deleteAllowed;
  }

  public boolean isSaveAllowed() {
    return saveAllowed;
  }

  public boolean isDeleteAllowed() {
    return deleteAllowed;
  }
}
