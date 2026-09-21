package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
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

  private Texture backgroundTexture;
  private Texture portalTexture;
  private Skin skin;

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

    backgroundTexture = new Texture(Gdx.files.internal("images/battle_background.png"));

    portalTexture = new Texture(Gdx.files.internal("images/elite_portal.png"));

    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    portalTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.stretch);
    background.setBounds(0f, 0f, stage.getWidth(), stage.getHeight());

    Image portal = new Image(portalTexture);
    portal.setScaling(Scaling.fit);

    float portalHeight = stage.getHeight() * 0.68f;
    float portalWidth = portalHeight * 0.75f;

    portal.setSize(portalWidth, portalHeight);
    portal.setOrigin(Align.center);
    portal.setPosition(0f, 0f);

    /*
     * The group owns the permanent idle animation.
     * The portal image itself owns the mouse-hover animation.
     *
     * Keeping them separate means hover effects do not cancel the
     * continuous portal animation.
     */
    Group portalGroup = new Group();
    portalGroup.setSize(portalWidth, portalHeight);
    portalGroup.setOrigin(Align.center);

    portalGroup.setPosition(
        (stage.getWidth() - portalWidth) / 2f, (stage.getHeight() - portalHeight) / 2f);

    portalGroup.addActor(portal);

    /* Continuous portal idle animation. */
    portalGroup.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.parallel(
                    Actions.scaleTo(1.03f, 1.03f, 0.9f, Interpolation.sineOut),
                    Actions.moveBy(0f, 6f, 0.9f, Interpolation.sineOut)),
                Actions.parallel(
                    Actions.scaleTo(1f, 1f, 0.9f, Interpolation.sineIn),
                    Actions.moveBy(0f, -6f, 0.9f, Interpolation.sineIn)))));
    portal.addListener(
        new ClickListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            portal.clearActions();
            portal.addAction(Actions.scaleTo(1.06f, 1.06f, 0.12f, Interpolation.sineOut));
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            portal.clearActions();
            portal.addAction(Actions.scaleTo(1f, 1f, 0.12f, Interpolation.sineOut));
          }

          @Override
          public void clicked(InputEvent event, float x, float y) {
            enterPortal();
          }
        });

    stage.addActor(background);
    stage.addActor(portalGroup);

    showPortalOpenedNotification(stage);
  }

  private void showPortalOpenedNotification(Stage stage) {
    Table notification = new Table();

    notification.setTouchable(Touchable.disabled);

    notification.setBackground(skin.newDrawable("white", new Color(0.08f, 0.03f, 0.12f, 0.90f)));

    Label message = new Label("A mysterious portal has opened!", skin, "large");

    message.setColor(new Color(0.90f, 0.75f, 1f, 1f));

    notification.add(message).pad(14f, 26f, 14f, 26f);

    notification.pack();

    notification.setPosition(
        (stage.getWidth() - notification.getWidth()) / 2f,
        stage.getHeight() - notification.getHeight() - 35f);

    notification.getColor().a = 0f;

    notification.addAction(
        Actions.sequence(
            Actions.fadeIn(0.25f),
            Actions.delay(2.2f),
            Actions.fadeOut(0.5f),
            Actions.removeActor()));

    stage.addActor(notification);
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
    if (backgroundTexture != null) {
      backgroundTexture.dispose();
    }

    if (portalTexture != null) {
      portalTexture.dispose();
    }

    if (skin != null) {
      skin.dispose();
    }

    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
