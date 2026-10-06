package com.csse3200.game.components.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Timer;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;

/** Connects battle UI events to valid transitions in the battle controller. */
public class BattleActions extends Component {
  static final String END_TURN_SELECTED_EVENT = "endTurnSelected";
  static final String PHASE_CHANGED_EVENT = "phaseChange";

  /** Fired on the battle UI entity with the latest one-line action description. */
  public static final String BATTLE_LOG_EVENT = "battleLog";

  /** Fired on the battle UI entity with the resolved enemy-facing effects, for Team 1. */
  public static final String ENEMY_EFFECTS_EVENT = "enemyEffects";

  /** Fired on the battle UI entity with the resolved player-facing effects, for Team 7. */
  public static final String PLAYER_EFFECTS_EVENT = "playerEffects";

  /** Fired on the battle UI entity when the player wins the battle. */
  public static final String BATTLE_WON_EVENT = "battleWon";

  /** Fired on the battle UI entity when the player loses the battle. */
  public static final String BATTLE_LOST_EVENT = "battleLost";

  /** Fired on the battle UI entity with the player's hand after it changes (e.g. a card played). */
  public static final String HAND_CHANGED_EVENT = "handChanged";

  /** How long the enemy "thinks" before its action, log line and effects are revealed. */
  private static final float ENEMY_TURN_DELAY = 1.2f;

  private final BattleController controller;
  private final GdxGame game;
  private final List<Entity> enemies;

  // While true, reveals (log, effects, phase changes, the hand coming back up) are queued instead
  // of fired immediately, so the enemy's whole turn can be held back and replayed together after
  // ENEMY_TURN_DELAY. This is the one point the "enemy thinks" pause lives — see dispatch() and
  // flushDeferredReveals().
  private boolean deferringEnemyTurn = false;
  private final List<Runnable> queuedReveals = new ArrayList<>();
  private final List<EnemyReleaseComponent> releaseAnimations = new ArrayList<>();
  private boolean awaitingRelease;
  private boolean released;

  public BattleActions(BattleController controller, GdxGame game) {
    this(controller, game, List.of());
  }

  public BattleActions(BattleController controller, GdxGame game, List<Entity> enemies) {
    this.controller = controller;
    this.game = game;
    this.enemies = List.copyOf(enemies);
  }

  /**
   * Returns whether the battle is waiting for player input.
   *
   * @return true only during the player turn, excluding card resolution and all other phases
   */
  public boolean isPlayerTurn() {
    return controller.isPlayerTurn();
  }

  @Override
  public void create() {
    entity.getEvents().addListener("battle", this::onStart);

    entity.getEvents().addListener(END_TURN_SELECTED_EVENT, controller::endPlayerTurn);
    controller.addPhaseChangeListener(this::onPhaseChange);
    entity.getEvents().addListener("endTurn", this::triggerEndTurn);

    // Re-broadcast the controller's battle-loop signals as plain entity events so the battle-log
    // UI, Team 1 (enemy effects) and Team 7 (player effects) can all subscribe in one place.
    // Log and enemy-effects reveals go through dispatch() so they hold back during the enemy's
    // "thinking" pause instead of appearing the instant the controller computes them.
    controller.addBattleLogListener(
        message -> dispatch(() -> entity.getEvents().trigger(BATTLE_LOG_EVENT, message)));
    controller.addEnemyEffectsListener(effects -> dispatch(() -> onEnemyEffects(effects)));
    controller.addPlayerEffectsListener(this::onPlayerEffects);
    controller.addBattleEndListener(this::onBattleEnd);
    controller.addHandChangedListener(hand -> entity.getEvents().trigger(HAND_CHANGED_EVENT, hand));
  }

  private void onPhaseChange(BattlePhase previousPhase, BattlePhase nextPhase) {
    // Entering the enemy's turn fires immediately — no delay on ending your own turn — and starts
    // holding back every reveal that follows until the whole enemy turn is done.
    if (nextPhase == BattlePhase.ENEMY_TURN && previousPhase == BattlePhase.PLAYER_END) {
      deferringEnemyTurn = true;
      entity.getEvents().trigger(PHASE_CHANGED_EVENT, nextPhase);
      return;
    }

    if (nextPhase == BattlePhase.PLAYER_TURN
        && previousPhase == BattlePhase.PLAYER_START
        && controller.getPlayerTurnNumber() > 1) {
      dispatch(
          () -> entity.getEvents().trigger("playerTurnStarted", controller.getPlayerTurnNumber()));
    }

    dispatch(() -> entity.getEvents().trigger(PHASE_CHANGED_EVENT, nextPhase));

    boolean enemyTurnOver =
        (nextPhase == BattlePhase.PLAYER_TURN && previousPhase == BattlePhase.PLAYER_START)
            || nextPhase == BattlePhase.VICTORY
            || nextPhase == BattlePhase.DEFEAT;
    if (deferringEnemyTurn && enemyTurnOver) {
      flushDeferredReveals();
    }
  }

  /** Fires now if the enemy isn't mid-turn, otherwise queues for flushDeferredReveals(). */
  private void dispatch(Runnable reveal) {
    if (deferringEnemyTurn) {
      queuedReveals.add(reveal);
    } else {
      reveal.run();
    }
  }

  /**
   * The single point the "enemy thinks" pause lives: replays everything queued during the enemy's
   * turn together, after one delay.
   */
  private void flushDeferredReveals() {
    deferringEnemyTurn = false;
    List<Runnable> reveals = new ArrayList<>(queuedReveals);
    queuedReveals.clear();
    Timer.schedule(
        new Timer.Task() {
          @Override
          public void run() {
            reveals.forEach(Runnable::run);
          }
        },
        ENEMY_TURN_DELAY);
  }

  private void onEnemyEffects(List<ResolvedCardEffect> effects) {
    entity.getEvents().trigger(ENEMY_EFFECTS_EVENT, effects);
  }

  private void onPlayerEffects(List<ResolvedCardEffect> effects) {
    entity.getEvents().trigger(PLAYER_EFFECTS_EVENT, effects);
  }

  /**
   * The battle finished. Announce it, then switch to the matching end screen. The screen swap is
   * deferred to the next frame so it does not run while we are still inside the battle's own
   * input/update cascade.
   */
  private void onBattleEnd(Boolean won) {
    boolean win = Boolean.TRUE.equals(won);
    entity.getEvents().trigger(win ? BATTLE_WON_EVENT : BATTLE_LOST_EVENT);

    // Report the result to the run so the map node is marked done (win) or the run stays put
    // (loss). The end screen reads this to decide whether to go back to the map or the menu.
    RunState runState = game.getRunState();
    if (runState != null) {
      boolean hadActiveEncounter = runState.getActiveNodeId() != null;
      if (win && isEligibleEliteVictory(runState)) {
        runState.setPendingEliteTempleReward(true);
      }
      runState.completeEncounter(win);
      if (win && hadActiveEncounter) {
        game.requestAutosaveAfterEncounter();
      }
    }

    // queue the release animations if the win conditions have been met.
    if (win) {
      releaseAnimations.clear();

      for (Entity e : enemies) {
        EnemyReleaseComponent release = e.getComponent(EnemyReleaseComponent.class);

        if (release != null) {
          release.startRelease();
          releaseAnimations.add(release);
        }
      }

      if (!releaseAnimations.isEmpty()) {
        awaitingRelease = true;
        return;
      }
    }

    openResultScreen(win);
  }

  private void triggerEndTurn() {
    controller.endPlayerTurn();
  }

  public void update() {
    if (!awaitingRelease) {
      return;
    }

    boolean finishedRelease =
        releaseAnimations.stream().allMatch(EnemyReleaseComponent::isFinished);

    if (finishedRelease) {
      awaitingRelease = false;
      openResultScreen(true);
    }
  }

  private void openResultScreen(boolean won) {
    GdxGame.ScreenType target = won ? GdxGame.ScreenType.VICTORY : GdxGame.ScreenType.DEFEAT;

    if (Gdx.app != null) {
      Gdx.app.postRunnable(
          () -> {
            if (!released) {
              game.setScreen(target);
            }
          });
    } else if (!released) {
      game.setScreen(target);
    }
  }

  private boolean isEligibleEliteVictory(RunState runState) {
    Integer activeNodeId = runState.getActiveNodeId();

    if (activeNodeId == null || runState.getMapGraph() == null) {
      return false;
    }

    MapNode activeNode = runState.getMapGraph().getNode(activeNodeId);

    if (activeNode == null || activeNode.getRoomType() != RoomType.ELITE) {
      return false;
    }

    int currentHealth = runState.getPlayerHealth();
    int maxHealth = runState.getPlayerMaxHealth();

    if (maxHealth <= 0) {
      return false;
    }

    float healthRatio = (float) currentHealth / maxHealth;

    return healthRatio >= 0.8f;
  }

  @Override
  public void dispose() {
    released = true;
    awaitingRelease = false;
    releaseAnimations.clear();
    super.dispose();
  }

  private void onStart() {
    game.setScreen(GdxGame.ScreenType.BATTLE_SCREEN);
  }
}
