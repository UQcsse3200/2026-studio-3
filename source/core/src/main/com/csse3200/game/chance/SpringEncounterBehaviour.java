package com.csse3200.game.chance;

import com.csse3200.game.cards.CardService;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Resolves the runtime-random outcomes of the Spring Event. */
public final class SpringEncounterBehaviour implements ChanceEncounterBehaviour {
  /** Stable identifier of the Spring Event's existing catalogue definition. */
  public static final String ENCOUNTER_ID = "healing-spring";

  /** Stable identifier of the choice that rolls a Spring outcome. */
  public static final String DRINK_CHOICE_ID = "drink";

  /** Stable identifier of the choice that leaves without an effect. */
  public static final String LEAVE_CHOICE_ID = "leave";

  private static final int OUTCOME_BOUND = 100;
  private static final int HEAL_UPPER_BOUND = 50;
  private static final int CARD_UPPER_BOUND = 80;
  private static final int HEAL_AMOUNT = 20;

  private final Random random;
  private final List<String> eligibleCardIds;

  /**
   * Creates Spring behaviour using all registered base-card definitions as eligible rewards.
   *
   * <p>Card IDs are deduplicated and sorted before selection so an injected random source produces
   * deterministic results independently of the Card Service's collection order.
   *
   * @param random injected source used for outcome and card selection
   * @param cardService authoritative source of registered card definitions
   * @throws IllegalArgumentException when the Card Service has no eligible card definitions
   */
  public SpringEncounterBehaviour(Random random, CardService cardService) {
    this.random = Objects.requireNonNull(random, "random cannot be null");
    Objects.requireNonNull(cardService, "cardService cannot be null");

    eligibleCardIds =
        cardService.getAllCards().stream()
            .filter(Objects::nonNull)
            .map(card -> card.id)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .sorted()
            .toList();
    if (eligibleCardIds.isEmpty()) {
      throw new IllegalArgumentException("Spring Event requires at least one registered card");
    }
  }

  @Override
  public ChanceBehaviourResult resolveChoice(String choiceId) {
    if (LEAVE_CHOICE_ID.equals(choiceId)) {
      return ChanceBehaviourResult.outcome(new ChanceOutcome(0, 0));
    }
    if (!DRINK_CHOICE_ID.equals(choiceId)) {
      return ChanceBehaviourResult.invalidChoice();
    }

    int outcomeRoll = random.nextInt(OUTCOME_BOUND);
    if (outcomeRoll < HEAL_UPPER_BOUND) {
      return ChanceBehaviourResult.outcome(new ChanceOutcome(HEAL_AMOUNT, 0));
    }
    if (outcomeRoll < CARD_UPPER_BOUND) {
      String cardId = eligibleCardIds.get(random.nextInt(eligibleCardIds.size()));
      return ChanceBehaviourResult.outcome(new ChanceOutcome(0, 0, cardId));
    }
    return ChanceBehaviourResult.outcome(new ChanceOutcome(0, 0));
  }
}
