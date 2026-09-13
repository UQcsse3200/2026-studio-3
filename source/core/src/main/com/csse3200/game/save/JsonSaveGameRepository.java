package com.csse3200.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JSON implementation of {@link SaveGameRepository} backed by libGDX external storage. */
public class JsonSaveGameRepository implements SaveGameRepository {
  public static final String DEFAULT_SAVE_DIRECTORY = "DECO2800Game/saves";

  private static final Logger logger = LoggerFactory.getLogger(JsonSaveGameRepository.class);
  private static final Pattern SLOT_FILE_PATTERN = Pattern.compile("slot-(\\d+)\\.json");

  private final FileHandle saveDirectory;
  private final Json json;

  /** Creates a repository in the game's external save directory. */
  public JsonSaveGameRepository() {
    this(Gdx.files.external(DEFAULT_SAVE_DIRECTORY));
  }

  /** Creates a repository at a supplied directory, primarily for tests and platform adapters. */
  public JsonSaveGameRepository(FileHandle saveDirectory) {
    if (saveDirectory == null) {
      throw new IllegalArgumentException("saveDirectory must not be null");
    }
    this.saveDirectory = saveDirectory;
    this.json = new Json();
    this.json.setOutputType(JsonWriter.OutputType.json);
    this.json.setUsePrototypes(false);
    this.json.setIgnoreUnknownFields(true);
  }

  @Override
  public SaveResult save(int slotId, SaveGameData data) {
    if (!isValidSlot(slotId)) {
      return SaveResult.failure(SaveError.INVALID_SLOT, invalidSlotMessage(slotId));
    }
    if (data == null) {
      return SaveResult.failure(SaveError.NO_SAVE_DATA, "Save data must not be null");
    }

    try {
      Path directory = saveDirectory.file().toPath();
      Files.createDirectories(directory);

      String serialised = json.prettyPrint(data);
      LoadResult validation = parse(serialised);
      if (!validation.success()) {
        return SaveResult.failure(validation.error(), validation.message());
      }

      Path target = slotPath(slotId);
      Path temporary = directory.resolve(target.getFileName() + ".tmp");
      Files.writeString(temporary, serialised, StandardCharsets.UTF_8);
      replaceFile(temporary, target);
      return SaveResult.success(data.metadata.copy());
    } catch (IOException | RuntimeException exception) {
      logger.error("Unable to save slot {}", slotId, exception);
      deleteTemporaryFile(slotId);
      return SaveResult.failure(SaveError.IO_ERROR, "Unable to write save slot " + slotId);
    }
  }

  @Override
  public LoadResult load(int slotId) {
    if (!isValidSlot(slotId)) {
      return LoadResult.failure(SaveError.INVALID_SLOT, invalidSlotMessage(slotId));
    }

    Path path = slotPath(slotId);
    if (!Files.exists(path)) {
      return LoadResult.failure(SaveError.SLOT_NOT_FOUND, "Save slot " + slotId + " was not found");
    }

    try {
      return parse(Files.readString(path, StandardCharsets.UTF_8));
    } catch (IOException exception) {
      logger.error("Unable to load slot {}", slotId, exception);
      return LoadResult.failure(SaveError.IO_ERROR, "Unable to read save slot " + slotId);
    }
  }

  @Override
  public SaveSlotListResult listSaveSlots() {
    Path directory = saveDirectory.file().toPath();
    if (!Files.exists(directory)) {
      return SaveSlotListResult.success(List.of());
    }

    List<SaveSlotMetadata> slots = new ArrayList<>();
    try (Stream<Path> files = Files.list(directory)) {
      files
          .filter(Files::isRegularFile)
          .map(path -> SLOT_FILE_PATTERN.matcher(path.getFileName().toString()))
          .filter(Matcher::matches)
          .map(matcher -> Integer.parseInt(matcher.group(1)))
          .sorted()
          .map(this::metadataForSlot)
          .forEach(slots::add);
      slots.sort(Comparator.comparingInt(metadata -> metadata.slotId));
      return SaveSlotListResult.success(slots);
    } catch (IOException | RuntimeException exception) {
      logger.error("Unable to list save slots", exception);
      return SaveSlotListResult.failure(SaveError.IO_ERROR, "Unable to list save slots");
    }
  }

  @Override
  public DeleteSaveResult delete(int slotId) {
    if (!isValidSlot(slotId)) {
      return DeleteSaveResult.failure(SaveError.INVALID_SLOT, invalidSlotMessage(slotId));
    }

    try {
      if (!Files.deleteIfExists(slotPath(slotId))) {
        return DeleteSaveResult.failure(
            SaveError.SLOT_NOT_FOUND, "Save slot " + slotId + " was not found");
      }
      deleteTemporaryFile(slotId);
      return DeleteSaveResult.succeeded();
    } catch (IOException exception) {
      logger.error("Unable to delete slot {}", slotId, exception);
      return DeleteSaveResult.failure(SaveError.IO_ERROR, "Unable to delete save slot " + slotId);
    }
  }

  private LoadResult parse(String serialised) {
    if (serialised == null || serialised.isBlank()) {
      return LoadResult.failure(SaveError.MALFORMED_SAVE, "Save file is empty");
    }

    try {
      JsonValue root = new JsonReader().parse(serialised);
      JsonValue versionValue = root.get("schemaVersion");
      if (versionValue == null || !versionValue.isNumber()) {
        return LoadResult.failure(
            SaveError.MALFORMED_SAVE, "Save file is missing a numeric schemaVersion");
      }

      int version = versionValue.asInt();
      if (version != SaveGameData.CURRENT_SCHEMA_VERSION) {
        return LoadResult.failure(
            SaveError.UNSUPPORTED_VERSION,
            "Unsupported save version "
                + version
                + "; expected "
                + SaveGameData.CURRENT_SCHEMA_VERSION);
      }

      SaveGameData data = json.fromJson(SaveGameData.class, serialised);
      if (data == null || data.metadata == null) {
        return LoadResult.failure(
            SaveError.MALFORMED_SAVE, "Save file is missing required metadata");
      }
      return LoadResult.success(data);
    } catch (RuntimeException exception) {
      logger.warn("Unable to parse save data", exception);
      return LoadResult.failure(SaveError.MALFORMED_SAVE, "Save file contains invalid JSON");
    }
  }

  private SaveSlotMetadata metadataForSlot(int slotId) {
    LoadResult result = load(slotId);
    if (!result.success()) {
      return SaveSlotMetadata.unreadable(slotId, result.message());
    }

    SaveSlotMetadata metadata = result.data().metadata.copy();
    if (metadata.slotId != slotId) {
      return SaveSlotMetadata.unreadable(slotId, "Save metadata does not match its slot file");
    }
    return metadata;
  }

  private Path slotPath(int slotId) {
    return saveDirectory.file().toPath().resolve("slot-" + slotId + ".json");
  }

  private void deleteTemporaryFile(int slotId) {
    try {
      Files.deleteIfExists(slotPath(slotId).resolveSibling("slot-" + slotId + ".json.tmp"));
    } catch (IOException exception) {
      logger.warn("Unable to remove temporary file for slot {}", slotId, exception);
    }
  }

  private static void replaceFile(Path temporary, Path target) throws IOException {
    try {
      Files.move(
          temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static boolean isValidSlot(int slotId) {
    return slotId > 0;
  }

  private static String invalidSlotMessage(int slotId) {
    return "Save slot ID must be positive: " + slotId;
  }
}
