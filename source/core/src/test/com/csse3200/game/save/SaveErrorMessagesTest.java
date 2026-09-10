package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SaveErrorMessagesTest {

  @Test
  void successfulSaveHasFriendlyMessage() {
    SaveResult result = SaveResult.success(new SaveSlotMetadata());
    assertEquals("Game saved.", SaveErrorMessages.forSave(result));
  }

  @Test
  void malformedSaveHasReadableMessage() {
    LoadResult result = LoadResult.failure(SaveError.MALFORMED_SAVE, "");
    assertEquals(
        "That save file is corrupted and can't be loaded.", SaveErrorMessages.forLoad(result));
  }

  @Test
  void failureMessageIncludesRawDetailWhenPresent() {
    DeleteSaveResult result = DeleteSaveResult.failure(SaveError.IO_ERROR, "disk full");
    String message = SaveErrorMessages.forDelete(result);
    assertTrue(message.contains("Couldn't read or write"));
    assertTrue(message.contains("disk full"));
  }

  @Test
  void everyErrorCodeHasAMessage() {
    for (SaveError error : SaveError.values()) {
      if (error == SaveError.NONE) continue;
      SaveResult result = SaveResult.failure(error, "");
      assertTrue(
          !SaveErrorMessages.forSave(result).isBlank(), "Missing message for " + error);
    }
  }
}
