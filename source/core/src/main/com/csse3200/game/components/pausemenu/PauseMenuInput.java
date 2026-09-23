package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Keyboard control for the pause menu. When the game isn't paused, Escape opens the menu. While
 * paused, it drives the menu by keyboard — Up/Down (or W/S) move the selection, Enter/Space
 * activates it, Escape backs out of a subview/dialog — and swallows all other input so gameplay
 * shortcuts don't leak through. The display and actions components handle the fired events.
 */
public class PauseMenuInput extends InputComponent {
  private static final int PAUSE_INPUT_PRIORITY = 100;

  public PauseMenuInput() {
    super(PAUSE_INPUT_PRIORITY);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!isPaused()) {
      if (keycode == Keys.ESCAPE) {
        entity.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
        return true;
      }
      return false;
    }

    // Paused: keyboard-drive the menu, and consume everything else.
    switch (keycode) {
      case Keys.UP:
      case Keys.W:
        entity.getEvents().trigger(PauseMenuDisplay.NAV_UP_EVENT);
        return true;
      case Keys.DOWN:
      case Keys.S:
        entity.getEvents().trigger(PauseMenuDisplay.NAV_DOWN_EVENT);
        return true;
      case Keys.ENTER:
      case Keys.SPACE:
        entity.getEvents().trigger(PauseMenuDisplay.NAV_SELECT_EVENT);
        return true;
      case Keys.ESCAPE:
        entity.getEvents().trigger(PauseMenuDisplay.NAV_BACK_EVENT);
        return true;
      default:
        return true;
    }
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
