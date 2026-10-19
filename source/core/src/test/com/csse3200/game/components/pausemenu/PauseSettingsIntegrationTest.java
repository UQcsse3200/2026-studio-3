package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.settingsmenu.SettingsPanel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class PauseSettingsIntegrationTest {
  @TempDir Path folder;
  private GdxGame game;
  private GamePauseService pauseService;
  private PauseMenuDisplay display;
  private Entity pauseMenu;
  private PauseMenuInput input;
  private Graphics originalGraphics;
  private Files originalFiles;
  private FileHandle settingsFile;
  private Stage stage;

  @BeforeEach
  void setUp() {
    originalGraphics = Gdx.graphics;
    originalFiles = Gdx.files;
    Gdx.files = spy(originalFiles);
    settingsFile = spy(new FileHandle(folder.resolve("settings.json").toFile()));
    doReturn(settingsFile).when(Gdx.files).external(anyString());
    Settings initial = new Settings();
    initial.masterVolume = 0.8f;
    initial.musicVolume = 0.6f;
    initial.fps = 75;
    initial.fullscreen = true;
    initial.vsync = false;
    UserSettings.set(initial, false);
    clearInvocations(settingsFile);

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
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerInputService(mock(InputService.class));
    pauseService = new GamePauseService(new GameTime());
    ServiceLocator.registerPauseService(pauseService);

    game = mock(GdxGame.class);
    display = new PauseMenuDisplay();
    input = new PauseMenuInput();
    pauseMenu =
        new Entity()
            .addComponent(display)
            .addComponent(new PauseMenuActions(game))
            .addComponent(input);
    pauseMenu.create();
  }

  @AfterEach
  void tearDown() {
    try {
      pauseMenu.dispose();
      stage.dispose();
    } finally {
      Gdx.files = originalFiles;
      Gdx.graphics = originalGraphics;
    }
  }

  @Test
  void settingsBackKeepsGameplayPausedUntilResume() {
    openSettings();
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

  @Test
  void backAndEscapeRetainAudioAndReopeningDiscardsDisplayDrafts() {
    SettingsPanel panel = openSettings();
    for (int attempt = 0; attempt < 2; attempt++) {
      panel.<TextField>findActor("fps-cap").setText("invalid");
      panel.<CheckBox>findActor("fullscreen").setChecked(false);
      panel.<CheckBox>findActor("vsync").setChecked(true);
      float music = attempt == 0 ? 0.2f : 0.3f;
      panel.<Slider>findActor("music-volume").setValue(music);
      panel.<CheckBox>findActor("mute").setChecked(attempt == 0);
      if (attempt == 0) {
        click(display.getSettingsBackButton());
      } else {
        assertTrue(input.keyDown(Keys.ESCAPE));
      }
      assertTrue(pauseService.isPaused());
      assertFalse(display.isSettingsVisible());
      click(display.getSettingsButton());
      panel = display.getSettingsPanel();
      assertEquals(music, panel.<Slider>findActor("music-volume").getValue(), 0.0001f);
      assertEquals(attempt == 0, panel.<CheckBox>findActor("mute").isChecked());
      assertEquals("75", panel.<TextField>findActor("fps-cap").getText());
      assertTrue(panel.<CheckBox>findActor("fullscreen").isChecked());
      assertFalse(panel.<CheckBox>findActor("vsync").isChecked());
    }
    verify(settingsFile, times(4)).writeString(anyString(), eq(false));
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void failedBackAndEscapeKeepSettingsAndGameplayPausedUntilRetrySucceeds() {
    SettingsPanel panel = openSettings();
    doThrow(new GdxRuntimeException("Simulated storage failure"))
        .when(settingsFile)
        .writeString(anyString(), eq(false));
    panel.<Slider>findActor("music-volume").setValue(0.2f);

    click(display.getSettingsBackButton());
    assertTrue(display.isSettingsVisible());
    assertTrue(input.keyDown(Keys.ESCAPE));
    assertSame(panel, display.getSettingsPanel());
    assertTrue(pauseService.isPaused());
    assertEquals(0.6f, UserSettings.get().musicVolume);
    Label feedback = panel.findActor("settings-feedback");
    assertEquals(
        "Audio updated, but could not be saved. Try again.", feedback.getText().toString());
    doCallRealMethod().when(settingsFile).writeString(anyString(), eq(false));

    assertTrue(input.keyDown(Keys.ESCAPE));

    assertFalse(display.isSettingsVisible());
    assertTrue(pauseService.isPaused());
    assertEquals(0.2f, UserSettings.get().musicVolume, 0.0001f);
    verify(settingsFile, times(4)).writeString(anyString(), eq(false));
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void resumeFlushesUnfinishedAudioWithoutApplyingDisplayAndReopensCleanly() {
    SettingsPanel panel = openSettings();
    panel.<TextField>findActor("fps-cap").setText("invalid");
    panel.<CheckBox>findActor("fullscreen").setChecked(false);
    startDrag(panel.findActor("master-volume"));

    click(display.getResumeButton());

    assertFalse(pauseService.isPaused());
    assertFalse(display.isMenuVisible());
    assertFalse(display.isSettingsVisible());
    assertEquals(0f, UserSettings.get().masterVolume);
    assertEquals(75, UserSettings.get().fps);
    assertTrue(UserSettings.get().fullscreen);
    assertTrue(input.keyDown(Keys.ESCAPE));
    click(display.getSettingsButton());
    assertEquals(0f, display.getSettingsPanel().<Slider>findActor("master-volume").getValue());
    assertEquals("75", display.getSettingsPanel().<TextField>findActor("fps-cap").getText());
    verify(settingsFile).writeString(anyString(), eq(false));
  }

  @Test
  void disposalFlushesUnfinishedAudioExactlyOnce() {
    startDrag(openSettings().findActor("master-volume"));

    display.dispose();
    display.dispose();

    assertEquals(0f, UserSettings.get().masterVolume);
    assertFalse(display.isSettingsVisible());
    assertTrue(pauseService.isPaused());
    verify(settingsFile).writeString(anyString(), eq(false));
  }

  @Test
  void reopeningAnOpenSettingsPanelPreservesItsDraftAndDrag() {
    SettingsPanel panel = openSettings();
    panel.<TextField>findActor("fps-cap").setText("invalid");
    startDrag(panel.findActor("master-volume"));

    click(display.getSettingsButton());

    assertSame(panel, display.getSettingsPanel());
    assertEquals("invalid", panel.<TextField>findActor("fps-cap").getText());
    assertTrue(panel.<Slider>findActor("master-volume").isDragging());
    assertEquals(0f, panel.<Slider>findActor("master-volume").getValue());
    verify(settingsFile, never()).writeString(anyString(), eq(false));
  }

  @Test
  void forcedResumeContinuesAfterAFinalFailedSaveAttempt() {
    startDrag(openSettings().findActor("master-volume"));
    doThrow(new GdxRuntimeException("Simulated storage failure"))
        .when(settingsFile)
        .writeString(anyString(), eq(false));

    click(display.getResumeButton());
    display.dispose();

    assertFalse(pauseService.isPaused());
    assertFalse(display.isMenuVisible());
    assertFalse(display.isSettingsVisible());
    assertEquals(0.8f, UserSettings.get().masterVolume);
    verify(settingsFile).writeString(anyString(), eq(false));
  }

  private SettingsPanel openSettings() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getSettingsButton());
    return display.getSettingsPanel();
  }

  private static void startDrag(Slider slider) {
    slider.setSize(300f, 30f);
    for (EventListener listener : slider.getListeners()) {
      if (listener instanceof InputListener input) {
        assertTrue(input.touchDown(new InputEvent(), 0f, 15f, 0, 0));
        assertTrue(slider.isDragging());
        return;
      }
    }
    throw new AssertionError("Slider input listener missing");
  }

  private static void click(Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
