package com.csse3200.game.chance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents one Player-independent Chance Encounter definition. */
public final class ChanceEncounter {
  private final String id;
  private final String description;
  private final List<ChanceChoice> choices;

  /**
   * Creates a Chance Encounter definition.
   *
   * <p>The encounter data, choices, choice identifiers, and outcomes are expected to be non-null.
   * Choice identifiers are expected to be unique within the encounter.
   *
   * @param id stable encounter definition identifier
   * @param description player-facing encounter description
   * @param choices ordered choices available for this encounter
   */
  public ChanceEncounter(String id, String description, List<ChanceChoice> choices) {
    this.id = id;
    this.description = description;
    this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
  }

  /**
   * Gets the stable encounter definition identifier.
   *
   * @return encounter identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the player-facing encounter description.
   *
   * @return encounter description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the ordered, read-only choices available for this encounter.
   *
   * @return encounter choices
   */
  public List<ChanceChoice> getChoices() {
    return choices;
  }

  /**
   * Resolves the choice with the given stable identifier.
   *
   * @param choiceId choice identifier
   * @return associated outcome, or null when the identifier is null or unknown
   */
  public ChanceOutcome resolveChoice(String choiceId) {
    if (choiceId == null) {
      return null;
    }

    for (ChanceChoice choice : choices) {
      if (choiceId.equals(choice.getId())) {
        return choice.resolve();
      }
    }
    return null;
  }
}
