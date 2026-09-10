package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonSaveGameRepositoryTest {
  @TempDir Path temporaryDirectory;

  private JsonSaveGameRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile()));
  }

  @Test
  void shouldRoundTripCompleteSaveData() {
    SaveGameData data = sampleData(2, "Second run");

    SaveResult saveResult = repository.save(2, data);
    LoadResult loadResult = repository.load(2);

    assertTrue(saveResult.success());
    assertTrue(loadResult.success());
    assertEquals(SaveError.NONE, loadResult.error());
    assertEquals(SaveGameData.CURRENT_SCHEMA_VERSION, loadResult.data().schemaVersion);
    assertEquals(2, loadResult.data().metadata.slotId);
    assertEquals("Second run", loadResult.data().metadata.runLabel);
    assertEquals(43, loadResult.data().player.currentHealth);
    assertEquals(List.of("strike", "defend", "strike"), loadResult.data().deck.cardIds);
    assertEquals(7, loadResult.data().map.currentNodeId);
    assertEquals(List.of(8, 9), loadResult.data().map.nodes.get(0).connectionIds);
    assertEquals(List.of("shop-1"), loadResult.data().progress.completedEncounterIds);
    assertFalse(Files.exists(temporaryDirectory.resolve("slot-2.json.tmp")));
  }

  @Test
  void shouldOverwriteExistingSlot() {
    assertTrue(repository.save(1, sampleData(1, "Old run")).success());
    assertTrue(repository.save(1, sampleData(1, "Updated run")).success());

    LoadResult result = repository.load(1);

    assertTrue(result.success());
    assertEquals("Updated run", result.data().metadata.runLabel);
  }

  @Test
  void shouldReportMissingSlot() {
    LoadResult result = repository.load(4);

    assertFalse(result.success());
    assertEquals(SaveError.SLOT_NOT_FOUND, result.error());
  }

  @Test
  void shouldRejectInvalidSlot() {
    assertEquals(SaveError.INVALID_SLOT, repository.load(0).error());
    assertEquals(SaveError.INVALID_SLOT, repository.save(-1, sampleData(1, "Run")).error());
    assertEquals(SaveError.INVALID_SLOT, repository.delete(0).error());
  }

  @Test
  void shouldRejectNullSaveData() {
    SaveResult result = repository.save(1, null);

    assertFalse(result.success());
    assertEquals(SaveError.NO_SAVE_DATA, result.error());
  }

  @Test
  void shouldReportMalformedJson() throws IOException {
    Files.writeString(temporaryDirectory.resolve("slot-1.json"), "{this is not valid json");

    LoadResult result = repository.load(1);

    assertFalse(result.success());
    assertEquals(SaveError.MALFORMED_SAVE, result.error());
  }

  @Test
  void shouldRejectSaveWithoutSchemaVersion() throws IOException {
    Files.writeString(temporaryDirectory.resolve("slot-1.json"), "{\"metadata\":{\"slotId\":1}}");

    LoadResult result = repository.load(1);

    assertFalse(result.success());
    assertEquals(SaveError.MALFORMED_SAVE, result.error());
  }

  @Test
  void shouldRejectUnsupportedSchemaVersion() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("slot-1.json"),
        "{\"schemaVersion\":999,\"metadata\":{\"slotId\":1}}");

    LoadResult result = repository.load(1);

    assertFalse(result.success());
    assertEquals(SaveError.UNSUPPORTED_VERSION, result.error());
  }

  @Test
  void shouldListValidAndUnreadableSlotsInOrder() throws IOException {
    assertTrue(repository.save(3, sampleData(3, "Third run")).success());
    assertTrue(repository.save(1, sampleData(1, "First run")).success());
    Files.writeString(temporaryDirectory.resolve("slot-2.json"), "bad-json");
    Files.writeString(temporaryDirectory.resolve("notes.json"), "ignored");

    SaveSlotListResult result = repository.listSaveSlots();

    assertTrue(result.success());
    assertEquals(List.of(1, 2, 3), result.slots().stream().map(slot -> slot.slotId).toList());
    assertTrue(result.slots().get(0).loadable);
    assertFalse(result.slots().get(1).loadable);
    assertFalse(result.slots().get(1).statusMessage.isBlank());
    assertTrue(result.slots().get(2).loadable);
  }

  @Test
  void shouldReturnEmptyListWhenDirectoryDoesNotExist() {
    Path missingDirectory = temporaryDirectory.resolve("missing");
    JsonSaveGameRepository missingRepository =
        new JsonSaveGameRepository(new FileHandle(missingDirectory.toFile()));

    SaveSlotListResult result = missingRepository.listSaveSlots();

    assertTrue(result.success());
    assertTrue(result.slots().isEmpty());
  }

  @Test
  void shouldDeleteExistingSlot() {
    assertTrue(repository.save(1, sampleData(1, "Run")).success());

    DeleteSaveResult firstDelete = repository.delete(1);
    DeleteSaveResult secondDelete = repository.delete(1);

    assertTrue(firstDelete.success());
    assertFalse(secondDelete.success());
    assertEquals(SaveError.SLOT_NOT_FOUND, secondDelete.error());
  }

  private static SaveGameData sampleData(int slotId, String label) {
    SaveGameData data =
        new SaveGameData(
            new PlayerSaveData(43, 60, 120, 8),
            new DeckSaveData(List.of("strike", "defend", "strike")),
            new MapSaveData(
                List.of(
                    new MapNodeSaveData(7, "COMBAT", "CURRENT", List.of(8, 9)),
                    new MapNodeSaveData(8, "SHOP", "AVAILABLE", List.of(7))),
                7,
                null),
            new ProgressSaveData(List.of("shop-1"), "reward-2", "MAP"));
    data.metadata = new SaveSlotMetadata(slotId, 123456L, label, 321L, "MAP");
    assertNotNull(data.metadata);
    return data;
  }
}
