package com.csse3200.game.chance;

/** Represents one selectable option in a Chance Encounter. */
public final class ChanceChoice {
  private final String id;
  private final String description;
  private final ChanceOutcome outcome;

  /**
   * Creates a Chance Encounter choice.
   *
   * @param id stable choice identifier
   * @param description player-facing choice description
   * @param outcome outcome produced when this choice is resolved
   */
  public ChanceChoice(String id, String description, ChanceOutcome outcome) {
    this.id = id;
    this.description = description;
    this.outcome = outcome;
  }

  /**
   * Gets the stable choice identifier.
   *
   * @return choice identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the player-facing choice description.
   *
   * @return choice description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Resolves this choice to its associated outcome.
   *
   * @return associated Chance Encounter outcome
   */
  public ChanceOutcome resolve() {
    return outcome;
  }
}
