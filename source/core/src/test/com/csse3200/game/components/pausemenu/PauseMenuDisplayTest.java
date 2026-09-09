package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** Tests for {@link PauseMenuDisplay}: button wiring, visibility, and exit confirmation. */
@ExtendWith(GameExtension.class)
class PauseMenuDisplayTest {
  private PauseMenuDisplay display;
  private AtomicInteger resumeCount;
  private AtomicInteger settingsCount;
  private AtomicInteger exitCount;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    renderService.setStage(new Stage(new ScreenViewport(), mock(SpriteBatch.class)));
    ServiceLocator.registerRenderService(renderService);

    resumeCount = new AtomicInteger();
    settingsCount = new AtomicInteger();
    exitCount = new AtomicInteger();

    display = new PauseMenuDisplay();
    Entity entity = new Entity().addComponent(display);
    entity.getEvents().addListener(PauseMenuDisplay.RESUME_EVENT, resumeCount::incrementAndGet);
    entity.getEvents().addListener(PauseMenuDisplay.SETTINGS_EVENT, settingsCount::incrementAndGet);
    entity.getEvents().addListener(PauseMenuDisplay.EXIT_TO_MENU_EVENT, exitCount::incrementAndGet);
    entity.create();
  }

  /** Fires a ChangeEvent on the actor, the same event a button raises when clicked. */
  private static void click(Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  @Test
  void startsHidden() {
    assertFalse(display.isMenuVisible());
  }

  @Test
  void togglePauseShowsThenHidesMenu() {
    display.getEntity().getEvents().trigger(PauseMenuDisplay.TOGGLE_PAUSE_EVENT);
    assertTrue(display.isMenuVisible());

    display.getEntity().getEvents().trigger(PauseMenuDisplay.TOGGLE_PAUSE_EVENT);
    assertFalse(display.isMenuVisible());
  }

  @Test
  void clickingResumeFiresResumeEvent() {
    click(display.getResumeButton());

    assertEquals(1, resumeCount.get());
    assertEquals(0, settingsCount.get());
    assertEquals(0, exitCount.get());
  }

  @Test
  void clickingSettingsFiresSettingsEvent() {
    click(display.getSettingsButton());

    assertEquals(1, settingsCount.get());
    assertEquals(0, resumeCount.get());
    assertEquals(0, exitCount.get());
  }

  @Test
  void returnToMainMenuShowsDialogAndDoesNotExitUntilConfirmed() {
    click(display.getReturnButton());

    // Dialog is shown, but nothing has left the run yet.
    assertNotNull(display.getConfirmDialog().getStage());
    assertEquals(0, exitCount.get());

    click(display.getConfirmButton());

    assertEquals(1, exitCount.get());
  }

  @Test
  void cancellingConfirmationDismissesDialogWithoutExiting() {
    click(display.getReturnButton());
    click(display.getCancelButton());

    assertEquals(0, exitCount.get());
    assertNull(display.getConfirmDialog().getStage());
  }
}
