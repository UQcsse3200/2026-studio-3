package com.csse3200.game.cards.debug;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/**
 * Input handler for the card effect debug dialog.
 *
 * <p>The dialog can be opened and closed by pressing the backtick key (`), following the same
 * toggle pattern as the engine's F1 debug terminal ({@code KeyboardTerminalInputComponent}).
 */
public class KeyboardCardEffectDebugInputComponent extends InputComponent {
  private static final int TOGGLE_OPEN_KEY = Input.Keys.GRAVE;
  private CardEffectDebugComponent debug;

  public KeyboardCardEffectDebugInputComponent() {
    super(10);
  }

  @Override
  public void create() {
    super.create();
    debug = entity.getComponent(CardEffectDebugComponent.class);
  }

  /**
   * If the toggle key is pressed, the dialog opens/closes. This dialog is read-only, so no other
   * keys are consumed while it's open.
   *
   * @return whether the input was processed
   */
  @Override
  public boolean keyDown(int keycode) {
    if (keycode == TOGGLE_OPEN_KEY) {
      debug.toggleOpen();
      return true;
    }
    return false;
  }
}
