package com.csse3200.game.encounters.integration;

import com.csse3200.game.chance.ChanceBehaviourResult;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceEncounterBehaviour;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.chance.DiceRoll;
import com.csse3200.game.chance.FixedChanceEncounterBehaviour;
import com.csse3200.game.maps.EncounterCallback;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Coordinates choice resolution, player updates, and map completion for one Chance Encounter. */
public final class ChanceEncounterSession {
  private final Integer nodeId;
  private final ChanceEncounter encounter;
  private final ChanceEncounterBehaviour behaviour;
  private final ChanceOutcomeApplier outcomeApplier;
  private final EncounterCallback completionCallback;

  private ChanceResolution resolution;
  private boolean awaitingDelegatedCompletion;
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
    this(
        nodeId,
        encounter,
        new FixedChanceEncounterBehaviour(encounter),
        outcomeApplier,
        completionCallback);
  }

  /**
   * Creates one Chance Encounter lifecycle session with an injected choice behaviour.
   *
   * <p>The encounter remains the immutable display definition. The behaviour independently resolves
   * choices, allowing later encounter types to compute runtime outcomes or delegate to a separate
   * flow without changing ordinary outcome application.
   *
   * @param nodeId map node that launched the encounter
   * @param encounter Chance Encounter definition displayed by the UI
   * @param behaviour player-independent choice resolver
   * @param outcomeApplier service applying normal outcomes to the player
   * @param completionCallback callback returning control to the map
   */
  public ChanceEncounterSession(
      Integer nodeId,
      ChanceEncounter encounter,
      ChanceEncounterBehaviour behaviour,
      ChanceOutcomeApplier outcomeApplier,
      EncounterCallback completionCallback) {
    if (nodeId == null) {
      throw new IllegalArgumentException("nodeId cannot be null");
    }
    this.nodeId = nodeId;
    this.encounter = Objects.requireNonNull(encounter, "encounter cannot be null");
    this.behaviour = Objects.requireNonNull(behaviour, "behaviour cannot be null");
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
    if (awaitingDelegatedCompletion) {
      return outcomeApplier.failure(
          ChanceResolution.Status.DELEGATED,
          null,
          "This encounter is awaiting completion from a delegated flow.");
    }

    ChanceBehaviourResult behaviourResult = behaviour.resolveChoice(choiceId);
    if (behaviourResult == null) {
      return outcomeApplier.failure(
          ChanceResolution.Status.INVALID_OUTCOME,
          null,
          "The encounter behaviour did not provide a resolution result.");
    }
    if (behaviourResult.getType() == ChanceBehaviourResult.Type.INVALID_CHOICE) {
      return outcomeApplier.failure(
          ChanceResolution.Status.INVALID_CHOICE, null, "The selected choice does not exist.");
    }
    if (behaviourResult.getType() == ChanceBehaviourResult.Type.AWAITING_CHOICE) {
      return outcomeApplier.failure(
          ChanceResolution.Status.AWAITING_CHOICE,
          null,
          "The encounter advanced and is awaiting another choice.");
    }
    if (behaviourResult.getType() == ChanceBehaviourResult.Type.DELEGATED) {
      awaitingDelegatedCompletion = true;
      return outcomeApplier.failure(
          ChanceResolution.Status.DELEGATED,
          null,
          "The selected choice is continuing in a delegated flow.");
    }

    ChanceOutcome outcome = behaviourResult.getOutcome();
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
   * Completes an encounter that handed control to a delegated flow.
   *
   * <p>A delegated flow owns its own result. For example, Card Fusion may either fuse cards or
   * leave without fusing, but either choice consumes the Event node and returns the player to the
   * map.
   *
   * @return true only when a pending delegated flow is completed for the first time
   */
  public boolean completeDelegated() {
    if (completed || !awaitingDelegatedCompletion) {
      return false;
    }
    awaitingDelegatedCompletion = false;
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

  /** Returns the catalogue choices valid at the current stage, in catalogue order. */
  public List<ChanceChoice> getAvailableChoices() {
    if (completed || isResolved()) {
      return List.of();
    }
    List<String> availableIds = behaviour.getAvailableChoiceIds(encounter);
    return encounter.getChoices().stream()
        .filter(choice -> availableIds.contains(choice.getId()))
        .toList();
  }

  /** Returns the current stage's player-facing choice instruction. */
  public String getChoicePrompt() {
    return behaviour.getChoicePrompt();
  }

  /** Returns feedback for an accepted choice that advanced to another stage. */
  public String getStageResultText() {
    return behaviour.getStageResultText();
  }

  /** Returns the latest two dice faces without changing the encounter result. */
  public Optional<DiceRoll> getLastDiceRoll() {
    return behaviour.getLastDiceRoll();
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
   * Reports whether this session is waiting for a delegated flow.
   *
   * @return true after behaviour delegates resolution and before the session is cancelled
   */
  public boolean isAwaitingDelegatedCompletion() {
    return awaitingDelegatedCompletion && !completed;
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
