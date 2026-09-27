package com.csse3200.game.narration;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads sequence IDs mapped to passages, each an array of one to three nonblank lines. */
public final class NarrationConfigLoader {
  public static final String DEFAULT_FILE = "configs/narration.json";

  private NarrationConfigLoader() {}

  /** Returns immutable passages; an unknown (including null) ID returns an empty list. */
  public static List<List<String>> loadSequence(String sequenceId) {
    return loadSequence(DEFAULT_FILE, sequenceId);
  }

  /**
   * Loads and validates the entire file before looking up an ID.
   *
   * @throws NarrationLoadingException for missing files, invalid JSON or invalid structure
   */
  public static List<List<String>> loadSequence(String filename, String sequenceId) {
    if (filename == null || filename.isBlank()) {
      throw new NarrationLoadingException(
          "Narration configuration filename must not be null or blank");
    }
    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      throw new NarrationLoadingException(
          "Narration configuration file does not exist: " + filename);
    }
    JsonValue root;
    try {
      root = new JsonReader().parse(file);
    } catch (Exception exception) {
      throw new NarrationLoadingException(
          "Malformed narration configuration file: " + filename, exception);
    }
    if (root == null || !root.isObject()) {
      throw new NarrationLoadingException(
          "Narration configuration root must be a JSON object: " + filename);
    }
    Map<String, List<List<String>>> sequences = new HashMap<>();
    for (JsonValue sequence : root) {
      if (sequence.name.isBlank() || !sequence.isArray() || sequences.containsKey(sequence.name)) {
        throw new NarrationLoadingException(
            "Invalid or duplicate sequence in " + filename + ": " + sequence.name);
      }
      List<List<String>> passages = new ArrayList<>();
      for (JsonValue passage : sequence) {
        if (!passage.isArray() || passage.size < 1 || passage.size > 3) {
          throw new NarrationLoadingException(
              "Passages must contain one to three lines: " + sequence.name);
        }
        List<String> lines = new ArrayList<>();
        for (JsonValue line : passage) {
          if (!line.isString() || line.asString().isBlank()) {
            throw new NarrationLoadingException(
                "Passage lines must be nonblank strings: " + sequence.name);
          }
          lines.add(line.asString());
        }
        passages.add(List.copyOf(lines));
      }
      sequences.put(sequence.name, List.copyOf(passages));
    }
    return sequences.getOrDefault(sequenceId, List.of());
  }
}
