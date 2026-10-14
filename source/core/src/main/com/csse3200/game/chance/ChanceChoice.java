package com.csse3200.game.chance;

import java.util.Optional;

/** Represents one selectable option in a Chance Encounter. */
public final class ChanceChoice {
  private final String id;
  private final String description;
  private final ChanceOutcome outcome;
  private final ChanceCurseRisk curseRisk;

  /**
   * Creates a Chance Encounter choice.
   *
   * @param id stable choice identifier
   * @param description player-facing choice description
   * @param outcome configured base reward, before any runtime behaviour adds random rewards
   */
  public ChanceChoice(String id, String description, ChanceOutcome outcome) {
    this(id, description, outcome, null);
  }

  /** Creates a choice with base rewards and optional risk resolved by its runtime behaviour. */
  public ChanceChoice(
      String id, String description, ChanceOutcome outcome, ChanceCurseRisk curseRisk) {
    this.id = id;
    this.description = description;
    this.outcome = outcome;
    this.curseRisk = curseRisk;
  }

  /** Returns the optional immutable Curse pool without drawing a random result. */
  public Optional<ChanceCurseRisk> getCurseRisk() {
    return Optional.ofNullable(curseRisk);
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
   * Returns this choice's configured base outcome. Runtime behaviour resolves optional Curse risk.
   *
   * @return associated Chance Encounter outcome
   */
  public ChanceOutcome resolve() {
    return outcome;
  }
}
