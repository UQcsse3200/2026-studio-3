package com.csse3200.game.chance;

import com.csse3200.game.chance.configs.ChanceChoiceConfig;
import com.csse3200.game.chance.configs.ChanceConfig;
import com.csse3200.game.chance.configs.ChanceCurseRiskConfig;
import com.csse3200.game.chance.configs.ChanceEncounterConfig;
import com.csse3200.game.encounters.integration.CardCatalogGateway;
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
    return validate(config, null);
  }

  static List<String> validate(ChanceConfig config, CardCatalogGateway cardCatalog) {
    if (config == null || config.encounters == null) {
      return List.of("configuration must contain an 'encounters' array");
    }

    List<String> errors = new ArrayList<>();
    Set<String> encounterIds = new HashSet<>();
    long totalWeight = 0L;

    for (int i = 0; i < config.encounters.length; i++) {
      ChanceEncounterConfig encounter = config.encounters[i];
      validateEncounter(encounter, i, encounterIds, cardCatalog, errors);
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
      CardCatalogGateway cardCatalog,
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
      validateCurseRisk(encounter, encounter.choices[i], label + ".choice[" + i + "]", errors);
      validateChoice(
          encounter.choices[i], label + ".choice[" + i + "]", choiceIds, cardCatalog, errors);
    }
  }

  private static void validateChoice(
      ChanceChoiceConfig choice,
      String label,
      Set<String> choiceIds,
      CardCatalogGateway cardCatalog,
      List<String> errors) {
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

    validateItemRewards(choice, label, errors);
    validateCardReward(choice.outcome.cardRewardId, label, cardCatalog, errors);
  }

  private static void validateItemRewards(
      ChanceChoiceConfig choice, String label, List<String> errors) {
    if (choice.outcome.itemRewardIds != null) {
      for (String itemId : choice.outcome.itemRewardIds) {
        if (itemId == null || !itemId.matches("[A-Z][A-Z0-9_]*")) {
          errors.add(label + ".outcome: invalid itemRewardId '" + itemId + "'");
        }
      }
    }
  }

  private static void validateCardReward(
      String cardRewardId, String label, CardCatalogGateway cardCatalog, List<String> errors) {
    if (cardRewardId != null && cardRewardId.isBlank()) {
      errors.add(label + ".outcome: cardRewardId must not be blank when present");
    } else if (cardRewardId != null && cardCatalog != null) {
      boolean knownCard;
      try {
        knownCard = cardCatalog.containsCard(cardRewardId);
      } catch (RuntimeException exception) {
        knownCard = false;
      }
      if (!knownCard) {
        errors.add(label + ".outcome: unknown cardRewardId '" + cardRewardId + "'");
      }
    }
  }

  private static void validateCurseRisk(
      ChanceEncounterConfig encounter,
      ChanceChoiceConfig choice,
      String label,
      List<String> errors) {
    if (choice == null || choice.curseRisk == null) return;
    if (!AbandonedMineEncounterBehaviour.ENCOUNTER_ID.equals(encounter.id)
        || !AbandonedMineEncounterBehaviour.SEARCH_CHOICE_ID.equals(choice.id)) {
      errors.add(label + ": curseRisk is supported only for abandoned-mine/search-tunnels");
    }
    var risk = choice.curseRisk;
    if (risk.triggerChance == null
        || !Double.isFinite(risk.triggerChance)
        || risk.triggerChance < 0
        || risk.triggerChance > 1) {
      errors.add(label + ": triggerChance must be between 0 and 1");
    }
    if (risk.items == null || risk.items.length == 0) {
      errors.add(label + ": Curse item pool must not be empty");
      return;
    }
    validateCursePool(risk, label, errors);
    validateFixedCurseRewards(choice, label, errors);
  }

  private static void validateCursePool(
      ChanceCurseRiskConfig risk, String label, List<String> errors) {
    Set<String> ids = new HashSet<>();
    long total = 0;
    for (var item : risk.items) {
      if (item == null || item.id == null || !item.id.matches("[A-Z][A-Z0-9_]*")) {
        errors.add(label + ": invalid Curse item ID");
        continue;
      }
      if (!ids.add(item.id)) errors.add(label + ": duplicate Curse item ID '" + item.id + "'");
      if (item.weight == null || item.weight <= 0) {
        errors.add(label + ": Curse item weight must be positive");
      } else total += item.weight;
    }
    if (total > Integer.MAX_VALUE) errors.add(label + ": Curse weight total exceeds integer range");
  }

  private static void validateFixedCurseRewards(
      ChanceChoiceConfig choice, String label, List<String> errors) {
    if (choice.outcome != null
        && (choice.outcome.cardRewardId != null
            || (choice.outcome.itemRewardIds != null && choice.outcome.itemRewardIds.length > 0))) {
      errors.add(label + ": Curse risk choice must not contain fixed Card or Item rewards");
    }
  }
}
