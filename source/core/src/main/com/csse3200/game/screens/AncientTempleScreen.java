package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Random;

/**
 * Temporary playable version of the hidden Ancient Temple encounter.
 *
 * <p>The player enters from the Elite portal, prays to the statue and selects one blessing before
 * returning to the map.
 */
public class AncientTempleScreen extends ScreenAdapter {
  private static final int ETERNITY_MAX_HEALTH_BONUS = 20;
  private static final int ETERNITY_MAX_ENERGY_BONUS = 1;
  private static final int ETERNITY_FOCUS_HEAL = 20;

  private static final float MESSAGE_FADE_IN_SECONDS = 0.4f;
  private static final float MESSAGE_HOLD_SECONDS = 1.8f;
  private static final float MESSAGE_FADE_OUT_SECONDS = 0.4f;

  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;
  private Texture backgroundTexture;
  private Texture templeButtonTexture;
  private Stack sceneRoot;

  private final Random random = new Random();

  private Table root;
  private boolean rewardClaimed;
  private EternityEffect offeredEternityEffect;

  private enum EternityEffect {
    VITALITY,
    FOCUS
  }

  public AncientTempleScreen(GdxGame game) {
    this.game = game;
    this.runState = game.getRunState();

    if (runState == null || !runState.hasPendingEliteTempleReward()) {
      throw new IllegalStateException(
          "AncientTempleScreen opened without a pending Elite temple reward");
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

    backgroundTexture = new Texture(Gdx.files.internal("images/ancient_temple.png"));

    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    templeButtonTexture =
        new Texture(Gdx.files.internal("images/ancient_temple_choice_button.png"));
    templeButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.stretch);

    root = new Table();

    sceneRoot = new Stack();
    sceneRoot.setFillParent(true);

    sceneRoot.add(background);
    sceneRoot.add(root);

    showTempleEntrance();

    stage.addActor(sceneRoot);
  }

  private void showTempleEntrance() {
    root.clearChildren();

    root.bottom();
    root.padBottom(45f);

    Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("title", Label.LabelStyle.class));
    titleStyle.fontColor = Color.valueOf("F1C879");

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = Color.valueOf("E3D3B8");

    Label title = new Label("Ancient Temple", titleStyle);

    Label description = new Label("An ancient presence watches in silence.", bodyStyle);
    showTransientMessage(title);
    showTransientMessage(description);

    TextButton prayButton = new TextButton("Pray", createTempleButtonStyle());
    prayButton.getLabel().setFontScale(1.15f);

    prayButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (prayButton.isDisabled()) {
              return;
            }

            prayButton.setDisabled(true);
            playPrayerShake();
          }
        });

    root.add(title).padBottom(8f);
    root.row();

    root.add(description).padBottom(18f);
    root.row();

    root.add(prayButton).width(460f).height(100f);
  }

  private TextButtonStyle createTempleButtonStyle() {
    TextureRegionDrawable normal =
        new TextureRegionDrawable(new TextureRegion(templeButtonTexture));

    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));

    // Normal
    style.up = normal;

    // Hover: warmer golden-red tone
    style.over = normal.tint(Color.valueOf("E6A85C"));

    // Pressed: dark red-brown
    style.down = normal.tint(Color.valueOf("8C493D"));

    style.fontColor = Color.valueOf("F6E8C8");
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.valueOf("F1C879");

    return style;
  }

  private TextButtonStyle createWarBlessingButtonStyle() {
    TextButtonStyle style = new TextButtonStyle(createTempleButtonStyle());
    style.fontColor = Color.valueOf("F1B45A");
    style.overFontColor = Color.valueOf("FFF1D6");
    style.downFontColor = Color.valueOf("D88A2D");
    return style;
  }

  private TextButtonStyle createEternityBlessingButtonStyle() {
    TextButtonStyle style = new TextButtonStyle(createTempleButtonStyle());
    style.fontColor = Color.valueOf("C9D8FF");
    style.overFontColor = Color.valueOf("F3F7FF");
    style.downFontColor = Color.valueOf("9FB6F2");
    return style;
  }

  private void showTransientMessage(Actor actor) {
    actor.getColor().a = 0f;

    actor.addAction(
        Actions.sequence(
            Actions.fadeIn(MESSAGE_FADE_IN_SECONDS),
            Actions.delay(MESSAGE_HOLD_SECONDS),
            Actions.fadeOut(MESSAGE_FADE_OUT_SECONDS)));
  }

  private void playPrayerShake() {
    if (sceneRoot == null) {
      showBlessings();
      return;
    }

    sceneRoot.clearActions();

    sceneRoot.addAction(
        Actions.sequence(
            Actions.moveBy(8f, 0f, 0.05f),
            Actions.moveBy(-16f, 0f, 0.05f),
            Actions.moveBy(14f, 4f, 0.05f),
            Actions.moveBy(-12f, -8f, 0.05f),
            Actions.moveBy(10f, 6f, 0.05f),
            Actions.moveBy(-4f, -2f, 0.05f),
            Actions.delay(0.10f),
            Actions.run(this::showBlessings)));
  }

  private void showBlessings() {
    root.clearChildren();

    root.bottom();
    root.padBottom(35f);

    // Decide the offered Eternity effect before the player chooses a blessing.
    // Once selected for this temple visit, it does not change.
    if (offeredEternityEffect == null) {
      offeredEternityEffect = random.nextBoolean() ? EternityEffect.VITALITY : EternityEffect.FOCUS;
    }

    Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("title", Label.LabelStyle.class));
    titleStyle.fontColor = Color.valueOf("F1C879");

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = Color.valueOf("E3D3B8");

    Label title = new Label("Your Prayer Has Been Answered", titleStyle);

    Label response = new Label("The ancient guardian answers. Choose one blessing.", bodyStyle);
    showTransientMessage(title);
    showTransientMessage(response);
    TextButton warButton =
        new TextButton(
            "Blessing of War\nChoose Any Card from the Divine Archive",
            createWarBlessingButtonStyle());

    warButton.getLabel().setFontScale(1.05f);
    warButton.getLabel().setWrap(true);
    warButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            openWarBlessing();
          }
        });

    String eternityText;

    if (offeredEternityEffect == EternityEffect.VITALITY) {
      eternityText = "Blessing of Eternity\nEternal Vitality: +20 Max HP and Fully Restore HP";
    } else {
      eternityText = "Blessing of Eternity\nEternal Focus: +1 Max Energy and Restore 20 HP";
    }

    TextButton eternityButton = new TextButton(eternityText, createEternityBlessingButtonStyle());

    eternityButton.getLabel().setFontScale(1.05f);
    eternityButton.getLabel().setWrap(true);
    eternityButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            claimEternityBlessing();
          }
        });

    root.add(title).padBottom(40f);
    root.row();

    root.add(response).padBottom(50f);
    root.row();

    root.add(warButton).width(680f).height(110f).padBottom(16f);
    root.row();

    root.add(eternityButton).width(680f).height(110f);
  }

  private void openWarBlessing() {
    game.setScreen(GdxGame.ScreenType.TEMPLE_CARD_SELECTION);
  }

  private void claimEternityBlessing() {
    if (rewardClaimed || offeredEternityEffect == null) {
      return;
    }

    rewardClaimed = true;

    if (offeredEternityEffect == EternityEffect.VITALITY) {
      grantEternalVitality();
    } else {
      grantEternalFocus();
    }
  }

  /**
   * Permanently increases maximum health by 20 for the current run and fully restores the player's
   * health.
   */
  private void grantEternalVitality() {
    PlayerRunState playerState = runState.getOrCreatePlayerState();

    int newMaxHealth = playerState.getMaxHealth() + ETERNITY_MAX_HEALTH_BONUS;

    playerState.restore(newMaxHealth, newMaxHealth, playerState.getGold());

    // Keep RunState's legacy health values in sync.
    runState.setPlayerMaxHealth(newMaxHealth);
    runState.setPlayerHealth(newMaxHealth);

    finishTemple();
  }

  /** Permanently increases maximum energy by 1 for the current run and restores 20 health. */
  private void grantEternalFocus() {
    PlayerRunState playerState = runState.getOrCreatePlayerState();

    int healedHealth =
        Math.min(playerState.getMaxHealth(), playerState.getCurrentHealth() + ETERNITY_FOCUS_HEAL);

    playerState.restore(healedHealth, playerState.getMaxHealth(), playerState.getGold());

    runState.setPlayerHealth(healedHealth);

    int currentMaxEnergy = runState.getPlayerMaxEnergy();

    if (currentMaxEnergy <= 0) {
      currentMaxEnergy = PlayerFactory.getDefaultMaxEnergy();
    }

    runState.setPlayerMaxEnergy(currentMaxEnergy + ETERNITY_MAX_ENERGY_BONUS);

    finishTemple();
  }

  private void finishTemple() {
    runState.clearPendingEliteTempleReward();
    game.setScreen(GdxGame.ScreenType.MAP);
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.08f, 0.06f, 0.04f, 1f);
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
    if (templeButtonTexture != null) {
      templeButtonTexture.dispose();
    }
    skin.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
