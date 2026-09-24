package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.cards.CardUpgradeDisplay;
import com.csse3200.game.components.cards.CardUpgradeSelection;
import com.csse3200.game.components.cards.PlayerDeckCardUpgradeCommitter;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Campfire encounter with a short rest scene and the existing card-upgrade flow. */
public class CampfireScreen extends ScreenAdapter {
  private static final float REST_HEAL_PERCENT = 0.30f;
  private static final float UI_WIDTH = 1280f;
  private static final float UI_HEIGHT = 800f;
  private static final float FADE_SECONDS = 0.18f;
  private static final float REST_SCENE_SECONDS = 0.9f;
  private static final String MAIN_SCENE = "images/campfire/main_scene_dusk.png";
  private static final String MAIN_SCENE_EXTENDED = "images/campfire/main_scene_extended.png";
  private static final String REST_SCENE = "images/campfire/rest_scene_wide.png";
  private static final String UPGRADE_SCENE = "images/campfire/upgrade_table_wide.png";
  private static final String REST_ICON = "images/campfire/rest_icon.png";
  private static final String UPGRADE_ICON = "images/campfire/upgrade_icon.png";
  private static final String[] TEXTURES = {
    MAIN_SCENE, MAIN_SCENE_EXTENDED, REST_SCENE, UPGRADE_SCENE, REST_ICON, UPGRADE_ICON
  };

  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;
  private Group activeScene;
  private Group mainScene;
  private Group restScene;
  private Group upgradeScene;
  private Image fadeOverlay;
  private Label restResult;
  private Table restOutcome;
  private boolean transitioning;

  public CampfireScreen(GdxGame game) {
    this(game, game.getRunState());
  }

  /** Allows a standalone preview to supply its own isolated run state. */
  protected CampfireScreen(GdxGame game, RunState runState) {
    this.game = game;
    this.runState = runState;
    if (runState == null) {
      throw new IllegalStateException("CampfireScreen opened without a RunState");
    }

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    renderer = RenderFactory.createRenderer();
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(TEXTURES);
    resources.loadAll();
    createUI();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity inputEntity = new Entity();
    inputEntity.addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(inputEntity);

    CardService cardLibrary = new CardLibrary(CardConfigLoader.loadCards());
    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardLibrary);
    CardUpgradeSelection upgradeSelection =
        CardUpgradeSelection.forPlayerDeck(playerDeck, cardLibrary, 1);
    CardUpgradeDisplay upgradeDisplay =
        new CardUpgradeDisplay(
            upgradeSelection,
            new PlayerDeckCardUpgradeCommitter(playerDeck),
            false,
            this::finishCampfire);
    ServiceLocator.getEntityService().register(new Entity().addComponent(upgradeDisplay));

    ResourceService resources = ServiceLocator.getResourceService();
    mainScene = newScene();
    Texture mainTexture = resources.getAsset(MAIN_SCENE, Texture.class);
    Texture extendedTexture = resources.getAsset(MAIN_SCENE_EXTENDED, Texture.class);
    // Preserve the exact original artwork in the centre. The taller version only fills
    // the missing top/bottom space and blends into the original near those edges.
    addSceneImage(mainScene, extendedTexture, true);
    Image mainBackground = addSceneImage(mainScene, mainTexture, false);
    blendSceneEdges(mainScene, extendedTexture, mainBackground);
    addDiamond(
        mainScene,
        mainBackground,
        resources.getAsset(REST_ICON, Texture.class),
        515f,
        480f,
        this::beginRest);
    addDiamond(
        mainScene,
        mainBackground,
        resources.getAsset(UPGRADE_ICON, Texture.class),
        1435f,
        445f,
        this::beginUpgrade);

    restScene = newScene();
    addSceneImage(restScene, resources.getAsset(REST_SCENE, Texture.class), true);
    createRestOutcome();
    restScene.setVisible(false);

    upgradeScene = newScene();
    Image tableBackground =
        addSceneImage(upgradeScene, resources.getAsset(UPGRADE_SCENE, Texture.class), true);
    addUpgradeTableHotspot(tableBackground, upgradeDisplay, upgradeSelection);
    upgradeScene.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            if (keycode != Input.Keys.ESCAPE || transitioning) {
              return false;
            }
            upgradeDisplay.hideLibrary();
            transitioning = true;
            fadeTo(
                mainScene,
                () -> {
                  stage.setKeyboardFocus(null);
                  transitioning = false;
                });
            return true;
          }
        });
    upgradeScene.setVisible(false);

    stage.addActor(mainScene);
    stage.addActor(restScene);
    stage.addActor(upgradeScene);
    activeScene = mainScene;
    fadeOverlay = new Image(skin.newDrawable("white", Color.BLACK));
    fadeOverlay.setBounds(0f, 0f, UI_WIDTH, UI_HEIGHT);
    fadeOverlay.getColor().a = 1f;
    fadeOverlay.setTouchable(Touchable.disabled);
    stage.addActor(fadeOverlay);
    transitioning = true;
    fadeOverlay.addAction(
        Actions.sequence(Actions.fadeOut(0.28f), Actions.run(() -> transitioning = false)));
  }

  private Group newScene() {
    Group scene = new Group();
    scene.setSize(UI_WIDTH, UI_HEIGHT);
    return scene;
  }

  /** Scales scene art without stretching; the wide scene assets fill the game viewport. */
  private Image addSceneImage(Group scene, Texture texture, boolean fill) {
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    float xScale = UI_WIDTH / texture.getWidth();
    float yScale = UI_HEIGHT / texture.getHeight();
    float scale = fill ? Math.max(xScale, yScale) : Math.min(xScale, yScale);
    Image image = new Image(texture);
    image.setSize(texture.getWidth() * scale, texture.getHeight() * scale);
    image.setPosition((UI_WIDTH - image.getWidth()) / 2f, (UI_HEIGHT - image.getHeight()) / 2f);
    image.setTouchable(Touchable.disabled);
    scene.addActor(image);
    return image;
  }

  private void blendSceneEdges(Group scene, Texture extension, Image fittedScene) {
    float edgeHeight = 60f;
    int stripCount = 24;
    float stripHeight = edgeHeight / stripCount;
    float inset = fittedScene.getY();
    for (int strip = 0; strip < stripCount; strip++) {
      float progress = strip / (float) (stripCount - 1);
      float alpha = 1f - Interpolation.smooth.apply(progress);
      addExtensionStrip(scene, extension, inset + strip * stripHeight, stripHeight, alpha);
      addExtensionStrip(
          scene, extension, UI_HEIGHT - inset - (strip + 1) * stripHeight, stripHeight, alpha);
    }
  }

  private void addExtensionStrip(Group scene, Texture texture, float y, float height, float alpha) {
    int sourceTop = Math.round((UI_HEIGHT - y - height) * texture.getHeight() / UI_HEIGHT);
    int sourceBottom = Math.round((UI_HEIGHT - y) * texture.getHeight() / UI_HEIGHT);
    sourceTop = Math.max(0, Math.min(texture.getHeight() - 1, sourceTop));
    sourceBottom = Math.max(sourceTop + 1, Math.min(texture.getHeight(), sourceBottom));
    Image strip =
        new Image(
            new TextureRegion(texture, 0, sourceTop, texture.getWidth(), sourceBottom - sourceTop));
    strip.setBounds(0f, y, UI_WIDTH, height);
    strip.setColor(1f, 1f, 1f, alpha);
    strip.setTouchable(Touchable.disabled);
    scene.addActor(strip);
  }

  private void addDiamond(
      Group scene,
      Image background,
      Texture iconTexture,
      float imageX,
      float imageY,
      Runnable onClick) {
    iconTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    // Coordinates come from the supplied 1672 x 941 main-scene reference.
    float scale = background.getWidth() / 1672f;
    float centerX = background.getX() + imageX * scale;
    float centerY = background.getY() + (941f - imageY) * scale;
    Image icon = new Image(iconTexture);
    icon.setSize(200f, 200f);
    icon.setPosition(centerX - 100f, centerY - 100f);
    icon.setTouchable(Touchable.disabled);
    scene.addActor(icon);

    Actor hotspot = new Actor();
    hotspot.setBounds(centerX - 68f, centerY - 70f, 136f, 140f);
    float restingY = icon.getY();
    hotspot.addListener(
        new ClickListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer != -1 || transitioning) {
              return;
            }
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            icon.clearActions();
            icon.addAction(
                Actions.forever(
                    Actions.sequence(
                        Actions.moveTo(icon.getX(), restingY + 4f, 0.9f, Interpolation.sine),
                        Actions.moveTo(icon.getX(), restingY - 4f, 1.8f, Interpolation.sine),
                        Actions.moveTo(icon.getX(), restingY, 0.9f, Interpolation.sine))));
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (pointer != -1) {
              return;
            }
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            icon.clearActions();
            icon.setY(restingY);
          }

          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (!transitioning) {
              onClick.run();
            }
          }
        });
    scene.addActor(hotspot);
  }

  private void addUpgradeTableHotspot(
      Image background, CardUpgradeDisplay upgradeDisplay, CardUpgradeSelection selection) {
    float scale = background.getHeight() / 992f;
    Actor hotspot = new Actor();
    // The Upgrade button is painted into the wide table scene, near its lower-right corner.
    hotspot.setBounds(
        background.getX() + 1005f * scale,
        background.getY() + 117f * scale,
        250f * scale,
        90f * scale);
    hotspot.addListener(
        new ClickListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer == -1 && !selection.getCardUpgradeOption().isEmpty()) {
              Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (pointer == -1) {
              Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }
          }

          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (!transitioning) {
              upgradeDisplay.showLibrary();
            }
          }
        });
    upgradeScene.addActor(hotspot);
  }

  private void createRestOutcome() {
    restOutcome = new Table();
    restOutcome.setFillParent(true);
    restOutcome.setBackground(skin.newDrawable("white", new Color(0.05f, 0.03f, 0.04f, 0.72f)));
    restResult = new Label("", skin, "large");
    restOutcome.add(restResult).padBottom(22f);
    restOutcome.row();
    TextButton continueButton = new TextButton("Continue", skin);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!transitioning) {
              finishCampfire();
            }
          }
        });
    restOutcome.add(continueButton).width(220f).height(60f);
    restOutcome.setVisible(false);
    restScene.addActor(restOutcome);
  }

  private void beginRest() {
    transitioning = true;
    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    fadeTo(
        restScene,
        () ->
            restScene.addAction(
                Actions.sequence(
                    Actions.delay(REST_SCENE_SECONDS),
                    Actions.run(
                        () ->
                            fadeTo(
                                restScene,
                                () -> {
                                  int restored = applyRest();
                                  restResult.setText(
                                      restored > 0
                                          ? "Restored " + restored + " HP"
                                          : "Health is already full.");
                                  restOutcome.setVisible(true);
                                },
                                () -> transitioning = false)))));
  }

  private void beginUpgrade() {
    transitioning = true;
    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    fadeTo(
        upgradeScene,
        () -> {
          ServiceLocator.getRenderService().getStage().setKeyboardFocus(upgradeScene);
          transitioning = false;
        });
  }

  private void fadeTo(Group nextScene, Runnable afterReveal) {
    fadeTo(nextScene, () -> {}, afterReveal);
  }

  private void fadeTo(Group nextScene, Runnable atBlack, Runnable afterReveal) {
    fadeOverlay.clearActions();
    fadeOverlay.toFront();
    fadeOverlay.addAction(
        Actions.sequence(
            Actions.fadeIn(FADE_SECONDS),
            Actions.run(
                () -> {
                  atBlack.run();
                  activeScene.setVisible(false);
                  nextScene.setVisible(true);
                  activeScene = nextScene;
                }),
            Actions.fadeOut(FADE_SECONDS),
            Actions.run(afterReveal)));
  }

  private int applyRest() {
    PlayerRunState playerState = runState.getOrCreatePlayerState();
    int oldHealth = playerState.getCurrentHealth();
    int healAmount = Math.max(1, Math.round(playerState.getMaxHealth() * REST_HEAL_PERCENT));
    int newHealth = Math.min(playerState.getMaxHealth(), oldHealth + healAmount);
    playerState.restore(newHealth, playerState.getMaxHealth(), playerState.getGold());
    // Keep legacy RunState health values in sync.
    runState.setPlayerHealth(newHealth);
    runState.setPlayerMaxHealth(playerState.getMaxHealth());
    return newHealth - oldHealth;
  }

  protected void finishCampfire() {
    runState.completeEncounter(true);
    game.setScreen(GdxGame.ScreenType.MAP);
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.05f, 0.03f, 0.04f, 1f);
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    skin.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
