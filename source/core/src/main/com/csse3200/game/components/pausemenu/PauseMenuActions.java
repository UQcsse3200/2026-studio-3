package com.csse3200.game.components.pausemenu;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to pause-menu events and performs the screen navigation for them.
 *
 * <p>Deliberately minimal: this owns only the navigation (sub-feature 5). The real pause behaviour
 * — freezing gameplay, blocking input, preserving the run — belongs to sub-feature 6, left
 * stubbed in {@link #onPause()} for Sahil to build on top of, using the same event names
 * defined on {@link PauseMenuDisplay}.
 */
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

  /**
   * Resumes play. The display hides itself on this event; this is where the unpause logic (the
   * mirror of {@link #onPause()}) belongs.
   */
  private void onResume() {
    logger.info("Resuming game from pause menu");
    // TODO(Sahil, sub-feature 6): unfreeze gameplay (timeScale=1) and re-enable gameplay/map input
  }

  /** Opens the settings screen. */
  private void onSettings() {
    logger.info("Opening settings from pause menu");
    game.setScreen(GdxGame.ScreenType.SETTINGS);
  }

  /** Leaves the current run for the main menu. Only fired after the display's confirm dialog. */
  private void onExitToMenu() {
    logger.info("Returning to main menu from pause menu");
    game.setScreen(GdxGame.ScreenType.MAIN_MENU);
  }

  /** Fired on Escape to open the menu. The display shows itself; real pause behaviour goes here. */
  private void onPause() {
    // TODO(Sahil, sub-feature 6): freeze gameplay (timeScale=0), block gameplay/map input while
    // paused, preserve the current run
  }
}
