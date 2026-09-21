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

  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;

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

    root = new Table();
    root.setFillParent(true);

    showTempleEntrance();

    stage.addActor(root);
  }

  private void showTempleEntrance() {
    root.clearChildren();

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = new Color(0.9f, 0.8f, 0.65f, 1f);

    Label title = new Label("Ancient Temple", skin, "title");

    Label statue = new Label("[ ANCIENT STONE STATUE ]", bodyStyle);
    statue.setFontScale(1.4f);

    Label description =
        new Label("The silent statue watches over the forgotten temple.", bodyStyle);

    TextButton prayButton = new TextButton("Pray", skin);

    prayButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            showBlessings();
          }
        });

    root.add(title).padBottom(60f);
    root.row();

    root.add(statue).padBottom(40f);
    root.row();

    root.add(description).padBottom(50f);
    root.row();

    root.add(prayButton).width(300f).height(80f);
  }

  private void showBlessings() {
    root.clearChildren();

    // Decide the offered Eternity effect before the player chooses a blessing.
    // Once selected for this temple visit, it does not change.
    if (offeredEternityEffect == null) {
      offeredEternityEffect = random.nextBoolean() ? EternityEffect.VITALITY : EternityEffect.FOCUS;
    }

    Label title = new Label("Your Prayer Has Been Answered", skin, "title");

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = new Color(0.9f, 0.8f, 0.65f, 1f);

    Label response = new Label("The stone statue stirs. Choose one blessing.", bodyStyle);

    TextButton warButton =
        new TextButton("Blessing of War\n" + "(Choose Any Card from the Divine Archive)", skin);

    warButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            openWarBlessing();
          }
        });

    String eternityText;

    if (offeredEternityEffect == EternityEffect.VITALITY) {
      eternityText =
          "Blessing of Eternity\n" + "(Eternal Vitality: +20 Max HP and Fully Restore HP)";
    } else {
      eternityText = "Blessing of Eternity\n" + "(Eternal Focus: +1 Max Energy and Restore 20 HP)";
    }

    TextButton eternityButton = new TextButton(eternityText, skin);

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

    root.add(warButton).width(620f).height(100f).padBottom(20f);
    root.row();

    root.add(eternityButton).width(620f).height(100f);
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
    skin.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
