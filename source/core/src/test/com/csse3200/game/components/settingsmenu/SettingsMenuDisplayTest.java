package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class SettingsMenuDisplayTest {
  @TempDir Path folder;
  private GdxGame game;
  private SettingsMenuDisplay display;
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
    game = mock(GdxGame.class);
    createDisplay();
  }

  @AfterEach
  void tearDown() {
    try {
      display.dispose();
      stage.dispose();
    } finally {
      Gdx.files = originalFiles;
      Gdx.graphics = originalGraphics;
    }
  }

  @Test
  void hostsReusablePanelAndBackReturnsToMainMenu() {
    display.getSettingsPanel().getBackButton().fire(new ChangeListener.ChangeEvent());

    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
    verify(settingsFile, never()).writeString(anyString(), eq(false));
  }

  @Test
  void backAndDisposalRetainSavedAudioAndReopeningDiscardsDisplayDrafts() {
    SettingsPanel panel = display.getSettingsPanel();
    panel.getFpsText().setText("invalid");
    panel.getFullScreenCheck().setChecked(false);
    panel.getVsyncCheck().setChecked(true);
    panel.getMusicVolumeSlider().setValue(0.2f);
    panel.getMuteCheck().setChecked(true);
    panel.getBackButton().fire(new ChangeListener.ChangeEvent());
    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
    display.dispose();
    createDisplay();

    SettingsPanel reopened = display.getSettingsPanel();
    assertEquals(0.2f, reopened.getMusicVolumeSlider().getValue(), 0.0001f);
    assertTrue(reopened.getMuteCheck().isChecked());
    assertEquals("75", reopened.getFpsText().getText());
    assertTrue(reopened.getFullScreenCheck().isChecked());
    assertFalse(reopened.getVsyncCheck().isChecked());
    verify(settingsFile, times(2)).writeString(anyString(), eq(false));
  }

  @Test
  void disposalFlushesAnUnfinishedDragExactlyOnce() {
    startDrag(display.getSettingsPanel().getMasterVolumeSlider());
    assertEquals(0.8f, UserSettings.get().masterVolume);

    display.dispose();
    display.dispose();

    assertEquals(0f, UserSettings.get().masterVolume);
    assertEquals(75, UserSettings.get().fps);
    assertNull(display.getSettingsPanel());
    verify(settingsFile).writeString(anyString(), eq(false));
  }

  @Test
  void failedBackKeepsSettingsOpenUntilAudioSaveCanBeRetried() {
    SettingsPanel panel = display.getSettingsPanel();
    doThrow(new GdxRuntimeException("Simulated storage failure"))
        .when(settingsFile)
        .writeString(anyString(), eq(false));
    panel.getMusicVolumeSlider().setValue(0.2f);
    panel.getBackButton().fire(new ChangeListener.ChangeEvent());

    verify(game, never()).setScreen(GdxGame.ScreenType.MAIN_MENU);
    assertEquals(panel, display.getSettingsPanel());
    Label feedback = panel.findActor("settings-feedback");
    assertEquals(
        "Audio updated, but could not be saved. Try again.", feedback.getText().toString());
    assertEquals(0.6f, UserSettings.get().musicVolume);
    doCallRealMethod().when(settingsFile).writeString(anyString(), eq(false));
    panel.getBackButton().fire(new ChangeListener.ChangeEvent());
    display.dispose();

    assertEquals(0.2f, UserSettings.get().musicVolume, 0.0001f);
    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
    verify(settingsFile, times(3)).writeString(anyString(), eq(false));
  }

  @Test
  void forcedDisposalMakesOneFinalAttemptWhenSavingFails() {
    startDrag(display.getSettingsPanel().getMasterVolumeSlider());
    doThrow(new GdxRuntimeException("Simulated storage failure"))
        .when(settingsFile)
        .writeString(anyString(), eq(false));

    display.dispose();
    display.dispose();

    assertNull(display.getSettingsPanel());
    assertEquals(0.8f, UserSettings.get().masterVolume);
    verify(settingsFile).writeString(anyString(), eq(false));
  }

  private void createDisplay() {
    display = new SettingsMenuDisplay(game);
    new Entity().addComponent(display).create();
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

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
