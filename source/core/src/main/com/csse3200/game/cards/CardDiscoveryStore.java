package com.csse3200.game.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists global card discovery outside save-game slots.
 *
 * <p>The JSON shape is {@code {"progress":[{"cardId":"strike","unlockState":"SEEN"}]}}.
 */
public class CardDiscoveryStore {
  private static final Logger logger = LoggerFactory.getLogger(CardDiscoveryStore.class);
  private static final String DEFAULT_PATH = "DECO2800Game/card_discovery.json";

  private final FileHandle file;

  public CardDiscoveryStore(FileHandle file) {
    if (file == null) {
      throw new IllegalArgumentException("file must not be null");
    }
    this.file = file;
  }

  /** Returns the default store in the same external root directory as user settings. */
  public static CardDiscoveryStore defaultStore() {
    return new CardDiscoveryStore(Gdx.files.external(DEFAULT_PATH));
  }

  /** Loads persisted progress, returning an empty map when no valid data is available. */
  public Map<String, CardUnlockState> load() {
    if (!file.exists()) {
      return Map.of();
    }

    try {
      StoreData data = json().fromJson(StoreData.class, file);
      if (data == null || data.progress == null) {
        logger.warn("Card discovery file has an invalid structure: {}", file);
        return Map.of();
      }

      Map<String, CardUnlockState> loaded = new LinkedHashMap<>();
      for (ProgressEntry entry : data.progress) {
        if (entry == null || entry.cardId == null || entry.cardId.isBlank()) {
          logger.warn("Card discovery file has an invalid entry: {}", file);
          continue;
        }
        loadEntry(entry, loaded);
      }
      return loaded;
    } catch (RuntimeException exception) {
      logger.warn("Unable to load card discovery from {}", file, exception);
      return Map.of();
    }
  }

  private static void loadEntry(ProgressEntry entry, Map<String, CardUnlockState> loaded) {
    try {
      loaded.put(entry.cardId, CardUnlockState.valueOf(entry.unlockState));
    } catch (IllegalArgumentException | NullPointerException exception) {
      logger.warn(
          "Ignoring unknown card discovery state '{}' for '{}'", entry.unlockState, entry.cardId);
    }
  }

  /** Saves all non-locked progress, logging rather than throwing if the write fails. */
  public void save(Map<String, CardUnlockState> progress) {
    try {
      StoreData data = new StoreData();
      for (Map.Entry<String, CardUnlockState> entry : progress.entrySet()) {
        if (entry.getValue() != CardUnlockState.LOCKED) {
          data.progress.add(new ProgressEntry(entry.getKey(), entry.getValue().name()));
        }
      }
      file.parent().mkdirs();
      file.writeString(json().prettyPrint(data), false, "UTF-8");
    } catch (RuntimeException exception) {
      logger.warn("Unable to save card discovery to {}", file, exception);
    }
  }

  private static Json json() {
    Json json = new Json();
    json.setOutputType(JsonWriter.OutputType.json);
    json.setUsePrototypes(false);
    return json;
  }

  public static class StoreData {
    public List<ProgressEntry> progress = new ArrayList<>();
  }

  public static class ProgressEntry {
    public String cardId = "";
    public String unlockState = "";

    public ProgressEntry() {}

    public ProgressEntry(String cardId, String unlockState) {
      this.cardId = cardId;
      this.unlockState = unlockState;
    }
  }
}
