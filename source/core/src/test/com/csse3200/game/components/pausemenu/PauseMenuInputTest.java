package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests for {@link PauseMenuInput}: Escape toggles the pause menu, other keys are ignored. */
@ExtendWith(GameExtension.class)
class PauseMenuInputTest {
  private PauseMenuInput input;
  private AtomicInteger toggleCount;

  @BeforeEach
  void setUp() {
    toggleCount = new AtomicInteger();
    input = new PauseMenuInput();
    Entity entity = new Entity().addComponent(input);
    entity
        .getEvents()
        .addListener(PauseMenuDisplay.TOGGLE_PAUSE_EVENT, toggleCount::incrementAndGet);
  }

  @Test
  void escapeFiresTogglePauseAndConsumesInput() {
    boolean handled = input.keyDown(Keys.ESCAPE);

    assertTrue(handled);
    assertEquals(1, toggleCount.get());
  }

  @Test
  void otherKeysAreIgnored() {
    boolean handled = input.keyDown(Keys.SPACE);

    assertFalse(handled);
    assertEquals(0, toggleCount.get());
  }
}
