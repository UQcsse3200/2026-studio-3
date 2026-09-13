package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.components.spritedisplay.displaying.EndBattleDisplay;
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
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Terminal screen shown when a battle ends, for either a win or a loss. */
public class EndBattleScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(EndBattleScreen.class);

  private final GdxGame game;
  private final Renderer renderer;
  private final boolean won;
  private boolean returning = false;

  public EndBattleScreen(GdxGame game, boolean won) {
    this.game = game;
    this.won = won;

    logger.debug("Initialising end-of-battle screen (won={})", won);
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    createUI(won);
  }

  private void createUI(boolean won) {
    Stage stage = ServiceLocator.getRenderService().getStage();

    // Heading + "click to continue" hint live in sprites/EndBattle.json; the heading's text is
    // filled in below once the components are listening.
    DisplayingFactory displays = new DisplayingFactory(Path.of("sprites/EndBattle.json"));

    Entity ui = new Entity().addComponent(new InputDecorator(stage, 10)).addComponent(displays);
    ui.getEvents().addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, this::returnToMenu);
    ServiceLocator.getEntityService().register(ui);

    ui.getEvents().trigger(EndBattleDisplay.RESULT_EVENT, won ? "VICTORY" : "DEFEAT");
  }

  /**
   * Leaves the end screen. After a win the run continues, so it goes back to the map to pick the
   * next node; after a loss (or once the run is over) the run is discarded and it returns to the
   * main menu.
   */
  private void returnToMenu() {
    if (returning) {
      return;
    }
    returning = true;

    RunState runState = game.getRunState();
    if (won && runState != null && runState.isRunActive()) {
      game.setScreen(GdxGame.ScreenType.MAP);
      return;
    }

    if (runState != null) {
      runState.endRun();
    }
    game.setScreen(GdxGame.ScreenType.MAIN_MENU);
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
    ServiceLocator.clear();
  }
}
