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
    INTRO,
    CARD_INVENTORY,
    USED_CARD,
    ENEMY_STATS,
    HAND,
    CARD_COST,
    ENERGY,
    HEALTH,
    BUFFS,
    CARD_DRAW,
    BATTLE_OUTCOME_RULES,
    PLAY_A_CARD,
    CARD_ANIMATION,
    ENEMY_ANIMATION,
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
  private final boolean referenceSequence;
  private String demonstrationCardId;
  private int enemyHealthBefore;
  private int playerHealthBefore;

  /** Initial snapshots are supplied only by the isolated tutorial owner. */
  public void observeInitialHealth(int enemyHealth, int playerHealth) {
    enemyHealthBefore = enemyHealth;
    playerHealthBefore = playerHealth;
  }

  /** Polls real resolution and visual completion; never applies damage or advances a turn. */
  public void observeResolution(
      int enemyHealth, int playerHealth, boolean settled, BattlePhase phase) {
    if (closed || !referenceSequence || !settled || phase != BattlePhase.PLAYER_TURN) return;
    if (currentStep == Step.CARD_ANIMATION && enemyHealth < enemyHealthBefore) {
      show(Step.ENEMY_STATS);
    } else if (currentStep == Step.ENEMY_ANIMATION) {
      show(Step.HEALTH);
    }
  }

  public BattleTutorialController() {
    this(false);
  }

  public BattleTutorialController(boolean referenceSequence) {
    this.referenceSequence = referenceSequence;
  }

  /** Exact tutorial copy, not a definition ID shared by duplicate cards. */
  public void setDemonstrationCardId(String instanceId) {
    demonstrationCardId = instanceId;
  }

  private final EventListener2<String, String> cardListener =
      (instanceId, targetId) -> onSuccessfulCardPlay(instanceId);

  public void onSuccessfulCardPlay(String instanceId) {
    if (!referenceSequence
        || demonstrationCardId == null
        || demonstrationCardId.equals(instanceId)) {
      onSuccessfulCardPlay();
    }
  }

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
        referenceSequence
            ? switch (currentStep) {
              case INTRO -> Step.HAND;
              case HAND -> Step.CARD_INVENTORY;
              case CARD_INVENTORY -> Step.ENERGY;
              case ENERGY -> Step.CARD_COST;
              case CARD_COST -> Step.PLAY_A_CARD;
              case ENEMY_STATS -> Step.USED_CARD;
              case USED_CARD -> Step.END_TURN;
              case END_TURN -> Step.HEALTH;
              case HEALTH -> Step.FREE_PLAY;
              case BUFFS -> Step.CARD_DRAW;
              case CARD_DRAW -> Step.BATTLE_OUTCOME_RULES;
              case BATTLE_OUTCOME_RULES -> Step.FREE_PLAY;
              default -> null;
            }
            : switch (currentStep) {
              case HAND -> Step.CARD_COST;
              case CARD_COST -> Step.ENERGY;
              case ENERGY -> Step.HEALTH;
              case HEALTH -> Step.BUFFS;
              case BUFFS -> Step.CARD_DRAW;
              case CARD_DRAW -> Step.BATTLE_OUTCOME_RULES;
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
    if (referenceSequence && currentStep != Step.PLAY_A_CARD) return;
    cardPlayed = true;
    if (currentStep == Step.PLAY_A_CARD) {
      show(referenceSequence ? Step.CARD_ANIMATION : Step.END_TURN);
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
      show(referenceSequence ? Step.ENEMY_ANIMATION : Step.FREE_PLAY);
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
    if (!referenceSequence && next == Step.PLAY_A_CARD && cardPlayed) {
      show(Step.END_TURN);
    } else if (!referenceSequence && next == Step.END_TURN && turnEnded) {
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
