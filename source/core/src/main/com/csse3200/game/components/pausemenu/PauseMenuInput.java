package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Keyboard control for the pause menu. When the game isn't paused, Escape opens the menu. While
 * paused, it drives the menu by keyboard — Up/Down (or W/S) move the selection, Enter/Space
 * activates it, Escape backs out of a subview/dialog or closes the menu when already at the root,
 * and swallows all other input so gameplay shortcuts don't leak through. The display and actions
 * components handle the fired events.
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

    // Forward keys to the focused pause-settings text field
    Stage settingsStage = getSettingsInputStage();
    if (keycode != Keys.ESCAPE && settingsStage != null) {
      settingsStage.keyDown(keycode);
      return true;
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
    if (!isPaused()) {
      return false;
    }
    Stage settingsStage = getSettingsInputStage();
    if (settingsStage != null && character != '\u001b') {
      settingsStage.keyTyped(character);
    }
    return true;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (!isPaused()) {
      return false;
    }
    Stage settingsStage = getSettingsInputStage();
    if (settingsStage != null && keycode != Keys.ESCAPE) {
      settingsStage.keyUp(keycode);
    }
    return true;
  }

  /** Forward editing to the focused settings field, while still consuming gameplay input. */
  private Stage getSettingsInputStage() {
    PauseMenuDisplay display = entity.getComponent(PauseMenuDisplay.class);
    return display == null ? null : display.getSettingsInputStage();
  }

  private boolean isPaused() {
    GamePauseService pauseService = ServiceLocator.getPauseService();
    return pauseService != null && pauseService.isPaused();
  }
}
