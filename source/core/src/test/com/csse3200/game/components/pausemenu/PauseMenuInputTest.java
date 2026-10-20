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
  private AtomicInteger navUpCount;
  private AtomicInteger navDownCount;
  private AtomicInteger navSelectCount;
  private AtomicInteger navBackCount;

  @BeforeEach
  void setUp() {
    pauseCount = new AtomicInteger();
    navUpCount = new AtomicInteger();
    navDownCount = new AtomicInteger();
    navSelectCount = new AtomicInteger();
    navBackCount = new AtomicInteger();
    input = new PauseMenuInput();
    Entity entity = new Entity().addComponent(input);
    entity.getEvents().addListener(PauseMenuDisplay.PAUSE_EVENT, pauseCount::incrementAndGet);
    entity.getEvents().addListener(PauseMenuDisplay.NAV_UP_EVENT, navUpCount::incrementAndGet);
    entity.getEvents().addListener(PauseMenuDisplay.NAV_DOWN_EVENT, navDownCount::incrementAndGet);
    entity
        .getEvents()
        .addListener(PauseMenuDisplay.NAV_SELECT_EVENT, navSelectCount::incrementAndGet);
    entity.getEvents().addListener(PauseMenuDisplay.NAV_BACK_EVENT, navBackCount::incrementAndGet);
  }

  private static void pauseGame() {
    GamePauseService pauseService = mock(GamePauseService.class);
    when(pauseService.isPaused()).thenReturn(true);
    ServiceLocator.registerPauseService(pauseService);
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
    pauseGame();

    assertTrue(input.keyTyped('a'));
    assertTrue(input.keyUp(Keys.SPACE));
    assertEquals(0, pauseCount.get());
  }

  @Test
  void arrowsAndEnterFireNavEventsWhilePaused() {
    pauseGame();

    assertTrue(input.keyDown(Keys.UP));
    assertTrue(input.keyDown(Keys.DOWN));
    assertTrue(input.keyDown(Keys.ENTER));

    assertEquals(1, navUpCount.get());
    assertEquals(1, navDownCount.get());
    assertEquals(1, navSelectCount.get());
    assertEquals(0, pauseCount.get());
  }

  @Test
  void escapeWhilePausedFiresNavBackNotPause() {
    pauseGame();

    assertTrue(input.keyDown(Keys.ESCAPE));

    assertEquals(1, navBackCount.get());
    assertEquals(0, pauseCount.get());
  }

  @Test
  void hasPriorityAboveGameplayAndDebugInputs() {
    assertTrue(input.getPriority() > 20);
  }
}
