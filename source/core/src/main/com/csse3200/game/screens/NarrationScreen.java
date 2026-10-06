package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.BufferUtils;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.narration.NarrationConfigLoader;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Objects;

/** Full-screen passages whose caller owns the destination after completion or skipping. */
public class NarrationScreen extends ScreenAdapter {
  static final float FADE_SECONDS = 0.6f;
  static final float HOLD_SECONDS = 2.5f;
  private static final float PASSAGE_WIDTH = 1000f;

  private final Renderer renderer;
  private final Skin skin;
  private final Runnable onComplete;
  private final Label passageLabel;
  private final InputListener skipListener;
  private boolean finishRequested;
  private boolean skipping;
  private boolean completed;
  private boolean disposed;
  private final FloatBuffer previousClearColour = BufferUtils.newFloatBuffer(4);

  /** Unknown IDs complete on the first frame. File errors propagate as loading exceptions. */
  public NarrationScreen(String sequenceId, Runnable onComplete) {
    this.onComplete = Objects.requireNonNull(onComplete, "Completion callback is required");
    // Validate before replacing any services.
    List<List<String>> passages = NarrationConfigLoader.loadSequence(sequenceId);
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    renderer = RenderFactory.createRenderer();
    skin = UIComponent.getSharedSkin();
    Stage stage = ServiceLocator.getRenderService().getStage();

    Table root = new Table();
    root.setFillParent(true);
    root.setBackground(skin.newDrawable("white", Color.BLACK));
    Label.LabelStyle passageStyle = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    passageStyle.fontColor = MenuTheme.warmParchment();
    passageLabel = new Label("", passageStyle);
    passageLabel.setAlignment(Align.center);
    passageLabel.setWrap(true);
    root.add(passageLabel).width(PASSAGE_WIDTH).expand();
    stage.addActor(root);

    Table hintTable = new Table();
    hintTable.setFillParent(true);
    hintTable.bottom().right().pad(MenuTheme.SCREEN_PADDING);
    Label.LabelStyle hintStyle = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    hintStyle.fontColor = MenuTheme.dustyMauve();
    Label hint = new Label("Click to skip", hintStyle);
    hintTable.add(hint);
    stage.addActor(hintTable);
    skipListener =
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            skip();
            return true;
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            skip();
            return true;
          }
        };
    stage.addCaptureListener(skipListener);
    ServiceLocator.getEntityService()
        .register(new Entity().addComponent(new InputDecorator(stage, 10)));

    passageLabel.getColor().a = 0f;
    SequenceAction sequence = Actions.sequence();
    for (List<String> lines : passages) {
      sequence.addAction(Actions.run(() -> passageLabel.setText(String.join("\n", lines))));
      sequence.addAction(Actions.fadeIn(FADE_SECONDS));
      sequence.addAction(Actions.delay(HOLD_SECONDS));
      sequence.addAction(Actions.fadeOut(FADE_SECONDS));
    }
    sequence.addAction(Actions.run(() -> finishRequested = true));
    passageLabel.addAction(sequence);
    finishRequested = passages.isEmpty();
  }

  private void skip() {
    if (disposed || completed || finishRequested || skipping) {
      return;
    }
    skipping = true;
    passageLabel.clearActions();
    passageLabel.addAction(
        Actions.sequence(Actions.fadeOut(FADE_SECONDS), Actions.run(() -> finishRequested = true)));
  }

  @Override
  public void render(float delta) {
    if (disposed || completed) {
      return;
    }
    // Clear the entire framebuffer, including viewport bars, to the requested black.
    previousClearColour.clear();
    Gdx.gl.glGetFloatv(GL20.GL_COLOR_CLEAR_VALUE, previousClearColour);
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
    ServiceLocator.getEntityService().update();
    renderer.render();
    Gdx.gl.glClearColor(
        previousClearColour.get(0),
        previousClearColour.get(1),
        previousClearColour.get(2),
        previousClearColour.get(3));
    // The callback may dispose this screen: never invoke it inside Stage.act/input dispatch.
    completeIfRequested();
  }

  private void completeIfRequested() {
    if (finishRequested && !disposed && !completed) {
      completed = true;
      onComplete.run();
    }
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    passageLabel.clearActions();
    renderer.getStage().removeCaptureListener(skipListener);
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
  }
}
