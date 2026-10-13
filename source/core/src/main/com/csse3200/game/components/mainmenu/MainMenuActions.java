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
    entity.getEvents().addListener(MainMenuDisplay.ENTER_TUTORIAL_EVENT, this::onEnterTutorial);
    entity.getEvents().addListener(MainMenuDisplay.LOAD_EVENT, this::onLoad);
    entity.getEvents().addListener(MainMenuDisplay.BESTIARY_EVENT, this::onBestiary);
    entity.getEvents().addListener(MainMenuDisplay.SETTINGS_EVENT, this::onSettings);
    entity.getEvents().addListener(MainMenuDisplay.EXIT_EVENT, this::onExit);
    entity.getEvents().addListener("library", this::onLibrary);
    entity.getEvents().addListener(MainMenuDisplay.DEMO_EVENT, game::openDemoEvent);
    entity.getEvents().addListener(MainMenuDisplay.DEMO_CAMPFIRE_EVENT, game::openDemoCampfire);
  }

  /** Discards any run in progress and opens a fresh map. */
  private void onStart() {
    logger.info("Opening map");
    game.startNewRun();
  }

  /** Launches the optional teaching encounter after New Game's Enter Tutorial choice. */
  private void onEnterTutorial() {
    logger.info("Opening the tutorial battle");
    game.startTutorialBattle();
  }

  /** Opens the Save/Load screen. */
  private void onLoad() {
    logger.info("Opening save/load screen");
    game.setScreen(GdxGame.ScreenType.SAVE_LOAD);
  }

  /** Opens the bestiary screen. */
  private void onBestiary() {
    logger.info("Opening library screen from bestiary button");
    game.setScreen(GdxGame.ScreenType.LIBRARY);
  }

  /** Opens the library screen. Not currently reachable from the main menu — see TODO. */
  // TODO: no menu button currently triggers this event; flagged to Team 4/William re: whether
  // Library needs a menu entry point now that the old menu (which had one) is gone.
  private void onLibrary() {
    logger.info("Opening library screen");
    game.setScreen(GdxGame.ScreenType.LIBRARY);
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
