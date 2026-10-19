package com.csse3200.game.rewards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.HashSet;
import java.util.Set;

/** Loads reviewed reward weights once, independently of the acquisition allow-list. */
public final class CardRewardPolicyLoader {
  public static final String DEFAULT_FILE = "configs/cardRewardPolicy.json";
  private static final String NORMAL_PROFILE = "normal";
  private static final String ELITE_PROFILE = "elite";

  private CardRewardPolicyLoader() {}

  public static CardRewardPolicy loadDefault() {
    return load(DEFAULT_FILE);
  }

  public static CardRewardPolicy load(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Reward policy filename is required");
    }
    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      throw new IllegalArgumentException("Reward policy file does not exist: " + filename);
    }
    try {
      return parse(file.readString());
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "Invalid reward policy " + filename + ": " + exception.getMessage(), exception);
    }
  }

  /** Parses the same strict schema used by internal assets; useful for tooling and tests. */
  public static CardRewardPolicy parse(String json) {
    JsonValue root;
    try {
      root = new JsonReader().parse(json);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Malformed reward policy JSON", exception);
    }
    requireFields(root, Set.of(NORMAL_PROFILE, ELITE_PROFILE), "reward policy");
    return new CardRewardPolicy(
        readProfile(root.get(NORMAL_PROFILE), NORMAL_PROFILE),
        readProfile(root.get(ELITE_PROFILE), ELITE_PROFILE));
  }

  private static CardRewardPolicy.RarityWeights readProfile(JsonValue node, String name) {
    requireFields(node, Set.of("COMMON", "UNCOMMON", "RARE"), name);
    try {
      return new CardRewardPolicy.RarityWeights(
          number(node, "COMMON", name), number(node, "UNCOMMON", name), number(node, "RARE", name));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(name + " profile: " + exception.getMessage(), exception);
    }
  }

  private static double number(JsonValue node, String field, String profile) {
    JsonValue value = node.get(field);
    if (!value.isNumber()) {
      throw new IllegalArgumentException(profile + "." + field + " must be a number");
    }
    return value.asDouble();
  }

  private static void requireFields(JsonValue node, Set<String> expected, String location) {
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException(location + " must be an object");
    }
    Set<String> seen = new HashSet<>();
    for (JsonValue child = node.child; child != null; child = child.next) {
      if (!expected.contains(child.name) || !seen.add(child.name)) {
        throw new IllegalArgumentException(
            location + " has unknown or duplicate field: " + child.name);
      }
    }
    if (!seen.equals(expected)) {
      Set<String> missing = new HashSet<>(expected);
      missing.removeAll(seen);
      throw new IllegalArgumentException(location + " is missing required fields: " + missing);
    }
  }
}
