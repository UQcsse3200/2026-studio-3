package com.csse3200.game.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.cards.configs.CardConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads and validates card definitions from a JSON configuration file. */
public final class CardConfigLoader {
  public static final String DEFAULT_CARD_FILE = "configs/cards.json";

  private static final Logger logger = LoggerFactory.getLogger(CardConfigLoader.class);

  private static final List<String> REQUIRED_FIELDS =
      List.of(
          "id",
          "name",
          "description",
          "cost",
          "type",
          "rarity",
          "target",
          "effects",
          "texturePath");

  private CardConfigLoader() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Loads cards from the default card configuration file.
   *
   * @return an immutable list of validated card definitions
   * @throws CardLoadingException if the file cannot be loaded or contains invalid cards
   */
  public static List<CardConfig> loadCards() {
    return loadCards(DEFAULT_CARD_FILE);
  }

  /**
   * Loads and validates cards from a JSON configuration file.
   *
   * @param filename internal asset path of the configuration file
   * @return an immutable list of validated card definitions
   * @throws CardLoadingException if the path, file structure or card data is invalid
   */
  public static List<CardConfig> loadCards(String filename) {
    JsonValue cardArray = loadCardArray(filename);

    List<CardConfig> cards = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    Set<String> cardIds = new HashSet<>();
    Json json = new Json();

    int index = 0;
    for (JsonValue cardData = cardArray.child;
        cardData != null;
        cardData = cardData.next, index++) {
      String position = "card[" + index + "]";

      if (!cardData.isObject()) {
        errors.add(position + " must be a JSON object");
        continue;
      }

      checkRequiredFields(cardData, position, errors);

      CardConfig card;
      try {
        card = json.readValue(CardConfig.class, cardData);
      } catch (Exception exception) {
        errors.add(position + " could not be parsed: " + getErrorMessage(exception));
        continue;
      }

      cards.add(card);
      String label = getCardLabel(card, index);

      for (String error : CardValidator.validate(card)) {
        errors.add(label + ": " + error);
      }

      if (card.id != null && !card.id.isBlank() && !cardIds.add(card.id)) {
        errors.add(label + ": duplicate card ID '" + card.id + "'");
      }
    }

    if (!errors.isEmpty()) {
      throw new CardLoadingException(formatErrors(filename, errors));
    }

    logger.info("Loaded {} card definitions from {}", cards.size(), filename);
    return List.copyOf(cards);
  }

  private static void checkRequiredFields(
      JsonValue cardData, String position, List<String> errors) {
    for (String field : REQUIRED_FIELDS) {
      JsonValue value = cardData.get(field);
      if (value == null || value.isNull()) {
        errors.add(position + ": missing required field '" + field + "'");
      }
    }
  }

  private static String getCardLabel(CardConfig card, int index) {
    if (card == null || card.id == null || card.id.isBlank()) {
      return "card[" + index + "]";
    }
    return "card[" + index + "] (" + card.id + ")";
  }

  private static String getErrorMessage(Exception exception) {
    String message = exception.getMessage();
    return message == null ? exception.getClass().getSimpleName() : message;
  }

  private static String formatErrors(String filename, List<String> errors) {
    String separator = System.lineSeparator() + "- ";
    return "Invalid card definitions in "
        + filename
        + ":"
        + System.lineSeparator()
        + "- "
        + String.join(separator, errors);
  }

  private static JsonValue loadCardArray(String filename) throws CardLoadingException {
    if (filename == null || filename.isBlank()) {
      throw new CardLoadingException("Card configuration filename must not be null or blank");
    }

    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      throw new CardLoadingException("Card configuration file does not exist: " + filename);
    }

    JsonValue root;
    try {
      root = new JsonReader().parse(file);
    } catch (Exception exception) {
      throw new CardLoadingException("Malformed card configuration file: " + filename, exception);
    }

    if (root == null || !root.isObject()) {
      throw new CardLoadingException("Card configuration root must be a JSON object: " + filename);
    }

    JsonValue cardArray = root.get("cards");
    if (cardArray == null || !cardArray.isArray()) {
      throw new CardLoadingException(
          "Card configuration must contain a 'cards' array: " + filename);
    }

    if (cardArray.size == 0) {
      throw new CardLoadingException(
          "Card configuration must contain at least one card: " + filename);
    }

    return cardArray;
  }
}
