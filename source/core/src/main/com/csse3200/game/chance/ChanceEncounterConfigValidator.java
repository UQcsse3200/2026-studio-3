package com.csse3200.game.chance;

import com.csse3200.game.chance.configs.ChanceChoiceConfig;
import com.csse3200.game.chance.configs.ChanceConfig;
import com.csse3200.game.chance.configs.ChanceEncounterConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates parsed Chance Encounter configuration before domain objects are created. */
final class ChanceEncounterConfigValidator {
  private ChanceEncounterConfigValidator() {
    throw new IllegalStateException("Utility class");
  }

  static List<String> validate(ChanceConfig config) {
    if (config == null || config.encounters == null) {
      return List.of("configuration must contain an 'encounters' array");
    }

    List<String> errors = new ArrayList<>();
    Set<String> encounterIds = new HashSet<>();
    long totalWeight = 0L;

    for (int i = 0; i < config.encounters.length; i++) {
      ChanceEncounterConfig encounter = config.encounters[i];
      validateEncounter(encounter, i, encounterIds, errors);
      if (encounter != null && encounter.weight != null && encounter.weight > 0) {
        totalWeight += encounter.weight;
      }
    }

    if (totalWeight > Integer.MAX_VALUE) {
      errors.add(
          "selection pool total weight must not exceed "
              + Integer.MAX_VALUE
              + ", was "
              + totalWeight);
    }

    return List.copyOf(errors);
  }

  private static void validateEncounter(
      ChanceEncounterConfig encounter,
      int encounterIndex,
      Set<String> encounterIds,
      List<String> errors) {
    String label = "encounter[" + encounterIndex + "]";
    if (encounter == null) {
      errors.add(label + " must not be null");
      return;
    }

    if (encounter.id == null || encounter.id.isBlank()) {
      errors.add(label + ": id must not be null or blank");
    } else if (!encounterIds.add(encounter.id)) {
      errors.add(label + ": duplicate encounter ID '" + encounter.id + "'");
    }

    if (encounter.description == null || encounter.description.isBlank()) {
      errors.add(label + ": description must not be null or blank");
    }

    if (encounter.weight == null) {
      errors.add(label + ": weight must be present");
    } else if (encounter.weight <= 0) {
      errors.add(label + ": weight must be positive, was " + encounter.weight);
    }

    if (encounter.choices == null || encounter.choices.length == 0) {
      errors.add(label + ": must define at least one choice");
      return;
    }

    Set<String> choiceIds = new HashSet<>();
    for (int i = 0; i < encounter.choices.length; i++) {
      validateChoice(encounter.choices[i], label + ".choice[" + i + "]", choiceIds, errors);
    }
  }

  private static void validateChoice(
      ChanceChoiceConfig choice, String label, Set<String> choiceIds, List<String> errors) {
    if (choice == null) {
      errors.add(label + " must not be null");
      return;
    }

    if (choice.id == null || choice.id.isBlank()) {
      errors.add(label + ": id must not be null or blank");
    } else if (!choiceIds.add(choice.id)) {
      errors.add(label + ": duplicate choice ID '" + choice.id + "'");
    }

    if (choice.description == null || choice.description.isBlank()) {
      errors.add(label + ": description must not be null or blank");
    }

    if (choice.outcome == null) {
      errors.add(label + ": outcome must not be null");
      return;
    }

    if (choice.outcome.healthDelta == null) {
      errors.add(label + ".outcome: healthDelta must be present");
    }
    if (choice.outcome.currencyDelta == null) {
      errors.add(label + ".outcome: currencyDelta must be present");
    }
  }
}
