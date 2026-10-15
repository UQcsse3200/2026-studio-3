package com.csse3200.game.components.save;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SaveLoadModeTest {
  @Test
  void mapModeAllowsSaveAndDelete() {
    assertTrue(SaveLoadMode.SAVE_AND_LOAD.isSaveAllowed());
    assertTrue(SaveLoadMode.SAVE_AND_LOAD.isDeleteAllowed());
  }

  @Test
  void mainMenuModeAllowsDeleteWithoutSave() {
    assertFalse(SaveLoadMode.LOAD_AND_DELETE.isSaveAllowed());
    assertTrue(SaveLoadMode.LOAD_AND_DELETE.isDeleteAllowed());
  }

  @Test
  void loadOnlyModeCannotMutateSlots() {
    assertFalse(SaveLoadMode.LOAD_ONLY.isSaveAllowed());
    assertFalse(SaveLoadMode.LOAD_ONLY.isDeleteAllowed());
  }
}
