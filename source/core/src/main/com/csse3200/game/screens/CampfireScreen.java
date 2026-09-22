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

/**
 * Campfire encounter where the player can rest or forge a card.
 *
 * <p>Rest restores health, while Forge upgrades one eligible card in the persistent player deck.
 */
public class CampfireScreen extends ScreenAdapter {
  private static final float REST_HEAL_PERCENT = 0.30f;

  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;

  public CampfireScreen(GdxGame game) {
    this.game = game;
    this.runState = game.getRunState();

    if (runState == null) {
      throw new IllegalStateException("CampfireScreen opened without a RunState");
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

    PlayerRunState playerState = runState.getOrCreatePlayerState();

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

    Entity upgradeEntity = new Entity().addComponent(upgradeDisplay);
    ServiceLocator.getEntityService().register(upgradeEntity);

    Table root = new Table();
    root.setFillParent(true);

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = new Color(0.9f, 0.8f, 0.65f, 1f);

    Label title = new Label("Campfire", skin, "title");

    Label health =
        new Label(
            "HP: " + playerState.getCurrentHealth() + " / " + playerState.getMaxHealth(),
            bodyStyle);

    int healAmount = Math.max(1, Math.round(playerState.getMaxHealth() * REST_HEAL_PERCENT));

    TextButton restButton = new TextButton("Rest\n(Restore " + healAmount + " HP)", skin);

    restButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            rest();
          }
        });

    TextButton forgeButton = new TextButton("Forge\n(Upgrade a Card)", skin);

    boolean hasUpgradableCards = !upgradeSelection.getCardUpgradeOption().isEmpty();

    forgeButton.setDisabled(!hasUpgradableCards);

    if (hasUpgradableCards) {
      forgeButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              upgradeDisplay.showLibrary();
            }
          });
    }

    Label forgeStatus =
        new Label(
            hasUpgradableCards ? "Choose a card to upgrade." : "No cards available to upgrade.",
            bodyStyle);

    root.add(title).padBottom(30f);
    root.row();

    root.add(health).padBottom(30f);
    root.row();

    root.add(restButton).width(420f).height(90f).padBottom(15f);
    root.row();

    root.add(forgeButton).width(420f).height(90f).padBottom(10f);
    root.row();

    root.add(forgeStatus);

    stage.addActor(root);
  }

  private void rest() {
    PlayerRunState playerState = runState.getOrCreatePlayerState();

    int healAmount = Math.max(1, Math.round(playerState.getMaxHealth() * REST_HEAL_PERCENT));

    int newHealth =
        Math.min(playerState.getMaxHealth(), playerState.getCurrentHealth() + healAmount);

    playerState.restore(newHealth, playerState.getMaxHealth(), playerState.getGold());

    // Keep legacy RunState health values in sync.
    runState.setPlayerHealth(newHealth);
    runState.setPlayerMaxHealth(playerState.getMaxHealth());

    finishCampfire();
  }

  private void finishCampfire() {
    runState.completeEncounter(true);
    game.setScreen(GdxGame.ScreenType.MAP);
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.10f, 0.06f, 0.03f, 1f);
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
