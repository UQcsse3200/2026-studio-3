package com.csse3200.game.components.mainmenu;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to events relevant to the Main Menu Screen and does something when one of the
 * events is triggered.
 */
public class MainMenuActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuActions.class);
  private GdxGame game;

  public MainMenuActions(GdxGame game) {
    this.game = game;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(MainMenuDisplay.START_EVENT, this::onStart);
    entity.getEvents().addListener(MainMenuDisplay.LOAD_EVENT, this::onLoad);
    entity.getEvents().addListener(MainMenuDisplay.BESTIARY_EVENT, this::onBestiary);
    entity.getEvents().addListener(MainMenuDisplay.SETTINGS_EVENT, this::onSettings);
    entity.getEvents().addListener(MainMenuDisplay.EXIT_EVENT, this::onExit);
  }

  /** Starts a new run on the map. */
  private void onStart() {
    logger.info("Opening map");
    game.getRunState().endRun();
    game.setScreen(GdxGame.ScreenType.MAP);
  }

  /** Intended for loading a saved game state. Load functionality is not actually implemented. */
  private void onLoad() {
    logger.info("Load game");
  }

  /** Intended for displaying the bestiary. Bestiary functionality is not actually implemented. */
  private void onBestiary() {
    logger.info("Bestiary");
  }

  /** Exits the game. */
  private void onExit() {
    logger.info("Exit game");
    game.exit();
  }

  /** Swaps to the Settings screen. */
  private void onSettings() {
    logger.info("Launching settings screen");
    game.setScreen(GdxGame.ScreenType.SETTINGS);
  }
}
