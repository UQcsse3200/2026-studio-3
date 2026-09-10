package com.csse3200.game.chance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.csse3200.game.chance.configs.ChanceChoiceConfig;
import com.csse3200.game.chance.configs.ChanceConfig;
import com.csse3200.game.chance.configs.ChanceEncounterConfig;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads Chance Encounter definitions from a JSON configuration file. */
public final class ChanceEncounterConfigLoader {
  public static final String DEFAULT_ENCOUNTER_FILE = "configs/chanceEncounters.json";

  private static final Logger logger = LoggerFactory.getLogger(ChanceEncounterConfigLoader.class);

  private ChanceEncounterConfigLoader() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Loads Chance Encounters from the default configuration file.
   *
   * @return immutable encounter definitions in configuration order
   * @throws ChanceEncounterLoadingException if the configuration cannot be loaded
   */
  public static List<ChanceEncounter> loadEncounters() {
    return loadEncounters(DEFAULT_ENCOUNTER_FILE);
  }

  /**
   * Loads Chance Encounters from a JSON configuration file.
   *
   * <p>This initial loader checks file and structural parsing failures. Full semantic validation is
   * handled by a later Issue #148 implementation step.
   *
   * @param filename internal asset path of the configuration file
   * @return immutable encounter definitions in configuration order
   * @throws ChanceEncounterLoadingException if the path, JSON, or required structure is invalid
   */
  public static List<ChanceEncounter> loadEncounters(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration filename must not be null or blank");
    }

    FileHandle file = Gdx.files.internal(filename);
    if (!file.exists()) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration file does not exist: " + filename);
    }

    ChanceConfig config;
    try {
      config = new Json().fromJson(ChanceConfig.class, file);
    } catch (RuntimeException exception) {
      throw new ChanceEncounterLoadingException(
          "Malformed Chance Encounter configuration file: " + filename, exception);
    }

    if (config == null || config.encounters == null) {
      throw new ChanceEncounterLoadingException(
          "Chance Encounter configuration must contain an 'encounters' array: " + filename);
    }

    List<ChanceEncounter> encounters = new ArrayList<>();
    for (int i = 0; i < config.encounters.length; i++) {
      encounters.add(toDomain(config.encounters[i], i, filename));
    }

    logger.info("Loaded {} Chance Encounter definitions from {}", encounters.size(), filename);
    return List.copyOf(encounters);
  }

  private static ChanceEncounter toDomain(
      ChanceEncounterConfig encounterConfig, int encounterIndex, String filename) {
    if (encounterConfig == null || encounterConfig.choices == null) {
      throw invalidStructure(filename, "encounter[" + encounterIndex + "] is incomplete");
    }

    List<ChanceChoice> choices = new ArrayList<>();
    for (int i = 0; i < encounterConfig.choices.length; i++) {
      ChanceChoiceConfig choiceConfig = encounterConfig.choices[i];
      if (choiceConfig == null || choiceConfig.outcome == null) {
        throw invalidStructure(
            filename, "encounter[" + encounterIndex + "].choice[" + i + "] is incomplete");
      }

      ChanceOutcome outcome =
          new ChanceOutcome(choiceConfig.outcome.healthDelta, choiceConfig.outcome.currencyDelta);
      choices.add(new ChanceChoice(choiceConfig.id, choiceConfig.description, outcome));
    }

    return new ChanceEncounter(encounterConfig.id, encounterConfig.description, choices);
  }

  private static ChanceEncounterLoadingException invalidStructure(String filename, String detail) {
    return new ChanceEncounterLoadingException(
        "Invalid Chance Encounter configuration in " + filename + ": " + detail);
  }
}
