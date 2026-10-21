package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.ui.PixelButtonStyles;
import com.csse3200.game.ui.UIComponent;

/** A tutorial victory acknowledgement before the player starts a fresh normal run. */
public class TutorialVictoryScreen extends ScreenAdapter {
  private static final String[] TEXTURES = {
    "images/dungeon.png",
    "images/ui/reward-panel.png",
    "images/ui/victory-title.png",
    "images/ui/tutorial-continue-button.png"
  };

  private final GdxGame game;
  private final Renderer renderer;
  private final Cell<Stack> panelCell;
  private final TextButton continueButton;
  private boolean continueRequested;
  private boolean continuing;
  private boolean disposed;

  public TutorialVictoryScreen(GdxGame game) {
    this.game = game;
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.getResourceService().loadTextures(TEXTURES);
    ServiceLocator.getResourceService().loadAll();
    renderer = RenderFactory.createRenderer();
    Stage stage = renderer.getStage();
    Skin skin = UIComponent.getSharedSkin();

    Image background = image(TEXTURES[0]);
    background.setScaling(Scaling.fill);
    background.setFillParent(true);
    stage.addActor(background);
    Image dimmer = new Image(skin.newDrawable("white", new Color(0.015f, 0.01f, 0.025f, 0.72f)));
    dimmer.setFillParent(true);
    stage.addActor(dimmer);

    Stack panel = new Stack();
    panel.setName("tutorial-victory-panel");
    Image frame = image(TEXTURES[1]);
    frame.setScaling(Scaling.stretch);
    panel.add(frame);
    Table content = new Table();
    content.pad(75f, 110f, 65f, 110f);
    Image title = image(TEXTURES[2]);
    title.setScaling(Scaling.fit);
    content.add(title).size(340f, 72f).padBottom(12f).row();

    Label.LabelStyle headingStyle = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    headingStyle.fontColor = Color.valueOf("F5C761");
    Label heading = new Label("Tutorial Complete", headingStyle);
    heading.setName("tutorial-victory-heading");
    content.add(heading).padBottom(24f).row();

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    bodyStyle.fontColor = Color.valueOf("E8D6AE");
    Label message =
        new Label("You've completed the tutorial.\nYour journey is ready to begin.", bodyStyle);
    message.setAlignment(Align.center);
    message.setWrap(true);
    message.setFontScale(0.85f);
    content.add(message).growX().padBottom(20f).row();

    Label.LabelStyle noteStyle = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    noteStyle.fontColor = Color.valueOf("C4B59F");
    Label note = new Label("Continue to start a fresh adventure.", noteStyle);
    note.setAlignment(Align.center);
    note.setWrap(true);
    content.add(note).growX().padBottom(14f).row();

    continueButton =
        new TextButton(
            "Continue",
            PixelButtonStyles.create(
                skin, ServiceLocator.getResourceService().getAsset(TEXTURES[3], Texture.class))) {
          @Override
          public void draw(Batch batch, float parentAlpha) {
            // Match the frame scaling supplied by PixelButtonStyles.
            float scale = 1f;
            if (!isDisabled()) {
              if (isPressed()) {
                scale = 0.9375f;
              } else if (isOver()) {
                scale = 1.125f;
              }
            }
            if (getLabel().getFontScaleX() != scale) {
              getLabel().setFontScale(scale);
            }
            super.draw(batch, parentAlpha);
          }
        };
    continueButton.setName("tutorial-victory-continue");
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!disposed && !continueRequested) {
              continueRequested = true;
              continueButton.setDisabled(true);
              AudioService.playSound(SoundId.BUTTON_CLICK, 0.5f);
            }
          }
        });
    content.add(continueButton).size(300f, 112f);
    panel.add(content);
    Table root = new Table();
    root.setFillParent(true);
    panelCell = root.add(panel);
    stage.addActor(root);
    resizePanel();
    ServiceLocator.getEntityService()
        .register(new Entity().addComponent(new InputDecorator(stage, 10)));
  }

  private static Image image(String path) {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return new Image(texture);
  }

  private void resizePanel() {
    Stage stage = renderer.getStage();
    float width =
        Math.min(880f, Math.min(stage.getWidth() * 0.9f, stage.getHeight() * 0.9f / 0.625f));
    panelCell.size(width, width * 0.625f);
  }

  @Override
  public void render(float delta) {
    if (disposed || continuing) {
      return;
    }
    ServiceLocator.getEntityService().update();
    renderer.render();
    // Navigation may dispose this screen, so wait until Stage input/actions have finished.
    if (continueRequested && !disposed) {
      continuing = true;
      game.startRunAfterTutorial();
    }
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    resizePanel();
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(TEXTURES);
  }
}
