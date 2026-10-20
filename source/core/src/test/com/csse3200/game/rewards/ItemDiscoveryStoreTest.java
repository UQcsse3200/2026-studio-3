package com.csse3200.game.rewards;

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
class ItemDiscoveryStoreTest {
  @TempDir Path directory;

  @Test
  void missingFileReturnsEmptyProgress() {
    assertTrue(storeAt("missing.json").load().isEmpty());
  }

  @Test
  void savesAndLoadsDiscoveredItems() {
    ItemDiscoveryStore store = storeAt("item_discovery.json");

    store.save(Map.of(ItemType.ENERGY_CRYSTAL, ItemUnlockState.DISCOVERED));

    assertEquals(Map.of(ItemType.ENERGY_CRYSTAL, ItemUnlockState.DISCOVERED), store.load());
  }

  @Test
  void doesNotWriteLockedEntries() throws IOException {
    Path file = directory.resolve("item_discovery.json");
    ItemDiscoveryStore store = storeAt(file.getFileName().toString());
    Map<ItemType, ItemUnlockState> progress = new LinkedHashMap<>();
    progress.put(ItemType.ENERGY_CRYSTAL, ItemUnlockState.DISCOVERED);
    progress.put(ItemType.IRON_AEGIS, ItemUnlockState.LOCKED);

    store.save(progress);

    String serialised = Files.readString(file);
    assertTrue(serialised.contains(ItemType.ENERGY_CRYSTAL.name()));
    assertFalse(serialised.contains(ItemType.IRON_AEGIS.name()));
  }

  @Test
  void ignoresInvalidEntriesButKeepsValidEntries() throws IOException {
    Path file = directory.resolve("item_discovery.json");
    Files.writeString(
        file,
        """
        {
          "progress": [
            {"itemId": "RETIRED_ITEM", "unlockState": "DISCOVERED"},
            {"itemId": "IRON_AEGIS", "unlockState": "FUTURE_STATE"},
            {"itemId": "WARRIORS_CREST", "unlockState": "DISCOVERED"}
          ]
        }
        """);

    Map<ItemType, ItemUnlockState> loaded =
        assertDoesNotThrow(storeAt("item_discovery.json")::load);

    assertEquals(Map.of(ItemType.WARRIORS_CREST, ItemUnlockState.DISCOVERED), loaded);
  }

  private ItemDiscoveryStore storeAt(String fileName) {
    return new ItemDiscoveryStore(Gdx.files.absolute(directory.resolve(fileName).toString()));
  }
}
