package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ServiceLocator;
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

  @Test
  void otherKeysAreConsumedWhilePaused() {
    GamePauseService pauseService = mock(GamePauseService.class);
    when(pauseService.isPaused()).thenReturn(true);
    ServiceLocator.registerPauseService(pauseService);

    assertTrue(input.keyDown(Keys.SPACE));
    assertTrue(input.keyTyped('a'));
    assertTrue(input.keyUp(Keys.SPACE));
    assertEquals(0, pauseCount.get());
  }

  @Test
  void hasPriorityAboveGameplayAndDebugInputs() {
    assertTrue(input.getPriority() > 20);
  }
}
