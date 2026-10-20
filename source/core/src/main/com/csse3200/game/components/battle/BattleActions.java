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
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
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

  private static final float POST_ENEMY_TURN_DELAY = 3f;
  private float postEnemyTurnTime;
  private Runnable pendingPlayerTurnReveal;

  public static final String ENEMY_TURN_EVENT = "enemyTurn";

  private final BattleController controller;
  private final GdxGame game;
  private final boolean tutorialBattle;
  private final List<Entity> enemies;

  // Queue enemy log, effects and phases for the existing "enemy thinks" pause. After these
  // reveals play together, pendingPlayerTurnReveal holds back the hand and player-turn message
  // for the separate pause after the enemy turn.
  private boolean deferringEnemyTurn = false;
  private int pendingRevealBatches;

  /** Whether enemy reveals or the following pause are still holding back player controls. */
  public boolean hasPendingEnemyReveals() {
    return deferringEnemyTurn || pendingRevealBatches > 0 || pendingPlayerTurnReveal != null;
  }

  private final List<Runnable> queuedReveals = new ArrayList<>();
  private final List<EnemyReleaseComponent> releaseAnimations = new ArrayList<>();
  private boolean awaitingRelease;
  private boolean released;

  public BattleActions(BattleController controller, GdxGame game) {
    this(controller, game, List.of(), false);
  }

  public BattleActions(BattleController controller, GdxGame game, boolean tutorialBattle) {
    this(controller, game, List.of(), tutorialBattle);
  }

  public BattleActions(BattleController controller, GdxGame game, List<Entity> enemies) {
    this(controller, game, enemies, false);
  }

  public BattleActions(
      BattleController controller, GdxGame game, List<Entity> enemies, boolean tutorialBattle) {
    this.controller = controller;
    this.game = game;
    this.enemies = List.copyOf(enemies);
    this.tutorialBattle = tutorialBattle;
  }

  /**
   * Returns whether the battle is waiting for player input.
   *
   * @return true only during the player turn, excluding card resolution and all other phases
   */
  public boolean isPlayerTurn() {
    return !released && !hasPendingEnemyReveals() && controller.isPlayerTurn();
  }

  @Override
  public void create() {
    entity.getEvents().addListener("battle", this::onStart);

    entity.getEvents().addListener(END_TURN_SELECTED_EVENT, this::triggerEndTurn);
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
    if (!tutorialBattle) {
      controller.addBattleEndListener(this::onBattleEnd);
    }
    controller.addHandChangedListener(hand -> entity.getEvents().trigger(HAND_CHANGED_EVENT, hand));
  }

  private void onPhaseChange(BattlePhase previousPhase, BattlePhase nextPhase) {
    if (nextPhase == BattlePhase.SETUP
        || nextPhase == BattlePhase.VICTORY
        || nextPhase == BattlePhase.DEFEAT) {
      pendingPlayerTurnReveal = null;
      postEnemyTurnTime = 0f;
    }
    // Entering the enemy's turn fires immediately — no delay on ending your own turn — and starts
    // holding back every reveal that follows until the whole enemy turn is done.
    if (nextPhase == BattlePhase.ENEMY_TURN && previousPhase == BattlePhase.PLAYER_END) {
      deferringEnemyTurn = true;
      entity.getEvents().trigger(ENEMY_TURN_EVENT);
      entity.getEvents().trigger(PHASE_CHANGED_EVENT, nextPhase);
      return;
    }

    if (deferringEnemyTurn
        && nextPhase == BattlePhase.PLAYER_TURN
        && previousPhase == BattlePhase.PLAYER_START) {
      int turnNumber = controller.getPlayerTurnNumber();
      dispatch(() -> beginPlayerTurnDelay(turnNumber));
      flushDeferredReveals();
      return;
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
    pendingRevealBatches++;
    Timer.schedule(
        new Timer.Task() {
          @Override
          public void run() {
            try {
              if (!released) {
                reveals.forEach(Runnable::run);
              }
            } finally {
              pendingRevealBatches--;
            }
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
    recordRunOutcome(win);

    // queue the release animations if the win conditions have been met.
    if (win && startReleaseAnimations()) {
      awaitingRelease = true;
      return;
    }

    openResultScreen(win);
  }

  private void recordRunOutcome(boolean win) {
    // Report the result to the run so the map node is marked done (win) or the run stays put
    // (loss). The end screen reads this to decide whether to go back to the map or the menu.
    RunState runState = game.getRunState();
    if (runState == null) {
      return;
    }
    boolean hadActiveEncounter = runState.getActiveNodeId() != null;
    if (win && isEligibleEliteVictory(runState)) {
      runState.setPendingEliteTempleReward(true);
    }
    runState.completeEncounter(win);
    if (win && hadActiveEncounter) {
      game.requestAutosaveAfterEncounter();
    }
  }

  private boolean startReleaseAnimations() {
    releaseAnimations.clear();
    for (Entity e : enemies) {
      EnemyReleaseComponent release = e.getComponent(EnemyReleaseComponent.class);
      if (release != null) {
        release.startRelease();
        releaseAnimations.add(release);
      }
    }
    return !releaseAnimations.isEmpty();
  }

  private void triggerEndTurn() {
    if (!released && !hasPendingEnemyReveals()) {
      controller.endPlayerTurn();
    }
  }

  @Override
  public void update() {
    updatePlayerTurnReturn();
    updateAutoEndTurn();
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

  /** Starts the pause after enemy effects have been shown, before returning the hand. */
  private void beginPlayerTurnDelay(int turnNumber) {
    if (released || !controller.isPlayerTurn()) {
      return;
    }
    postEnemyTurnTime = 0f;
    pendingPlayerTurnReveal =
        () -> {
          entity.getEvents().trigger("playerTurnStarted", turnNumber);
          entity.getEvents().trigger(PHASE_CHANGED_EVENT, BattlePhase.PLAYER_TURN);
        };
  }

  private void updatePlayerTurnReturn() {
    if (released || pendingPlayerTurnReveal == null) {
      return;
    }
    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return;
    }
    postEnemyTurnTime += Math.max(0f, time.getDeltaTime());
    if (postEnemyTurnTime >= POST_ENEMY_TURN_DELAY) {
      Runnable reveal = pendingPlayerTurnReveal;
      pendingPlayerTurnReveal = null;
      postEnemyTurnTime = 0f;
      reveal.run();
    }
  }

  /** End an empty player turn on the next update, after its last card has resolved. */
  private void updateAutoEndTurn() {
    if (isPlayerTurn() && controller.isPlayerEnergyDepleted()) {
      controller.endPlayerTurn();
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
    pendingPlayerTurnReveal = null;
    postEnemyTurnTime = 0f;
    awaitingRelease = false;
    releaseAnimations.clear();
    super.dispose();
  }

  private void onStart() {
    game.setScreen(
        tutorialBattle ? GdxGame.ScreenType.TUTORIAL_BATTLE : GdxGame.ScreenType.BATTLE_SCREEN);
  }
}
