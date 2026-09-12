package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.save.GameStateSnapshotProvider;
import com.csse3200.game.save.JsonSaveGameRepository;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Save/load screen backed by the current run's real player, deck and map state. */
public class SaveLoadScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(SaveLoadScreen.class);
  private static final List<Integer> SLOT_IDS = List.of(1, 2, 3);

  private final GdxGame game;
  private final Renderer renderer;

  public SaveLoadScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising save/load screen services");
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerTimeSource(new GameTime());

    renderer = RenderFactory.createRenderer();
    createUI();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    RunState runState = game.getRunState();
    CardLibrary cardLibrary = new CardLibrary(CardConfigLoader.loadCards());
    PlayerRunState playerState = runState.getOrCreatePlayerState();
    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardLibrary);

    SaveGameService saveGameService =
        new SaveGameService(
            new JsonSaveGameRepository(),
            new GameStateSnapshotProvider(playerState, playerDeck, runState));
    SaveGameRestoreService restoreService =
        new SaveGameRestoreService(playerState, playerDeck, runState);

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(
            new SaveLoadPanel(
                saveGameService,
                SLOT_IDS,
                restoreService,
                () ->
                    game.setScreen(
                        runState.isRunActive()
                            ? GdxGame.ScreenType.MAP
                            : GdxGame.ScreenType.MAIN_MENU)));
    ServiceLocator.getEntityService().register(ui);
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
    logger.debug("Disposing save/load screen");
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }
}
