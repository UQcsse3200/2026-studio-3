package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Temporary screen shown after an eligible Elite victory.
 *
 * <p>The final version will display an animated portal leading to the Ancient Temple encounter.
 */
public class ElitePortalScreen extends ScreenAdapter {
  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;

  public ElitePortalScreen(GdxGame game) {
    this.game = game;
    this.runState = game.getRunState();

    if (runState == null || !runState.hasPendingEliteTempleReward()) {
      throw new IllegalStateException("ElitePortalScreen opened without a pending Elite reward");
    }

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));

    createUI();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    Entity inputEntity = new Entity();
    inputEntity.addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(inputEntity);

    Table root = new Table();
    root.setFillParent(true);

    Label title = new Label("A Mysterious Portal Appears...", skin, "title");

    Label.LabelStyle descriptionStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    descriptionStyle.fontColor = new Color(0.9f, 0.8f, 0.65f, 1f);

    Label description =
        new Label(
            "Your victory has awakened something hidden beyond the battlefield.", descriptionStyle);
    TextButton portalButton = new TextButton("Enter Portal", skin);

    portalButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            enterPortal();
          }
        });

    root.add(title).padBottom(40f);
    root.row();

    root.add(description).padBottom(50f);
    root.row();

    root.add(portalButton).width(320f).height(90f);

    stage.addActor(root);
  }

  private void enterPortal() {
    game.setScreen(GdxGame.ScreenType.ANCIENT_TEMPLE);
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.05f, 0.03f, 0.08f, 1f);
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    skin.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
