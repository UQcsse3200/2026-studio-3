package com.csse3200.game.tutorial;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import java.util.Objects;

/** Small usable text fallback until the battle guidance owner supplies positioned highlights. */
public final class BattleTutorialTextView implements BattleTutorialView {
  private final Stage stage;
  private final Skin skin;
  private Table panel;
  private Label message;
  private TextButton continueButton;
  private Runnable continueAction;
  private Runnable exitAction;

  public BattleTutorialTextView(Stage stage, Skin skin) {
    this.stage = Objects.requireNonNull(stage, "stage cannot be null");
    this.skin = Objects.requireNonNull(skin, "skin cannot be null");
  }

  @Override
  public void bindActions(Runnable continueAction, Runnable exitAction) {
    this.continueAction = Objects.requireNonNull(continueAction, "continueAction cannot be null");
    this.exitAction = Objects.requireNonNull(exitAction, "exitAction cannot be null");
    if (panel != null) {
      return;
    }
    panel = new Table();
    panel.setName("tutorial-prompt");
    panel.setFillParent(true);
    panel.top().padTop(12f);
    panel.setTouchable(Touchable.childrenOnly);
    message = new Label("", skin);
    message.setWrap(true);
    panel.add(message).width(620f).padRight(16f);
    continueButton = new TextButton("Continue", skin);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            BattleTutorialTextView.this.continueAction.run();
          }
        });
    panel.add(continueButton).width(130f).height(45f);
    TextButton exitButton = new TextButton("Exit Tutorial", skin);
    exitButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            BattleTutorialTextView.this.exitAction.run();
          }
        });
    panel.add(exitButton).width(170f).height(45f).padLeft(12f);
    stage.addActor(panel);
  }

  @Override
  public void show(BattleTutorialPrompt prompt) {
    if (panel == null) {
      throw new IllegalStateException("Tutorial actions must be bound before showing a prompt");
    }
    message.setText(prompt.text());
    continueButton.setVisible(prompt.canContinue());
  }

  @Override
  public void clear() {
    if (panel != null) {
      panel.remove();
      panel = null;
    }
    continueAction = null;
    exitAction = null;
  }
}
