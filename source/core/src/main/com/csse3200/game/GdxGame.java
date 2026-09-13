package com.csse3200.game;

import static com.badlogic.gdx.Gdx.app;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.screens.BattleScreen;
import com.csse3200.game.screens.CardLibraryScreen;
import com.csse3200.game.screens.EncounterScreen;
import com.csse3200.game.screens.EndBattleScreen;
import com.csse3200.game.screens.LibraryScreen;
import com.csse3200.game.screens.MainGameScreen;
import com.csse3200.game.screens.MainMenuScreen;
import com.csse3200.game.screens.MapScreen;
import com.csse3200.game.screens.SaveLoadScreen;
import com.csse3200.game.screens.SettingsScreen;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the non-platform-specific game logic. Controls which screen is currently running.
 * The current screen triggers transitions to other screens. This works similarly to a finite state
 * machine (See the State Pattern).
 */
public class GdxGame extends Game {
  private static final Logger logger = LoggerFactory.getLogger(GdxGame.class);
  private BestiaryService bestiaryService;

  /**
   * Gets discovery progress shared by all screens in this game session.
   *
   * @return process-lifetime Bestiary service
   */
  public BestiaryService getBestiaryService() {
    return bestiaryService;
  }

  // Lives here rather than on a screen, since setScreen() disposes the outgoing screen.
  private final RunState runState = new RunState();

  public RunState getRunState() {
    return runState;
  }

  @Override
  public void create() {
    logger.info("Creating game");
    loadSettings();
    bestiaryService = BestiaryService.loadDefault();

    // Sets background to light yellow
    Gdx.gl.glClearColor(248f / 255f, 249 / 255f, 178 / 255f, 1);

    setScreen(ScreenType.MAIN_MENU);
  }

  /** Loads the game's settings. */
  private void loadSettings() {
    logger.debug("Loading game settings");
    UserSettings.Settings settings = UserSettings.get();
    UserSettings.applySettings(settings);
  }

  /**
   * Sets the game's screen to a new screen of the provided type.
   *
   * @param screenType screen type
   */
  public void setScreen(ScreenType screenType) {
    logger.info("Setting game screen to {}", screenType);
    Screen currentScreen = getScreen();
    if (currentScreen != null) {
      currentScreen.dispose();
    }
    ServiceLocator.registerBestiaryService(bestiaryService);
    setScreen(newScreen(screenType));
  }

  /** Opens the battle screen. Used by encounter navigation and the temporary debug shortcut. */
  public void startBattle() {
    setScreen(ScreenType.BATTLE_SCREEN);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing of current screen");
    getScreen().dispose();
  }

  /**
   * Create a new screen of the provided type.
   *
   * @param screenType screen type
   * @return new screen
   */
  private Screen newScreen(ScreenType screenType) {
    switch (screenType) {
      case MAIN_MENU:
        return new MainMenuScreen(this);
      case MAIN_GAME:
        return new MainGameScreen(this);
      case SETTINGS:
        return new SettingsScreen(this);
      case SAVE_LOAD:
        return new SaveLoadScreen(this);
      case LIBRARY:
        return new LibraryScreen(this);
      case CARD_LIBRARY:
        return new CardLibraryScreen(this);
      case MAP:
        return new MapScreen(this);
      case ENCOUNTER:
        return new EncounterScreen(this);
      case BATTLE_SCREEN:
        return new BattleScreen(this);
      case VICTORY:
        return new EndBattleScreen(this, true);
      case DEFEAT:
        return new EndBattleScreen(this, false);
      default:
        return null;
    }
  }

  public enum ScreenType {
    MAIN_MENU,
    MAIN_GAME,
    SETTINGS,
    SAVE_LOAD,
    LIBRARY,
    CARD_LIBRARY,
    MAP,
    ENCOUNTER,
    BATTLE_SCREEN,
    VICTORY,
    DEFEAT
  }

  /** Exit the game. */
  public void exit() {
    app.exit();
  }
}
