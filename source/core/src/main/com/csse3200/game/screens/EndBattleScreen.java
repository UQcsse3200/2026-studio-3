package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.cards.CardUpgradeDisplay;
import com.csse3200.game.components.cards.CardUpgradeSelection;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.components.cards.PlayerDeckCardUpgradeCommitter;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingRecord;
import com.csse3200.game.components.spritedisplay.displaying.EndBattleDisplay;
import com.csse3200.game.components.spritedisplay.displaying.RewardDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.rewards.RewardGenerator;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Terminal screen shown when a battle ends, for either a win or a loss. */
public class EndBattleScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(EndBattleScreen.class);
  private static final String[] REWARD_TEXTURES = {
    "images/ui/reward-panel.png",
    "images/ui/reward-card.png",
    "images/ui/gold-reward.png",
    "images/ui/victory-title.png",
    "images/ui/lucky-coin.png",
    "images/ui/energy-crystal.png",
    "images/ui/merchants-favor.png",
    "images/ui/iron-aegis.png",
    "images/ui/warriors-crest.png"
  };

  private final GdxGame game;
  private final Renderer renderer;
  private final boolean won;
  private String[] cardTextures = new String[0];
  private boolean returning = false;
  private boolean rewardClaimed = false;

  public EndBattleScreen(GdxGame game, boolean won) {
    this.game = game;
    this.won = won;

    logger.debug("Initialising end-of-battle screen (won={})", won);
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    ServiceLocator.getResourceService().loadTextures(REWARD_TEXTURES);
    ServiceLocator.getResourceService().loadAll();

    renderer = RenderFactory.createRenderer();
    List<CardConfig> cardConfigs = CardConfigLoader.loadCards();
    CardService cardLibrary = new CardLibrary(cardConfigs);
    if (won) {
      loadCardAssets(cardConfigs);
    }
    createUI(won, cardLibrary);
  }

  private void createUI(boolean won, CardService cardLibrary) {
    Stage stage = ServiceLocator.getRenderService().getStage();

    DisplayingFactory displays = new DisplayingFactory(Path.of("sprites/EndBattle.json"));

    Entity ui = new Entity().addComponent(new InputDecorator(stage, 10)).addComponent(displays);

    boolean requiresPlayerChoice = false;

    if (won) {
      RewardService rewardService =
          new RewardService(new RewardGenerator(), cardLibrary, new Random());
      DisplayingRecord rewardRecord =
          DisplayingRecord.builder("").position(0, 500).variant("reward").build();
      ui.addComponent(
          new RewardDisplay(
              rewardRecord,
              rewardService,
              game,
              cardLibrary,
              game.getCardDiscoveryService(),
              game::autosaveAfterRewardClaimed));
      requiresPlayerChoice = true;

      RunState runState = game.getRunState();
      if (runState != null) {
        PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardLibrary);
        CardUpgradeSelection upgradeSelection =
            CardUpgradeSelection.forPlayerDeck(playerDeck, cardLibrary, 2);
        if (!upgradeSelection.getCardUpgradeOption().isEmpty()) {
          ui.addComponent(
              new CardUpgradeDisplay(
                  upgradeSelection, new PlayerDeckCardUpgradeCommitter(playerDeck)));
        }
      }
    }

    if (requiresPlayerChoice) {
      for (EndBattleDisplay endBattleDisplay : displays.getDisplayings(EndBattleDisplay.class)) {
        endBattleDisplay.setClickToReturnEnabled(false);
        endBattleDisplay.setVisible(false);
      }
    }

    ui.getEvents().addListener(RewardDisplay.REWARD_CLAIMED_EVENT, this::onRewardClaimed);
    ui.getEvents().addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, this::returnToMenu);
    ServiceLocator.getEntityService().register(ui);

    if (requiresPlayerChoice) {
      for (EndBattleDisplay endBattleDisplay : displays.getDisplayings(EndBattleDisplay.class)) {
        endBattleDisplay.setClickToReturnEnabled(false);
        endBattleDisplay.setVisible(false);
      }
    }
  }

  private void loadCardAssets(List<CardConfig> cardConfigs) {
    cardTextures = CardWidgetAssets.collectTexturePaths(cardConfigs);
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(cardTextures);
    resources.loadAll();
  }

  /**
   * Leaves the end screen. A final victory ends the run and plays the victory crawl before the main
   * menu. Other active-run wins continue to the Elite portal when eligible, or to the map. A loss
   * in an active run ends it and plays the defeat crawl before the main menu; otherwise, the run is
   * discarded and the main menu opens directly.
   */
  private void returnToMenu() {
    // A victory is not durable until its selected reward has been applied and autosaved.
    if (won && !rewardClaimed) {
      logger.debug("Ignoring victory-screen exit before a reward is claimed");
      return;
    }
    if (returning) {
      return;
    }
    returning = true;

    RunState runState = game.getRunState();

    if (won && runState != null && runState.isFinalEncounterCompleted()) {
      runState.endRun();
      game.showNarration("victory", GdxGame.ScreenType.MAIN_MENU);
      return;
    }

    if (won && runState != null && runState.isRunActive()) {
      if (runState.hasPendingEliteTempleReward()) {
        logger.info("Elite temple reward is eligible, opening portal");
        game.setScreen(GdxGame.ScreenType.ELITE_PORTAL);
        return;
      }

      game.setScreen(GdxGame.ScreenType.MAP);
      return;
    }

    boolean lostRun = !won && runState != null && runState.isRunActive();
    if (runState != null) {
      runState.endRun();
    }
    if (lostRun) {
      game.showNarration("defeat", GdxGame.ScreenType.MAIN_MENU);
    } else {
      game.setScreen(GdxGame.ScreenType.MAIN_MENU);
    }
  }

  private void onRewardClaimed() {
    rewardClaimed = true;
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(cardTextures);
  }
}
