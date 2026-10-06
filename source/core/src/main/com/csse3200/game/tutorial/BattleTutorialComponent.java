package com.csse3200.game.tutorial;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import java.util.Objects;
import java.util.function.Consumer;

/** Attaches guidance to an isolated tutorial battle; its owner supplies UI and the exit flow. */
public final class BattleTutorialComponent extends Component {
  private final BattleController battle;
  private final BattleTutorialView view;
  private final Consumer<BattleTutorialController.Outcome> onFinished;
  private final BattleTutorialController tutorial = new BattleTutorialController();
  private BattleTutorialController.Outcome pendingOutcome;
  private boolean created;
  private boolean completionQueued;
  private boolean disposed;

  public BattleTutorialComponent(
      BattleController battle,
      BattleTutorialView view,
      Consumer<BattleTutorialController.Outcome> onFinished) {
    this.battle = Objects.requireNonNull(battle, "battle cannot be null");
    this.view = Objects.requireNonNull(view, "view cannot be null");
    this.onFinished = Objects.requireNonNull(onFinished, "onFinished cannot be null");
  }

  /** Register this component before starting the battle so it observes the first player action. */
  @Override
  public void create() {
    if (created || disposed) {
      throw new IllegalStateException(
          "Tutorial component cannot be created twice or after disposal");
    }
    created = true;
    tutorial.addStepListener(step -> view.show(BattleTutorialPrompt.forStep(step)));
    tutorial.addOutcomeListener(outcome -> pendingOutcome = outcome);
    tutorial.bindTo(battle);
    view.bindActions(() -> tutorial.continueInformation(), tutorial::cancel);
    tutorial.start();
  }

  /** Completes outside the battle event dispatch and defers navigation to the next frame. */
  @Override
  public void update() {
    if (disposed || completionQueued || pendingOutcome == null) {
      return;
    }
    completionQueued = true;
    Runnable finish =
        () -> {
          if (disposed) {
            return;
          }
          BattleTutorialController.Outcome outcome = pendingOutcome;
          dispose();
          onFinished.accept(outcome);
        };
    if (Gdx.app == null) {
      finish.run();
    } else {
      Gdx.app.postRunnable(finish);
    }
  }

  /** Screen disposal is silent: it must not launch a new run after the player left the screen. */
  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    tutorial.close();
    pendingOutcome = null;
    view.clear();
  }
}
