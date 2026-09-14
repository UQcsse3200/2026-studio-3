package com.csse3200.game.chance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.chance.configs.ChanceChoiceConfig;
import com.csse3200.game.chance.configs.ChanceConfig;
import com.csse3200.game.chance.configs.ChanceEncounterConfig;
import com.csse3200.game.encounters.integration.CardCatalogGateway;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and validates Chance Encounter definitions from a JSON configuration file.
 *
 * <p>Catalog-less entry points validate JSON structure and reward-ID syntax. Use a catalog-aware
 * entry point when reward IDs must also be checked against registered card definitions.
 */
public final class ChanceEncounterConfigLoader {
  public static final String DEFAULT_ENCOUNTER_FILE = "configs/chanceEncounters.json";

  private static final Logger logger = LoggerFactory.getLogger(ChanceEncounterConfigLoader.class);

  private ChanceEncounterConfigLoader() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Loads Chance Encounters from the default configuration file.
   *
   * <p>This catalog-less entry point validates reward-ID syntax but does not verify that a reward
   * card is registered.
   *
   * @return immutable structurally validated encounter definitions in configuration order
   * @throws ChanceEncounterLoadingException if the configuration cannot be loaded
   */
  public static List<ChanceEncounter> loadEncounters() {
    return loadEncounters(DEFAULT_ENCOUNTER_FILE, null);
  }

  /**
   * Loads the default Chance configuration and validates rewards against the production card
   * catalog boundary.
   *
   * @param cardCatalog authoritative card lookup boundary
   * @return immutable structurally and semantically validated encounter definitions in
   *     configuration order
   */
  public static List<ChanceEncounter> loadEncountersWithCatalog(CardCatalogGateway cardCatalog) {
    if (cardCatalog == null) {
      throw new IllegalArgumentException("cardCatalog cannot be null");
    }
    return loadEncounters(DEFAULT_ENCOUNTER_FILE, cardCatalog);
  }

  /**
   * Loads Chance Encounters from a JSON configuration file.
   *
   * <p>This catalog-less entry point validates reward-ID syntax but does not verify that a reward
   * card is registered.
   *
   * @param filename internal asset path of the configuration file
   * @return immutable structurally validated encounter definitions in configuration order
   * @throws ChanceEncounterLoadingException if the path, JSON, or encounter data is invalid
   */
  public static List<ChanceEncounter> loadEncounters(String filename) {
    return loadEncounters(filename, null);
  }

  /**
   * Loads a Chance configuration and validates card rewards against the supplied catalog.
   *
   * @param filename internal asset path of the configuration file
   * @param cardCatalog authoritative card lookup boundary, or null for structural validation only
   * @return immutable structurally validated definitions, including semantic card validation when a
   *     catalog is supplied
   */
  public static List<ChanceEncounter> loadEncounters(
      String filename, CardCatalogGateway cardCatalog) {
    if (filename == null || filename.isBlank()) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration filename must not be null or blank");
    }

    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration file does not exist: " + filename);
    }

    JsonValue root;
    try {
      root = new JsonReader().parse(file);
    } catch (Exception exception) {
      throw new ChanceEncounterLoadingException(
          "Malformed Chance Encounter configuration file: " + filename, exception);
    }

    if (root == null || !root.isObject()) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration root must be a JSON object: " + filename);
    }

    JsonValue encounterArray = root.get("encounters");
    if (encounterArray == null || !encounterArray.isArray()) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration must contain an 'encounters' array: " + filename);
    }

    List<String> errors = new ArrayList<>();
    validateNumericFields(encounterArray, errors);
    if (!errors.isEmpty()) {
      throw invalidDefinitions(filename, errors);
    }

    ChanceConfig config;
    try {
      config = new Json().readValue(ChanceConfig.class, root);
    } catch (Exception exception) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration could not be parsed: " + filename, exception);
    }

    errors.addAll(ChanceEncounterConfigValidator.validate(config, cardCatalog));
    if (!errors.isEmpty()) {
      throw invalidDefinitions(filename, errors);
    }

    List<ChanceEncounter> encounters = new ArrayList<>();
    for (int i = 0; i < config.encounters.length; i++) {
      encounters.add(toDomain(config.encounters[i]));
    }

    logger.info("Loaded {} Chance Encounter definitions from {}", encounters.size(), filename);
    return List.copyOf(encounters);
  }

  private static ChanceEncounter toDomain(ChanceEncounterConfig encounterConfig) {
    List<ChanceChoice> choices = new ArrayList<>();
    for (ChanceChoiceConfig choiceConfig : encounterConfig.choices) {
      ChanceOutcome outcome =
          new ChanceOutcome(
              choiceConfig.outcome.healthDelta,
              choiceConfig.outcome.currencyDelta,
              choiceConfig.outcome.cardRewardId);
      choices.add(new ChanceChoice(choiceConfig.id, choiceConfig.description, outcome));
    }

    return new ChanceEncounter(
        encounterConfig.id, encounterConfig.description, choices, encounterConfig.weight);
  }

  private static void validateNumericFields(JsonValue encounterArray, List<String> errors) {
    int encounterIndex = 0;
    for (JsonValue encounter = encounterArray.child;
        encounter != null;
        encounter = encounter.next, encounterIndex++) {
      if (!encounter.isObject()) {
        continue;
      }

      validateInteger(encounter.get("weight"), "encounter[" + encounterIndex + "].weight", errors);

      JsonValue choices = encounter.get("choices");
      if (choices == null || !choices.isArray()) {
        continue;
      }

      int choiceIndex = 0;
      for (JsonValue choice = choices.child; choice != null; choice = choice.next, choiceIndex++) {
        if (!choice.isObject()) {
          continue;
        }

        String choiceLabel = "encounter[" + encounterIndex + "].choice[" + choiceIndex + "]";
        JsonValue outcome = choice.get("outcome");
        if (outcome == null || outcome.isNull()) {
          continue;
        }
        if (!outcome.isObject()) {
          errors.add(choiceLabel + ": outcome must be a JSON object");
          continue;
        }

        String label = choiceLabel + ".outcome";
        validateInteger(outcome.get("healthDelta"), label + ".healthDelta", errors);
        validateInteger(outcome.get("currencyDelta"), label + ".currencyDelta", errors);
        validateOptionalString(outcome.get("cardRewardId"), label + ".cardRewardId", errors);
      }
    }
  }

  private static void validateOptionalString(JsonValue value, String label, List<String> errors) {
    if (value == null) {
      return;
    }
    if (value.isNull()) {
      errors.add(label + " must not be null when present");
    } else if (!value.isString()) {
      errors.add(label + " must be a string when present");
    } else if (value.asString().isBlank()) {
      errors.add(label + " must not be blank when present");
    }
  }

  private static void validateInteger(JsonValue value, String label, List<String> errors) {
    if (value == null || value.isNull()) {
      return;
    }
    if (!value.isNumber()) {
      errors.add(label + " must be an integer");
      return;
    }

    try {
      new BigDecimal(value.asString()).intValueExact();
    } catch (ArithmeticException | NumberFormatException exception) {
      errors.add(label + " must be an exact 32-bit integer, was " + value.asString());
    }
  }

  private static ChanceEncounterLoadingException invalidDefinitions(
      String filename, List<String> errors) {
    String separator = System.lineSeparator() + "- ";
    return new ChanceEncounterLoadingException(
        "Invalid Chance Encounter definitions in "
            + filename
            + ":"
            + System.lineSeparator()
            + "- "
            + String.join(separator, errors));
  }
}
