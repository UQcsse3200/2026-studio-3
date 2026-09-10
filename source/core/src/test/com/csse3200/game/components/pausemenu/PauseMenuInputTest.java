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

/** Tests for {@link PauseMenuInput}: Escape opens the pause menu, other keys are ignored. */
@ExtendWith(GameExtension.class)
class PauseMenuInputTest {
  private PauseMenuInput input;
  private AtomicInteger pauseCount;

  @BeforeEach
  void setUp() {
    pauseCount = new AtomicInteger();
    input = new PauseMenuInput();
    Entity entity = new Entity().addComponent(input);
    entity
        .getEvents()
        .addListener(PauseMenuDisplay.PAUSE_EVENT, pauseCount::incrementAndGet);
  }

  @Test
  void escapeFiresPauseEventAndConsumesInput() {
    boolean handled = input.keyDown(Keys.ESCAPE);

    assertTrue(handled);
    assertEquals(1, pauseCount.get());
  }

  @Test
  void otherKeysAreIgnored() {
    boolean handled = input.keyDown(Keys.SPACE);

    assertFalse(handled);
    assertEquals(0, pauseCount.get());
  }
}
