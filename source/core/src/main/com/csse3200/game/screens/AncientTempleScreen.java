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
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Temporary playable version of the hidden Ancient Temple encounter.
 *
 * <p>The player enters from the Elite portal, prays to the statue and selects one blessing before
 * returning to the map.
 */
public class AncientTempleScreen extends ScreenAdapter {
  private static final int GOLD_REWARD = 50;
  private static final int HEAL_REWARD = 20;

  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;

  private Table root;
  private boolean rewardClaimed;

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

    Label title = new Label("Your Prayer Has Been Answered", skin, "title");

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = new Color(0.9f, 0.8f, 0.65f, 1f);

    Label response =
        new Label("The stone statue stirs. An ancient power answers your prayer.", bodyStyle);

    TextButton goldButton = new TextButton("Blessing of Wealth  (+50 Gold)", skin);

    goldButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            claimGoldBlessing();
          }
        });

    TextButton healButton = new TextButton("Blessing of Vitality  (Heal 20 HP)", skin);

    healButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            claimHealingBlessing();
          }
        });

    root.add(title).padBottom(40f);
    root.row();

    root.add(response).padBottom(50f);
    root.row();

    root.add(goldButton).width(420f).height(80f).padBottom(20f);
    root.row();

    root.add(healButton).width(420f).height(80f);
  }

  private void claimGoldBlessing() {
    if (rewardClaimed) {
      return;
    }
    rewardClaimed = true;

    PlayerRunState playerState = runState.getOrCreatePlayerState();

    playerState.restore(
        playerState.getCurrentHealth(),
        playerState.getMaxHealth(),
        playerState.getGold() + GOLD_REWARD);

    finishTemple();
  }

  private void claimHealingBlessing() {
    if (rewardClaimed) {
      return;
    }
    rewardClaimed = true;

    PlayerRunState playerState = runState.getOrCreatePlayerState();

    int healedHealth =
        Math.min(playerState.getMaxHealth(), playerState.getCurrentHealth() + HEAL_REWARD);

    playerState.restore(healedHealth, playerState.getMaxHealth(), playerState.getGold());

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
