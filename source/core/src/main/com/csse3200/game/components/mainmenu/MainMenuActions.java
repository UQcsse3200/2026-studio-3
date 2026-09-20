package com.csse3200.game.components.mainmenu;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import com.csse3200.game.debug.DemoEncounterAccess;
import com.csse3200.game.maps.RoomType;
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
    entity.getEvents().addListener("library", this::onLibrary);
    if (DemoEncounterAccess.ENABLED) {
      entity.getEvents().addListener(MainMenuDisplay.DEMO_SHOP_EVENT, this::onDemoShop);
      entity.getEvents().addListener(MainMenuDisplay.DEMO_EVENT_EVENT, this::onDemoEvent);
    }
  }

  /** Discards any run in progress and opens a fresh map. */
  private void onStart() {
    logger.info("Opening map");
    game.getRunState().endRun();
    game.setScreen(GdxGame.ScreenType.MAP);
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

  private void onDemoShop() {
    logger.info("Opening temporary Shop demo");
    DemoEncounterAccess.open(game, RoomType.SHOP);
  }

  private void onDemoEvent() {
    logger.info("Opening temporary Event demo");
    DemoEncounterAccess.open(game, RoomType.EVENT);
  }
}
