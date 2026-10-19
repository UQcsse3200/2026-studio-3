package com.csse3200.game.rewards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Persists permanent Item Library discovery outside individual save-game slots. */
public class ItemDiscoveryStore {
  private static final Logger logger = LoggerFactory.getLogger(ItemDiscoveryStore.class);
  private static final String DEFAULT_PATH = "DECO2800Game/item_discovery.json";

  private final FileHandle file;

  public ItemDiscoveryStore(FileHandle file) {
    if (file == null) {
      throw new IllegalArgumentException("file must not be null");
    }
    this.file = file;
  }

  /** Returns the default store in the same external root directory as user settings. */
  public static ItemDiscoveryStore defaultStore() {
    return new ItemDiscoveryStore(Gdx.files.external(DEFAULT_PATH));
  }

  /** Loads valid persisted progress, returning an empty map when no data can be read. */
  public Map<ItemType, ItemUnlockState> load() {
    if (!file.exists()) {
      return Map.of();
    }

    try {
      StoreData data = json().fromJson(StoreData.class, file);
      if (data == null || data.progress == null) {
        logger.warn("Item discovery file has an invalid structure: {}", file);
        return Map.of();
      }

      Map<ItemType, ItemUnlockState> loaded = new EnumMap<>(ItemType.class);
      for (ProgressEntry entry : data.progress) {
        loadEntry(entry, loaded);
      }
      return loaded;
    } catch (RuntimeException exception) {
      logger.warn("Unable to load item discovery from {}", file, exception);
      return Map.of();
    }
  }

  /** Saves only discovered entries, logging rather than throwing when persistence fails. */
  public void save(Map<ItemType, ItemUnlockState> progress) {
    try {
      StoreData data = new StoreData();
      for (Map.Entry<ItemType, ItemUnlockState> entry : progress.entrySet()) {
        if (entry.getValue() != ItemUnlockState.LOCKED) {
          data.progress.add(new ProgressEntry(entry.getKey().name(), entry.getValue().name()));
        }
      }
      file.parent().mkdirs();
      file.writeString(json().prettyPrint(data), false, "UTF-8");
    } catch (RuntimeException exception) {
      logger.warn("Unable to save item discovery to {}", file, exception);
    }
  }

  private static void loadEntry(ProgressEntry entry, Map<ItemType, ItemUnlockState> loaded) {
    if (entry == null || entry.itemId == null || entry.itemId.isBlank()) {
      return;
    }
    try {
      loaded.put(ItemType.valueOf(entry.itemId), ItemUnlockState.valueOf(entry.unlockState));
    } catch (IllegalArgumentException | NullPointerException exception) {
      logger.warn(
          "Ignoring invalid item discovery entry '{}' with state '{}'",
          entry.itemId,
          entry.unlockState);
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
    public String itemId = "";
    public String unlockState = "";

    public ProgressEntry() {}

    public ProgressEntry(String itemId, String unlockState) {
      this.itemId = itemId;
      this.unlockState = unlockState;
    }
  }
}
