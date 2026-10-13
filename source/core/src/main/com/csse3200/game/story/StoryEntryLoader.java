package com.csse3200.game.story;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the Library Story entries from JSON.
 *
 * <p>Expected shape:
 *
 * <pre>{@code
 * { "entries": [ { "id": "the_pantheon", "title": "The Pantheon", "paragraphs": ["..."] } ] }
 * }</pre>
 *
 * <p>Loading never throws for content problems, so a bad edit cannot crash the Library. A missing
 * or malformed file returns an empty list; an invalid or duplicate entry is skipped with a warning
 * and the remaining entries are kept in file order.
 */
public final class StoryEntryLoader {
  public static final String DEFAULT_FILE = "configs/storyEntries.json";
  private static final Logger logger = LoggerFactory.getLogger(StoryEntryLoader.class);

  private StoryEntryLoader() {}

  /** Loads entries from {@link #DEFAULT_FILE}. */
  public static List<StoryEntry> loadDefault() {
    return load(DEFAULT_FILE);
  }

  /**
   * Loads entries from an internal asset file.
   *
   * @param filename path relative to the assets root
   * @return immutable list of valid entries in file order; empty if the file cannot be used
   */
  public static List<StoryEntry> load(String filename) {
    JsonValue entries = readEntriesArray(filename);
    if (entries == null) {
      return List.of();
    }

    List<StoryEntry> loaded = new ArrayList<>();
    Set<String> seenIds = new HashSet<>();
    int index = 0;
    for (JsonValue entry = entries.child; entry != null; entry = entry.next, index++) {
      StoryEntry parsed = parseEntry(entry, filename, index);
      if (parsed == null) {
        continue;
      }
      if (!seenIds.add(parsed.id())) {
        logger.warn("Skipping duplicate story entry '{}' in {}", parsed.id(), filename);
        continue;
      }
      loaded.add(parsed);
    }
    return List.copyOf(loaded);
  }

  private static JsonValue readEntriesArray(String filename) {
    if (filename == null || filename.isBlank()) {
      logger.error("Story entry filename must not be null or blank");
      return null;
    }
    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      logger.error("Story entry file does not exist: {}", filename);
      return null;
    }

    JsonValue root;
    try {
      root = new JsonReader().parse(file);
    } catch (RuntimeException exception) {
      logger.error("Malformed story entry file: {}", filename, exception);
      return null;
    }
    if (root == null || !root.isObject()) {
      logger.error("Story entry root must be a JSON object: {}", filename);
      return null;
    }
    JsonValue entries = root.get("entries");
    if (entries == null || !entries.isArray()) {
      logger.error("Story entry file must contain an 'entries' array: {}", filename);
      return null;
    }
    return entries;
  }

  private static StoryEntry parseEntry(JsonValue entry, String filename, int index) {
    if (entry == null || !entry.isObject()) {
      logger.warn("Skipping story entry {} in {}: not a JSON object", index, filename);
      return null;
    }
    String id = stringField(entry, "id");
    String title = stringField(entry, "title");
    JsonValue paragraphsValue = entry.get("paragraphs");
    if (paragraphsValue == null || !paragraphsValue.isArray()) {
      logger.warn("Skipping story entry {} in {}: 'paragraphs' must be an array", index, filename);
      return null;
    }

    List<String> paragraphs = new ArrayList<>();
    for (JsonValue paragraph = paragraphsValue.child;
        paragraph != null;
        paragraph = paragraph.next) {
      if (!paragraph.isString()) {
        logger.warn("Skipping story entry {} in {}: paragraphs must be strings", index, filename);
        return null;
      }
      paragraphs.add(paragraph.asString());
    }

    try {
      return new StoryEntry(id, title, paragraphs);
    } catch (IllegalArgumentException exception) {
      logger.warn("Skipping story entry {} in {}: {}", index, filename, exception.getMessage());
      return null;
    }
  }

  private static String stringField(JsonValue entry, String name) {
    JsonValue value = entry.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }
}
