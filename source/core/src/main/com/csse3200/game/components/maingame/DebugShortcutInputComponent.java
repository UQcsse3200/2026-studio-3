package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.GdxGame;
import com.csse3200.game.input.InputComponent;

/** Provides temporary keyboard shortcuts for development screens. */
public class DebugShortcutInputComponent extends InputComponent {
  private final GdxGame game;
  private boolean controlPressed;
  private boolean shiftPressed;

  public DebugShortcutInputComponent(GdxGame game) {
    super(20);
    this.game = game;
  }

  @Override
  public boolean keyDown(int keycode) {
    switch (keycode) {
      case Keys.CONTROL_LEFT:
        controlPressed = true;
        return false;
      case Keys.SHIFT_LEFT:
        shiftPressed = true;
        return false;
      case Keys.B:
        if (controlPressed && shiftPressed) {
          game.startBattle();
          return true;
        }
        return false;
      case Keys.P:
        if (controlPressed && shiftPressed) {
          game.startElitePortalDebug();
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  @Override
  public boolean keyUp(int keycode) {
    switch (keycode) {
      case Keys.CONTROL_LEFT:
        controlPressed = false;
        return false;
      case Keys.SHIFT_LEFT:
        shiftPressed = false;
        return false;
      default:
        return false;
    }
  }
}
