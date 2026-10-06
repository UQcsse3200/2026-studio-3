package com.csse3200.game.narration;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class NarrationConfigLoaderTest {
  @TempDir Path directory;

  @Test
  void productionConfigDefinesAllStorySequences() {
    for (String id : new String[] {"opening", "pre_boss", "victory", "defeat"}) {
      assertFalse(
          NarrationConfigLoader.loadSequence(id).isEmpty(),
          "Missing or empty narration sequence: " + id);
    }
  }

  @Test
  void loadsPassagesInOrderAsImmutableLists() throws Exception {
    Path file = directory.resolve("narration.json");
    Files.writeString(file, "{\"opening\": [[\"Line one.\", \"Line two.\"], [\"Solo line.\"]]}");
    var opening = NarrationConfigLoader.loadSequence(file.toString(), "opening");
    assertEquals(2, opening.size());
    assertEquals(java.util.List.of("Line one.", "Line two."), opening.get(0));
    assertEquals(java.util.List.of("Solo line."), opening.get(1));
    var firstPassage = opening.get(0);
    var extraPassage = java.util.List.of("x");
    assertThrows(UnsupportedOperationException.class, () -> firstPassage.add("changed"));
    assertThrows(UnsupportedOperationException.class, () -> opening.add(extraPassage));
  }

  @Test
  void unknownSequenceReturnsEmpty() {
    assertTrue(NarrationConfigLoader.loadSequence("unknown").isEmpty());
    assertTrue(NarrationConfigLoader.loadSequence(null).isEmpty());
  }

  @Test
  void missingFileThrowsLoadingException() {
    String missingPath = directory.resolve("missing.json").toString();
    var error =
        assertThrows(
            NarrationLoadingException.class,
            () -> NarrationConfigLoader.loadSequence(missingPath, "opening"));
    assertTrue(error.getMessage().contains("does not exist"));
  }

  @Test
  void malformedFileWrapsParserFailure() throws Exception {
    Path file = directory.resolve("narration.json");
    Files.writeString(file, "{\"opening\": [");
    String filename = file.toString();
    var error =
        assertThrows(
            NarrationLoadingException.class,
            () -> NarrationConfigLoader.loadSequence(filename, "opening"));
    assertTrue(error.getMessage().contains("Malformed narration configuration file"));
    assertNotNull(error.getCause());
  }

  @Test
  void invalidPassagesAreRejectedEvenForUnknownId() throws Exception {
    Path file = directory.resolve("narration.json");
    String filename = file.toString();
    for (String json :
        new String[] {
          "[]",
          "{\"opening\": [[42]]}",
          "{\"opening\": [[]]}",
          "{\"opening\": [[\"\"]]}",
          "{\"opening\": [[\"a\",\"b\",\"c\",\"d\"]]}"
        }) {
      Files.writeString(file, json);
      assertThrows(
          NarrationLoadingException.class,
          () -> NarrationConfigLoader.loadSequence(filename, "unknown"));
    }
  }
}
