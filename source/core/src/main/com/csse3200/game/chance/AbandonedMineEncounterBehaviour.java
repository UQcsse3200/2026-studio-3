package com.csse3200.game.chance;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

/** Resolves and retains one Mine exploration roll for the lifetime of its Event session. */
public final class AbandonedMineEncounterBehaviour implements ChanceEncounterBehaviour {
  public static final String ENCOUNTER_ID = "abandoned-mine";
  public static final String SEARCH_CHOICE_ID = "search-tunnels";
  public static final String LEAVE_CHOICE_ID = "leave";
  private final ChanceEncounter encounter;
  private final Random random;
  private final ChanceChoice search;
  private final ChanceCurseRisk risk;
  private ChanceOutcome exploration;

  /** Creates a fresh per-session behaviour with an injected random source and configured pool. */
  public AbandonedMineEncounterBehaviour(ChanceEncounter encounter, Random random) {
    this.encounter = Objects.requireNonNull(encounter);
    this.random = Objects.requireNonNull(random);
    search =
        encounter.getChoices().stream()
            .filter(choice -> SEARCH_CHOICE_ID.equals(choice.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Mine requires search-tunnels"));
    risk =
        search
            .getCurseRisk()
            .orElseThrow(() -> new IllegalArgumentException("Mine requires curseRisk"));
  }

  @Override
  public ChanceBehaviourResult resolveChoice(String choiceId) {
    if (LEAVE_CHOICE_ID.equals(choiceId)) {
      ChanceOutcome leave = encounter.resolveChoice(choiceId);
      return leave == null
          ? ChanceBehaviourResult.invalidChoice()
          : ChanceBehaviourResult.outcome(leave);
    }
    if (!SEARCH_CHOICE_ID.equals(choiceId)) return ChanceBehaviourResult.invalidChoice();
    if (exploration == null) {
      ChanceOutcome base = search.resolve();
      var items = new ArrayList<>(base.getItemRewardIds());
      if (random.nextDouble() < risk.triggerChance()) {
        int total = risk.items().stream().mapToInt(ChanceCurseRisk.WeightedItem::weight).sum();
        int roll = random.nextInt(total);
        for (ChanceCurseRisk.WeightedItem item : risk.items()) {
          if (roll < item.weight()) {
            items.add(item.id());
            break;
          }
          roll -= item.weight();
        }
      }
      // Retain the complete outcome before application so every failed transaction retries it.
      exploration =
          ChanceOutcome.withRewards(
              base.getHealthDelta(), base.getCurrencyDelta(), base.getCardRewardIds(), items);
    }
    return ChanceBehaviourResult.outcome(exploration);
  }
}
