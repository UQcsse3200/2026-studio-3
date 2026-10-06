package com.csse3200.game.tutorial;

import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.events.listeners.EventListener2;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Tracks tutorial guidance without changing the rules or state of the battle it observes. */
public final class BattleTutorialController implements AutoCloseable {
  public enum Step {
    HAND,
    CARD_COST,
    ENERGY,
    HEALTH,
    BUFFS,
    DRAW_AND_TURNS,
    BATTLE_OUTCOME_RULES,
    PLAY_A_CARD,
    END_TURN,
    FREE_PLAY,
    BATTLE_ENDED,
    CANCELLED
  }

  public enum Outcome {
    WON,
    LOST,
    CANCELLED
  }

  private final List<Consumer<Step>> stepListeners = new ArrayList<>();
  private final List<Consumer<Outcome>> outcomeListeners = new ArrayList<>();
  private Step currentStep;
  private Outcome outcome;
  private boolean cardPlayed;
  private boolean turnEnded;
  private boolean bound;
  private boolean closed;
  private BattleController battle;
  private final EventListener2<String, String> cardListener =
      (instanceId, targetId) -> onSuccessfulCardPlay();
  private final EventListener2<BattlePhase, BattlePhase> phaseListener =
      (previous, next) -> onPhaseChanged(next);
  private final EventListener1<Boolean> endListener = this::onBattleEnded;

  /** Connects this guide to an existing battle before that battle starts. */
  public void bindTo(BattleController battle) {
    Objects.requireNonNull(battle, "battle cannot be null");
    requireOpen();
    if (bound) {
      throw new IllegalStateException("Tutorial is already bound to a battle");
    }
    bound = true;
    this.battle = battle;
    battle.addCardPlayedListener(cardListener);
    battle.addPhaseChangeListener(phaseListener);
    battle.addBattleEndListener(endListener);
  }

  public void addStepListener(Consumer<Step> listener) {
    requireOpen();
    stepListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
  }

  public void addOutcomeListener(Consumer<Outcome> listener) {
    requireOpen();
    outcomeListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
  }

  public Optional<Step> getCurrentStep() {
    return Optional.ofNullable(currentStep);
  }

  public Optional<Outcome> getOutcome() {
    return Optional.ofNullable(outcome);
  }

  /** Starts the guide once. Listeners should be registered before calling this method. */
  public boolean start() {
    if (closed || currentStep != null) {
      return false;
    }
    show(Step.HAND);
    return true;
  }

  /** Advances explanatory steps only; action steps need a real battle event. */
  public boolean continueInformation() {
    if (closed || currentStep == null || outcome != null) {
      return false;
    }
    Step next =
        switch (currentStep) {
          case HAND -> Step.CARD_COST;
          case CARD_COST -> Step.ENERGY;
          case ENERGY -> Step.HEALTH;
          case HEALTH -> Step.BUFFS;
          case BUFFS -> Step.DRAW_AND_TURNS;
          case DRAW_AND_TURNS -> Step.BATTLE_OUTCOME_RULES;
          case BATTLE_OUTCOME_RULES -> Step.PLAY_A_CARD;
          default -> null;
        };
    if (next == null) {
      return false;
    }
    show(next);
    return true;
  }

  /** Only a successful card play counts; rejected attempts never call this hook. */
  public void onSuccessfulCardPlay() {
    if (closed || currentStep == null || outcome != null) {
      return;
    }
    cardPlayed = true;
    if (currentStep == Step.PLAY_A_CARD) {
      show(Step.END_TURN);
    }
  }

  /** The player-end phase is reached only after the player actually ends their turn. */
  public void onPhaseChanged(BattlePhase nextPhase) {
    if (closed
        || currentStep == null
        || outcome != null
        || !cardPlayed
        || nextPhase != BattlePhase.PLAYER_END) {
      return;
    }
    turnEnded = true;
    if (currentStep == Step.END_TURN) {
      show(Step.FREE_PLAY);
    }
  }

  /** Reports the real battle result; the owning flow decides where to navigate afterward. */
  public void onBattleEnded(Boolean won) {
    if (closed || currentStep == null || outcome != null || won == null) {
      return;
    }
    finish(Boolean.TRUE.equals(won) ? Outcome.WON : Outcome.LOST, Step.BATTLE_ENDED);
  }

  /** Stops the guide when its owning battle is exited. */
  public void cancel() {
    if (closed || currentStep == null || outcome != null) {
      return;
    }
    finish(Outcome.CANCELLED, Step.CANCELLED);
  }

  private void show(Step next) {
    currentStep = next;
    for (Consumer<Step> listener : List.copyOf(stepListeners)) {
      if (closed || currentStep != next) {
        return;
      }
      listener.accept(next);
    }

    // Actions performed while the player was reading earlier prompts still count.
    if (closed || currentStep != next) {
      return;
    }
    if (next == Step.PLAY_A_CARD && cardPlayed) {
      show(Step.END_TURN);
    } else if (next == Step.END_TURN && turnEnded) {
      show(Step.FREE_PLAY);
    }
  }

  private void finish(Outcome result, Step finalStep) {
    outcome = result;
    show(finalStep);
    for (Consumer<Outcome> listener : List.copyOf(outcomeListeners)) {
      if (closed) {
        return;
      }
      listener.accept(result);
    }
  }

  /**
   * Releases observers without reporting cancellation or navigating. Call from screen disposal,
   * outside a battle event dispatch; the tutorial component handles this timing automatically.
   */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    if (battle != null) {
      battle.removeCardPlayedListener(cardListener);
      battle.removePhaseChangeListener(phaseListener);
      battle.removeBattleEndListener(endListener);
      battle = null;
    }
    stepListeners.clear();
    outcomeListeners.clear();
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Tutorial is closed");
    }
  }
}
