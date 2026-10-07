package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class PopupInputComponentTest {
  @Mock PopupDisplay popup;

  private PopupInputComponent input;

  @BeforeEach
  void setUp() {
    input = new PopupInputComponent(popup);
  }

  @Test
  void escapeClosesAndConsumesVisiblePopup() {
    when(popup.isShowing()).thenReturn(true);

    assertTrue(input.keyDown(Keys.ESCAPE));
    verify(popup).hide();
  }

  @Test
  void escapeFallsThroughWhenPopupIsHidden() {
    when(popup.isShowing()).thenReturn(false);

    assertFalse(input.keyDown(Keys.ESCAPE));
    verify(popup, never()).hide();
  }

  @Test
  void otherKeysAreIgnoredWhilePopupIsVisible() {
    assertFalse(input.keyDown(Keys.SPACE));
    verify(popup, never()).hide();
  }

  @Test
  void hasPriorityAbovePauseMenuInput() {
    assertTrue(input.getPriority() > 100);
  }

  @Test
  void rejectsNullPopup() {
    assertThrows(IllegalArgumentException.class, () -> new PopupInputComponent(null));
  }
}
