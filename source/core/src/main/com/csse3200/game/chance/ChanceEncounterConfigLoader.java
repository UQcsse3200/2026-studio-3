package com.csse3200.game.chance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.chance.configs.ChanceChoiceConfig;
import com.csse3200.game.chance.configs.ChanceConfig;
import com.csse3200.game.chance.configs.ChanceCurseRiskConfig;
import com.csse3200.game.chance.configs.ChanceEncounterConfig;
import com.csse3200.game.encounters.integration.CardCatalogGateway;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and validates Chance Encounter definitions from a JSON configuration file.
 *
 * <p>Catalog-less entry points validate JSON structure and reward-ID syntax. Use a catalog-aware
 * entry point when reward IDs must also be checked against registered card definitions. Item IDs
 * are checked for enum-name syntax here and against the authoritative item API before settlement.
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
          ChanceOutcome.withRewards(
              choiceConfig.outcome.healthDelta,
              choiceConfig.outcome.currencyDelta,
              choiceConfig.outcome.cardRewardId == null
                  ? List.of()
                  : List.of(choiceConfig.outcome.cardRewardId),
              choiceConfig.outcome.itemRewardIds == null
                  ? List.of()
                  : List.of(choiceConfig.outcome.itemRewardIds));
      ChanceCurseRiskConfig risk = choiceConfig.curseRisk;
      ChanceCurseRisk curseRisk =
          risk == null
              ? null
              : new ChanceCurseRisk(
                  risk.triggerChance,
                  Arrays.stream(risk.items)
                      .map(item -> new ChanceCurseRisk.WeightedItem(item.id, item.weight))
                      .toList());
      choices.add(new ChanceChoice(choiceConfig.id, choiceConfig.description, outcome, curseRisk));
    }

    return new ChanceEncounter(
        encounterConfig.id, encounterConfig.description, choices, encounterConfig.weight);
  }

  private static void validateNumericFields(JsonValue encounterArray, List<String> errors) {
    int encounterIndex = 0;
    for (JsonValue encounter = encounterArray.child;
        encounter != null;
        encounter = encounter.next, encounterIndex++) {
      if (!encounter.isObject()) continue;
      String label = "encounter[" + encounterIndex + "]";
      validateInteger(encounter.get("weight"), label + ".weight", errors);
      validateChoices(encounter.get("choices"), label, errors);
    }
  }

  private static void validateChoices(JsonValue choices, String label, List<String> errors) {
    if (choices == null || !choices.isArray()) return;
    int choiceIndex = 0;
    for (JsonValue choice = choices.child; choice != null; choice = choice.next, choiceIndex++) {
      if (!choice.isObject()) continue;
      String choiceLabel = label + ".choice[" + choiceIndex + "]";
      validateCurseRisk(choice.get("curseRisk"), choiceLabel + ".curseRisk", errors);
      validateOutcomeFields(choice.get("outcome"), choiceLabel, errors);
    }
  }

  private static void validateOutcomeFields(
      JsonValue outcome, String choiceLabel, List<String> errors) {
    if (outcome == null || outcome.isNull()) return;
    if (!outcome.isObject()) {
      errors.add(choiceLabel + ": outcome must be a JSON object");
      return;
    }
    String label = choiceLabel + ".outcome";
    validateInteger(outcome.get("healthDelta"), label + ".healthDelta", errors);
    validateInteger(outcome.get("currencyDelta"), label + ".currencyDelta", errors);
    validateOptionalString(outcome.get("cardRewardId"), label + ".cardRewardId", errors);
    validateItemRewardIds(outcome.get("itemRewardIds"), label, errors);
  }

  private static void validateItemRewardIds(JsonValue items, String label, List<String> errors) {
    if (items == null) return;
    if (!items.isArray()) {
      errors.add(label + ".itemRewardIds must be an array when present");
      return;
    }
    int itemIndex = 0;
    for (JsonValue item = items.child; item != null; item = item.next, itemIndex++) {
      validateOptionalString(item, label + ".itemRewardIds[" + itemIndex + "]", errors);
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

  private static void validateCurseRisk(JsonValue risk, String label, List<String> errors) {
    if (risk == null) return;
    if (!risk.isObject()) {
      errors.add(label + " must be an object when present");
      return;
    }
    JsonValue chance = risk.get("triggerChance");
    if (chance == null
        || !chance.isNumber()
        || !Double.isFinite(chance.asDouble())
        || chance.asDouble() < 0
        || chance.asDouble() > 1) {
      errors.add(label + ".triggerChance must be a number between 0 and 1");
    }
    JsonValue items = risk.get("items");
    if (items == null || !items.isArray() || items.size == 0) {
      errors.add(label + ".items must be a nonempty array");
      return;
    }
    int index = 0;
    for (JsonValue item = items.child; item != null; item = item.next, index++) {
      String itemLabel = label + ".items[" + index + "]";
      if (!item.isObject()) {
        errors.add(itemLabel + " must be an object");
      } else {
        validateOptionalString(item.get("id"), itemLabel + ".id", errors);
        validateInteger(item.get("weight"), itemLabel + ".weight", errors);
      }
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
      // bitLength excludes the sign bit, so both signed int endpoints fit in 31 bits.
      if (new BigDecimal(value.asString()).toBigIntegerExact().bitLength() > Integer.SIZE - 1) {
        throw new ArithmeticException("Integer out of range");
      }
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
