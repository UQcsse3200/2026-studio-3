package com.csse3200.game.encounters.integration;

import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.maps.EncounterCallback;
import java.util.Objects;

/** Coordinates choice resolution, player updates, and map completion for one Chance Encounter. */
public final class ChanceEncounterSession {
  private final Integer nodeId;
  private final ChanceEncounter encounter;
  private final ChanceOutcomeApplier outcomeApplier;
  private final EncounterCallback completionCallback;

  private ChanceResolution resolution;
  private boolean completed;

  /**
   * Creates one Chance Encounter lifecycle session.
   *
   * @param nodeId map node that launched the encounter
   * @param encounter Chance Encounter definition
   * @param outcomeApplier service applying the selected outcome to the player
   * @param completionCallback callback returning control to the map
   */
  public ChanceEncounterSession(
      Integer nodeId,
      ChanceEncounter encounter,
      ChanceOutcomeApplier outcomeApplier,
      EncounterCallback completionCallback) {
    if (nodeId == null) {
      throw new IllegalArgumentException("nodeId cannot be null");
    }
    this.nodeId = nodeId;
    this.encounter = Objects.requireNonNull(encounter, "encounter cannot be null");
    this.outcomeApplier = Objects.requireNonNull(outcomeApplier, "outcomeApplier cannot be null");
    this.completionCallback = completionCallback;
  }

  /**
   * Resolves one choice and applies its outcome to the player.
   *
   * @param choiceId selected choice identifier
   * @return application result; failed attempts do not close the encounter
   */
  public ChanceResolution resolveChoice(String choiceId) {
    if (completed) {
      return outcomeApplier.failure(
          ChanceResolution.Status.ENCOUNTER_CLOSED, null, "This encounter has already ended.");
    }
    if (resolution != null && resolution.isSuccess()) {
      return outcomeApplier.failure(
          ChanceResolution.Status.ALREADY_RESOLVED,
          resolution.getOutcome(),
          "A choice has already been resolved.");
    }

    ChanceOutcome outcome = encounter.resolveChoice(choiceId);
    if (outcome == null) {
      return outcomeApplier.failure(
          ChanceResolution.Status.INVALID_CHOICE, null, "The selected choice does not exist.");
    }

    ChanceResolution attempt = outcomeApplier.apply(outcome);
    if (attempt.isSuccess()) {
      resolution = attempt;
    }
    return attempt;
  }

  /**
   * Completes a successfully resolved encounter and returns to the map.
   *
   * @return true only for the first valid completion call
   */
  public boolean complete() {
    if (completed || resolution == null || !resolution.isSuccess()) {
      return false;
    }
    completed = true;
    if (completionCallback != null) {
      completionCallback.onEncounterComplete(nodeId, true);
    }
    return true;
  }

  /**
   * Leaves the encounter without advancing map progression.
   *
   * @return true only for the first cancellation call
   */
  public boolean cancel() {
    if (completed) {
      return false;
    }
    completed = true;
    if (completionCallback != null) {
      completionCallback.onEncounterComplete(nodeId, false);
    }
    return true;
  }

  /**
   * Returns the node that launched this session.
   *
   * @return map node ID associated with this session
   */
  public Integer getNodeId() {
    return nodeId;
  }

  /**
   * Returns the immutable encounter definition displayed by the UI.
   *
   * @return Chance Encounter definition used by this session
   */
  public ChanceEncounter getEncounter() {
    return encounter;
  }

  /**
   * Returns the successful resolution retained by this session.
   *
   * @return successful applied resolution, or null before a choice succeeds
   */
  public ChanceResolution getResolution() {
    return resolution;
  }

  /**
   * Reports whether a choice has been committed.
   *
   * @return true after a choice has been applied successfully
   */
  public boolean isResolved() {
    return resolution != null && resolution.isSuccess();
  }

  /**
   * Reports whether this session has notified its completion callback.
   *
   * @return true after completion or cancellation has been reported
   */
  public boolean isCompleted() {
    return completed;
  }
}
