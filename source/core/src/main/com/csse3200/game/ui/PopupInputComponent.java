package com.csse3200.game.ui;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.input.InputComponent;

/**
 * Gives an open {@link PopupDisplay} first refusal on Escape before the pause-menu input handler.
 *
 * <p>The handler deliberately ignores every key while its popup is hidden, so Escape retains its
 * normal pause-menu behaviour when no popup is open.
 */
public class PopupInputComponent extends InputComponent {
  static final int POPUP_INPUT_PRIORITY = 110;

  private final PopupDisplay popup;

  public PopupInputComponent(PopupDisplay popup) {
    super(POPUP_INPUT_PRIORITY);
    if (popup == null) {
      throw new IllegalArgumentException("popup must not be null");
    }
    this.popup = popup;
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode != Keys.ESCAPE || !popup.isShowing()) {
      return false;
    }

    popup.hide();
    return true;
  }
}
