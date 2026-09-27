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
  void loadsKnownSequencesAndPreservesLines() {
    var opening = NarrationConfigLoader.loadSequence("opening");
    assertEquals(3, opening.size());
    assertEquals(2, opening.get(0).size());
    assertEquals("[PLACEHOLDER] Opening passage one.", opening.get(0).get(0));
    assertEquals("[PLACEHOLDER] Text to be supplied by Ziqin.", opening.get(0).get(1));
    assertEquals(3, NarrationConfigLoader.loadSequence("pre_boss").size());
    assertThrows(UnsupportedOperationException.class, () -> opening.get(0).add("changed"));
  }

  @Test
  void unknownSequenceReturnsEmpty() {
    assertTrue(NarrationConfigLoader.loadSequence("unknown").isEmpty());
    assertTrue(NarrationConfigLoader.loadSequence(null).isEmpty());
  }

  @Test
  void missingFileThrowsLoadingException() {
    var error =
        assertThrows(
            NarrationLoadingException.class,
            () ->
                NarrationConfigLoader.loadSequence(
                    directory.resolve("missing.json").toString(), "opening"));
    assertTrue(error.getMessage().contains("does not exist"));
  }

  @Test
  void malformedFileWrapsParserFailure() throws Exception {
    Path file = directory.resolve("narration.json");
    Files.writeString(file, "{\"opening\": [");
    var error =
        assertThrows(
            NarrationLoadingException.class,
            () -> NarrationConfigLoader.loadSequence(file.toString(), "opening"));
    assertTrue(error.getMessage().contains("Malformed narration configuration file"));
    assertNotNull(error.getCause());
  }

  @Test
  void invalidPassagesAreRejectedEvenForUnknownId() throws Exception {
    Path file = directory.resolve("narration.json");
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
          () -> NarrationConfigLoader.loadSequence(file.toString(), "unknown"));
    }
  }
}
