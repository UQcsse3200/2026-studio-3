package com.csse3200.game.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;

/** Loads the shared card-acquisition allow-list from an internal JSON asset. */
public final class CardAcquisitionPoolLoader {
  public static final String DEFAULT_POOL_FILE = "configs/cardAcquisitionPool.json";

  private CardAcquisitionPoolLoader() {
    throw new IllegalStateException("Utility class");
  }

  /** Loads the default acquisition allow-list against the supplied authoritative catalogue. */
  public static CardAcquisitionPool loadDefault(CardService cardService) {
    return load(DEFAULT_POOL_FILE, cardService);
  }

  /** Loads and validates one acquisition allow-list. */
  public static CardAcquisitionPool load(String filename, CardService cardService) {
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Acquisition pool filename must not be null or blank");
    }
    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      throw new IllegalArgumentException("Acquisition pool file does not exist: " + filename);
    }

    JsonValue root;
    try {
      root = new JsonReader().parse(file);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Malformed acquisition pool file: " + filename, exception);
    }
    if (root == null || !root.isObject()) {
      throw new IllegalArgumentException(
          "Acquisition pool root must be a JSON object: " + filename);
    }
    JsonValue ids = root.get("eligibleCardIds");
    if (ids == null || !ids.isArray()) {
      throw new IllegalArgumentException(
          "Acquisition pool must contain an 'eligibleCardIds' array: " + filename);
    }

    List<String> cardIds = new ArrayList<>();
    for (JsonValue id = ids.child; id != null; id = id.next) {
      if (!id.isString()) {
        throw new IllegalArgumentException(
            "eligibleCardIds must contain only strings: " + filename);
      }
      cardIds.add(id.asString());
    }
    return new CardAcquisitionPool(cardService, cardIds);
  }
}
