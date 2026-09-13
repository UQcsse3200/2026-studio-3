package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Listens for the Escape key and opens the pause menu by firing {@link
 * PauseMenuDisplay#PAUSE_EVENT} on its entity's events. The display (and the actions component)
 * decide what to do with that event.
 */
public class PauseMenuInput extends InputComponent {
  private static final int PAUSE_INPUT_PRIORITY = 100;

  public PauseMenuInput() {
    super(PAUSE_INPUT_PRIORITY);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode == Keys.ESCAPE) {
      entity.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
      return true;
    }
    return isPaused();
  }

  @Override
  public boolean keyTyped(char character) {
    return isPaused();
  }

  @Override
  public boolean keyUp(int keycode) {
    return isPaused();
  }

  private boolean isPaused() {
    GamePauseService pauseService = ServiceLocator.getPauseService();
    return pauseService != null && pauseService.isPaused();
  }
}
