package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.extensions.GameExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class CardDiscoveryStoreTest {
  @TempDir Path directory;

  @Test
  void missingFileReturnsEmptyProgress() {
    CardDiscoveryStore store = storeAt("missing.json");

    assertTrue(store.load().isEmpty());
  }

  @Test
  void savesAndLoadsProgress() {
    CardDiscoveryStore store = storeAt("card_discovery.json");

    store.save(Map.of("strike", CardUnlockState.SEEN));

    assertEquals(Map.of("strike", CardUnlockState.SEEN), store.load());
  }

  @Test
  void doesNotWriteLockedEntries() throws IOException {
    Path file = directory.resolve("card_discovery.json");
    CardDiscoveryStore store = storeAt(file.getFileName().toString());
    Map<String, CardUnlockState> progress = new LinkedHashMap<>();
    progress.put("strike", CardUnlockState.SEEN);
    progress.put("defend", CardUnlockState.LOCKED);

    store.save(progress);

    String serialised = Files.readString(file);
    assertTrue(serialised.contains("strike"));
    assertFalse(serialised.contains("defend"));
  }

  @Test
  void malformedFileReturnsEmptyProgressWithoutThrowing() throws IOException {
    Path file = directory.resolve("card_discovery.json");
    Files.writeString(file, "{not valid json");
    CardDiscoveryStore store = storeAt(file.getFileName().toString());

    Map<String, CardUnlockState> loaded = assertDoesNotThrow(store::load);

    assertTrue(loaded.isEmpty());
  }

  @Test
  void ignoresUnknownStateNames() throws IOException {
    Path file = directory.resolve("card_discovery.json");
    Files.writeString(
        file,
        """
        {
          "progress": [
            {"cardId": "strike", "unlockState": "FUTURE_STATE"},
            {"cardId": "defend", "unlockState": "SEEN"}
          ]
        }
        """);
    CardDiscoveryStore store = storeAt(file.getFileName().toString());

    assertEquals(Map.of("defend", CardUnlockState.SEEN), store.load());
  }

  @Test
  void skipsInvalidEntriesButKeepsValidOnes() throws IOException {
    Path file = directory.resolve("card_discovery.json");
    Files.writeString(
        file,
        """
        {
          "progress": [
            {"cardId": "", "unlockState": "SEEN"},
            {"cardId": "defend", "unlockState": "SEEN"}
          ]
        }
        """);
    CardDiscoveryStore store = storeAt(file.getFileName().toString());

    assertEquals(Map.of("defend", CardUnlockState.SEEN), store.load());
  }

  private CardDiscoveryStore storeAt(String fileName) {
    return new CardDiscoveryStore(Gdx.files.absolute(directory.resolve(fileName).toString()));
  }
}
