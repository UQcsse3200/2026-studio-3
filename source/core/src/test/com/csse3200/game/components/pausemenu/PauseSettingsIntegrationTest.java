package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class PauseSettingsIntegrationTest {
  private GdxGame game;
  private GamePauseService pauseService;
  private PauseMenuDisplay display;
  private Entity pauseMenu;
  private Stage stage;
  private InputService inputs;
  private InputComponent gameplay;
  private Graphics originalGraphics;
  private Input originalInput;
  private MockedStatic<UserSettings> settingsMock;

  @BeforeEach
  void setUp() {
    originalGraphics = Gdx.graphics;
    originalInput = Gdx.input;
    Gdx.input = mock(Input.class);
    inputs = new InputService();
    ServiceLocator.registerInputService(inputs);
    settingsMock = mockStatic(UserSettings.class);
    settingsMock.when(UserSettings::get).thenAnswer(invocation -> new UserSettings.Settings());
    DisplayMode mode = new TestDisplayMode(1280, 800, 60, 32);
    Graphics graphics = mock(Graphics.class);
    when(graphics.getDisplayMode()).thenReturn(mode);
    when(graphics.getDisplayModes(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new DisplayMode[] {mode});
    Gdx.graphics = graphics;

    RenderService renderService = new RenderService();
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    pauseService = new GamePauseService(new GameTime());
    ServiceLocator.registerPauseService(pauseService);

    game = mock(GdxGame.class);
    display = new PauseMenuDisplay();
    pauseMenu =
        new Entity()
            .addComponent(display)
            .addComponent(new PauseMenuActions(game))
            .addComponent(new PauseMenuInput())
            .addComponent(new InputDecorator(stage, 10));
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    entities.register(pauseMenu);
    gameplay = mock(InputComponent.class);
    inputs.register(gameplay);
  }

  @AfterEach
  void tearDown() {
    assertAll(
        () -> {
          if (pauseMenu != null) pauseMenu.dispose();
        },
        () -> {
          if (stage != null) stage.dispose();
        },
        () -> {
          if (settingsMock != null) settingsMock.close();
        },
        () -> Gdx.graphics = originalGraphics,
        () -> Gdx.input = originalInput);
  }

  @Test
  void allowsFpsEditingWhileBlockingGameplayInput() {
    TextField fps = focusFps();
    fps.selectAll();

    inputs.keyDown(Keys.NUM_1);
    inputs.keyTyped('1');
    inputs.keyUp(Keys.NUM_1);
    inputs.keyTyped('2');
    inputs.keyTyped('0');

    assertEquals("120", fps.getText());

    inputs.keyDown(Keys.LEFT);
    inputs.keyUp(Keys.LEFT);
    inputs.keyTyped('\b');

    assertEquals("10", fps.getText());

    inputs.keyTyped('4');

    assertEquals("140", fps.getText());

    org.mockito.Mockito.verify(gameplay, never()).keyDown(org.mockito.ArgumentMatchers.anyInt());
    org.mockito.Mockito.verify(gameplay, never()).keyTyped(org.mockito.ArgumentMatchers.anyChar());
    org.mockito.Mockito.verify(gameplay, never()).keyUp(org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void typingIgnoresMenuShortcutsAndEscapeReturnsToPauseMenu() {
    TextField fps = focusFps();
    fps.selectAll();

    inputs.keyDown(Keys.W);
    inputs.keyTyped('w');
    inputs.keyUp(Keys.W);

    inputs.keyDown(Keys.S);
    inputs.keyTyped('s');
    inputs.keyUp(Keys.S);

    inputs.keyDown(Keys.SPACE);
    inputs.keyTyped(' ');
    inputs.keyUp(Keys.SPACE);
    inputs.keyDown(Keys.ENTER);

    assertEquals("ws ", fps.getText());
    assertNull(display.getFocusedButton());
    settingsMock.verify(
        () ->
            UserSettings.set(
                any(UserSettings.Settings.class), org.mockito.ArgumentMatchers.eq(true)),
        never());

    inputs.keyDown(Keys.ESCAPE);

    assertFalse(display.isSettingsVisible());
    assertNull(stage.getKeyboardFocus());
    assertTrue(pauseService.isPaused());

    inputs.keyTyped('9');

    assertEquals("ws ", fps.getText());

    inputs.keyDown(Keys.DOWN);

    assertEquals(display.getResumeButton(), display.getFocusedButton());
  }

  @Test
  void blocksOtherTextFieldsUntilResumed() {
    TextField fps = focusFps();
    TextField underlying = new TextField("unchanged", fps.getStyle());
    underlying.setCursorPosition(underlying.getText().length());
    stage.addActor(underlying);
    stage.setKeyboardFocus(underlying);

    inputs.keyDown(Keys.NUM_9);
    inputs.keyTyped('9');
    inputs.keyUp(Keys.NUM_9);

    assertEquals("unchanged", underlying.getText());
    assertEquals("60", fps.getText());

    click(display.getSettingsBackButton());
    click(display.getResumeButton());
    inputs.keyTyped('9');

    assertEquals("unchanged9", underlying.getText());
  }

  private TextField focusFps() {
    inputs.keyDown(Keys.ESCAPE);
    click(display.getSettingsButton());
    TextField fps = display.getSettingsPanel().findActor("fps-cap");
    stage.setKeyboardFocus(fps);
    return fps;
  }

  @Test
  void settingsBackKeepsGameplayPausedUntilResume() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getSettingsButton());

    assertTrue(pauseService.isPaused());
    assertTrue(display.isSettingsVisible());
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));

    click(display.getSettingsBackButton());

    assertTrue(pauseService.isPaused());
    assertTrue(display.isMenuVisible());
    assertFalse(display.isSettingsVisible());
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));

    click(display.getResumeButton());
    assertFalse(pauseService.isPaused());
    assertFalse(display.isMenuVisible());
  }

  private static void click(com.badlogic.gdx.scenes.scene2d.Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
