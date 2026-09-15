package com.csse3200.game.components.pausemenu;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Listens to pause-menu events and performs pause state changes and screen navigation. */
public class PauseMenuActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PauseMenuActions.class);
  private final GdxGame game;

  public PauseMenuActions(GdxGame game) {
    this.game = game;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(PauseMenuDisplay.RESUME_EVENT, this::onResume);
    entity.getEvents().addListener(PauseMenuDisplay.SETTINGS_EVENT, this::onSettings);
    entity.getEvents().addListener(PauseMenuDisplay.EXIT_TO_MENU_EVENT, this::onExitToMenu);
    entity.getEvents().addListener(PauseMenuDisplay.PAUSE_EVENT, this::onPause);
  }

  /** Resumes play. The display hides itself on this event. */
  private void onResume() {
    logger.info("Resuming game from pause menu");
    getPauseService().resume();
  }

  /** Opens settings inside the pause menu display. */
  private void onSettings() {
    logger.info("Opening settings from pause menu");
  }

  /** Leaves the current run for the main menu. Only fired after the display's confirm dialog. */
  private void onExitToMenu() {
    logger.info("Returning to main menu from pause menu");
    if (game.getRunState() != null) {
      game.getRunState().abandonEncounter();
    }
    getPauseService().resume();
    game.setScreen(GdxGame.ScreenType.MAIN_MENU);
  }

  /** Fired on Escape to open the menu. The display shows itself. */
  private void onPause() {
    getPauseService().pause();
  }

  private GamePauseService getPauseService() {
    GamePauseService pauseService = ServiceLocator.getPauseService();
    if (pauseService == null) {
      throw new IllegalStateException("Pause service is not registered");
    }
    return pauseService;
  }
}
