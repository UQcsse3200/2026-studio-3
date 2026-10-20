package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.components.save.SaveLoadPanel;
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
  private final SaveLoadPanel saveLoadPanel;

  public PauseMenuInput() {
    this(null);
  }

  public PauseMenuInput(SaveLoadPanel saveLoadPanel) {
    super(PAUSE_INPUT_PRIORITY);
    this.saveLoadPanel = saveLoadPanel;
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

    if (saveLoadPanel != null && saveLoadPanel.isLoadConfirmationVisible()) {
      if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
        saveLoadPanel.confirmPendingLoad();
      } else if (keycode == Keys.ESCAPE) {
        saveLoadPanel.cancelPendingLoad();
      }
      return true;
    }

    if (saveLoadPanel != null && saveLoadPanel.isVisible()) {
      if (keycode == Keys.ESCAPE) {
        saveLoadPanel.navigateBack();
      }
      return true;
    }

    // Paused: keyboard-drive the menu, and consume everything else.
    switch (keycode) {
      case Keys.UP, Keys.W:
        entity.getEvents().trigger(PauseMenuDisplay.NAV_UP_EVENT);
        return true;
      case Keys.DOWN, Keys.S:
        entity.getEvents().trigger(PauseMenuDisplay.NAV_DOWN_EVENT);
        return true;
      case Keys.ENTER, Keys.SPACE:
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
