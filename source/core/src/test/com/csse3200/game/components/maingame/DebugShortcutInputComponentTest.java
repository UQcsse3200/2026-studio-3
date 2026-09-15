package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.GdxGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebugShortcutInputComponentTest {
  @Mock GdxGame game;

  private DebugShortcutInputComponent input;

  @BeforeEach
  void setUp() {
    input = new DebugShortcutInputComponent(game);
  }

  @Test
  void shouldStartBattleWhenCtrlShiftBPressed() {
    assertFalse(input.keyDown(Keys.CONTROL_LEFT));
    assertFalse(input.keyDown(Keys.SHIFT_LEFT));

    assertTrue(input.keyDown(Keys.B));

    verify(game).startBattle();
  }

  @Test
  void shouldNotStartBattleWithoutBothModifiers() {
    assertFalse(input.keyDown(Keys.CONTROL_LEFT));

    assertFalse(input.keyDown(Keys.B));

    verify(game, never()).startBattle();
  }

  @Test
  void shouldNotStartBattleAfterControlReleased() {
    input.keyDown(Keys.CONTROL_LEFT);
    input.keyDown(Keys.SHIFT_LEFT);

    assertFalse(input.keyUp(Keys.CONTROL_LEFT));
    assertFalse(input.keyDown(Keys.B));

    verify(game, never()).startBattle();
  }

  @Test
  void shouldNotStartBattleAfterShiftReleased() {
    input.keyDown(Keys.CONTROL_LEFT);
    input.keyDown(Keys.SHIFT_LEFT);

    assertFalse(input.keyUp(Keys.SHIFT_LEFT));
    assertFalse(input.keyDown(Keys.B));

    verify(game, never()).startBattle();
  }
}
