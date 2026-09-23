package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
  private Stage stage;
  private PauseMenuDisplay display;
  private AtomicInteger resumeCount;
  private AtomicInteger saveLoadCount;
  private AtomicInteger settingsCount;
  private AtomicInteger exitCount;
  private AtomicInteger quitCount;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);

    resumeCount = new AtomicInteger();
    saveLoadCount = new AtomicInteger();
    settingsCount = new AtomicInteger();
    exitCount = new AtomicInteger();
    quitCount = new AtomicInteger();

    display = new PauseMenuDisplay();
    Entity entity = new Entity().addComponent(display);
    entity.getEvents().addListener(PauseMenuDisplay.RESUME_EVENT, resumeCount::incrementAndGet);
    entity.getEvents().addListener(PauseMenuDisplay.QUIT_EVENT, quitCount::incrementAndGet);
    entity
        .getEvents()
        .addListener(PauseMenuDisplay.SAVE_LOAD_EVENT, saveLoadCount::incrementAndGet);
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
  void pauseEventOpensMenuButDoesNotCloseIt() {
    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    assertTrue(display.isMenuVisible());

    // A second pause event (Escape pressed again) must NOT close the menu.
    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    assertTrue(display.isMenuVisible());
  }

  @Test
  void clickingResumeHidesMenu() {
    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    assertTrue(display.isMenuVisible());

    click(display.getResumeButton());

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
  void clickingSaveLoadFiresSaveLoadEvent() {
    click(display.getSaveLoadButton());

    assertEquals(1, saveLoadCount.get());
    assertEquals(0, resumeCount.get());
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
  void clickingSettingsShowsSettingsViewInsidePauseMenu() {
    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);

    click(display.getSettingsButton());

    assertTrue(display.isMenuVisible());
    assertTrue(display.isSettingsVisible());
    assertEquals(1, settingsCount.get());
  }

  @Test
  void settingsBackReturnsToPauseButtons() {
    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getSettingsButton());

    click(display.getSettingsBackButton());

    assertTrue(display.isMenuVisible());
    assertFalse(display.isSettingsVisible());
  }

  @Test
  void resumeClosesSettingsViewAndPauseMenu() {
    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getSettingsButton());

    click(display.getResumeButton());

    assertFalse(display.isMenuVisible());
    assertFalse(display.isSettingsVisible());
    assertEquals(1, resumeCount.get());
  }

  @Test
  void pauseOverlayIsTouchableAndBlocksUnderlyingActors() {
    AtomicInteger underlayTouches = new AtomicInteger();
    Actor underlay = new Actor();
    underlay.setBounds(0f, 0f, 500f, 500f);
    underlay.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            underlayTouches.incrementAndGet();
            return true;
          }
        });
    stage.addActor(underlay);
    underlay.toBack();

    display.getEntity().getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    display.getRootTable().setBounds(0f, 0f, 500f, 500f);

    assertEquals(Touchable.enabled, display.getRootTable().getTouchable());
    stage.touchDown(10, 10, 0, 0);
    assertEquals(0, underlayTouches.get());
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

  @Test
  void quitShowsDialogAndDoesNotQuitUntilConfirmed() {
    click(display.getQuitButton());

    // Dialog is shown, but nothing has quit yet.
    assertNotNull(display.getConfirmDialog().getStage());
    assertEquals(0, quitCount.get());
    assertEquals(0, exitCount.get()); // quit and main-menu share the dialog but not the action

    click(display.getConfirmButton());

    assertEquals(1, quitCount.get());
    assertEquals(0, exitCount.get());
  }

  @Test
  void saveDisabledOmitsSaveButton() {
    PauseMenuDisplay noSave = new PauseMenuDisplay(false);
    new Entity().addComponent(noSave).create();

    assertFalse(noSave.hasSaveLoadButton());
    assertNull(noSave.getSaveLoadButton());
  }

  /** Fires a keyboard-navigation event on the display's entity. */
  private void nav(String navEvent) {
    display.getEntity().getEvents().trigger(navEvent);
  }

  @Test
  void openingMenuStartsInMouseModeWithNoKeyboardFocus() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    assertNull(display.getFocusedButton());
  }

  @Test
  void firstNavDownActivatesKeyboardFocusOnFirstButton() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    nav(PauseMenuDisplay.NAV_DOWN_EVENT);
    assertSame(display.getResumeButton(), display.getFocusedButton());
  }

  @Test
  void navDownTwiceFocusesSecondButton() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    nav(PauseMenuDisplay.NAV_DOWN_EVENT); // Resume
    nav(PauseMenuDisplay.NAV_DOWN_EVENT); // Save & Load
    assertSame(display.getSaveLoadButton(), display.getFocusedButton());
  }

  @Test
  void firstNavUpWrapsToLastButton() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    nav(PauseMenuDisplay.NAV_UP_EVENT);
    assertSame(display.getQuitButton(), display.getFocusedButton());
  }

  @Test
  void navSelectActivatesFocusedButton() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    nav(PauseMenuDisplay.NAV_DOWN_EVENT); // activate keyboard focus on Resume
    nav(PauseMenuDisplay.NAV_SELECT_EVENT);
    assertEquals(1, resumeCount.get());
  }

  @Test
  void navSelectInMouseModeDoesNothing() {
    nav(PauseMenuDisplay.PAUSE_EVENT); // no nav key pressed yet -> still mouse mode
    nav(PauseMenuDisplay.NAV_SELECT_EVENT);
    assertEquals(0, resumeCount.get());
  }

  @Test
  void mouseMovementClearsKeyboardFocus() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    nav(PauseMenuDisplay.NAV_DOWN_EVENT);
    assertSame(display.getResumeButton(), display.getFocusedButton());

    InputEvent moved = new InputEvent();
    moved.setType(InputEvent.Type.mouseMoved);
    display.getRootTable().fire(moved);

    assertNull(display.getFocusedButton());
  }

  @Test
  void keyboardNavigatingToSettingsOpensSettingsView() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    nav(PauseMenuDisplay.NAV_DOWN_EVENT); // Resume
    nav(PauseMenuDisplay.NAV_DOWN_EVENT); // Save & Load
    nav(PauseMenuDisplay.NAV_DOWN_EVENT); // Settings
    nav(PauseMenuDisplay.NAV_SELECT_EVENT);

    assertTrue(display.isSettingsVisible());
    assertEquals(1, settingsCount.get());
  }

  @Test
  void navBackReturnsFromSettingsToPauseButtons() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getSettingsButton());
    assertTrue(display.isSettingsVisible());

    nav(PauseMenuDisplay.NAV_BACK_EVENT);

    assertFalse(display.isSettingsVisible());
    assertTrue(display.isMenuVisible());
  }

  @Test
  void navBackCancelsConfirmDialogWithoutExiting() {
    nav(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getReturnButton());
    assertNotNull(display.getConfirmDialog().getStage());

    nav(PauseMenuDisplay.NAV_BACK_EVENT);

    assertNull(display.getConfirmDialog().getStage());
    assertEquals(0, exitCount.get());
  }
}
