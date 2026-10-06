package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests for {@link PauseButtonDisplay}: it opens the pause menu and hides while paused. */
@ExtendWith(GameExtension.class)
class PauseButtonDisplayTest {
  private PauseButtonDisplay display;
  private Entity entity;
  private AtomicInteger pauseCount;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    renderService.setStage(new Stage(new ScreenViewport(), mock(SpriteBatch.class)));
    ServiceLocator.registerRenderService(renderService);

    pauseCount = new AtomicInteger();
    display = new PauseButtonDisplay();
    entity = new Entity().addComponent(display);
    entity.getEvents().addListener(PauseMenuDisplay.PAUSE_EVENT, pauseCount::incrementAndGet);
    entity.create();
  }

  private static void click(Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  @Test
  void startsVisible() {
    assertTrue(display.isButtonVisible());
  }

  @Test
  void clickingButtonFiresPauseEvent() {
    click(display.getPauseButton());
    assertEquals(1, pauseCount.get());
  }

  @Test
  void hidesWhilePausedAndReappearsOnResume() {
    entity.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    assertFalse(display.isButtonVisible());

    entity.getEvents().trigger(PauseMenuDisplay.RESUME_EVENT);
    assertTrue(display.isButtonVisible());
  }
}
