package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.services.AudioSettingsApplier;
import com.csse3200.game.services.ServiceLocator;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class UserSettingsTest {
  @TempDir Path temporaryDirectory;

  @Test
  void shouldUseAudioDefaults() {
    Settings settings = new Settings();

    assertEquals(1f, settings.masterVolume);
    assertEquals(1f, settings.musicVolume);
    assertEquals(1f, settings.soundEffectsVolume);
    assertFalse(settings.muted);
  }

  @Test
  void shouldCalculateEffectiveAudioVolumes() {
    Settings settings = new Settings();
    settings.masterVolume = 0.5f;
    settings.musicVolume = 0.8f;
    settings.soundEffectsVolume = 0.4f;

    assertEquals(0.4f, settings.getEffectiveMusicVolume(), 0.0001f);
    assertEquals(0.2f, settings.getEffectiveSoundEffectsVolume(), 0.0001f);

    settings.muted = true;
    assertEquals(0f, settings.getEffectiveMusicVolume());
    assertEquals(0f, settings.getEffectiveSoundEffectsVolume());
    assertEquals(0.8f, settings.musicVolume);
    assertEquals(0.4f, settings.soundEffectsVolume);
  }

  @Test
  void shouldNormaliseAudioVolumesBeforeApplying() {
    Gdx.graphics = mock(Graphics.class);
    Settings settings = new Settings();
    settings.masterVolume = -2f;
    settings.musicVolume = 4f;
    settings.soundEffectsVolume = Float.NaN;

    UserSettings.applySettings(settings);

    assertEquals(0f, settings.masterVolume);
    assertEquals(1f, settings.musicVolume);
    assertEquals(1f, settings.soundEffectsVolume);
  }

  @Test
  void shouldApplyEffectiveVolumesToRegisteredAudioSystem() {
    Gdx.graphics = mock(Graphics.class);
    AudioSettingsApplier applier = mock(AudioSettingsApplier.class);
    withExternalSettingsFile(
        () -> {
          ServiceLocator.registerAudioSettingsApplier(applier);
          return null;
        });
    verify(applier).applyVolumes(1f, 1f);
    clearInvocations(applier);
    Settings settings = new Settings();
    settings.masterVolume = 0.5f;
    settings.musicVolume = 0.6f;
    settings.soundEffectsVolume = 0.2f;

    UserSettings.applySettings(settings);

    verify(applier).applyVolumes(0.3f, 0.1f);
  }

  @Test
  void shouldPersistAudioAndDisplaySettings() {
    withExternalSettingsFile(
        () -> {
          Settings settings = new Settings();
          settings.masterVolume = 0.75f;
          settings.musicVolume = 0.5f;
          settings.soundEffectsVolume = 0.25f;
          settings.muted = true;
          settings.fps = 90;
          settings.fullscreen = true;
          settings.vsync = false;

          UserSettings.set(settings, false);
          Settings loaded = UserSettings.get();

          assertEquals(0.75f, loaded.masterVolume);
          assertEquals(0.5f, loaded.musicVolume);
          assertEquals(0.25f, loaded.soundEffectsVolume);
          assertEquals(true, loaded.muted);
          assertEquals(90, loaded.fps);
          assertEquals(true, loaded.fullscreen);
          assertEquals(false, loaded.vsync);
          return null;
        });
  }

  @Test
  void shouldLoadAudioDefaultsFromOlderSettingsFile() {
    withExternalSettingsFile(
        () -> {
          Gdx.files
              .external("DECO2800Game/settings.json")
              .writeString("{fps: 75, fullscreen: true, vsync: false}", false);

          Settings loaded = UserSettings.get();

          assertEquals(1f, loaded.masterVolume);
          assertEquals(1f, loaded.musicVolume);
          assertEquals(1f, loaded.soundEffectsVolume);
          assertFalse(loaded.muted);
          assertEquals(75, loaded.fps);
          assertEquals(true, loaded.fullscreen);
          assertEquals(false, loaded.vsync);
          return null;
        });
  }

  @Test
  void shouldRejectNullSettings() {
    assertThrows(IllegalArgumentException.class, () -> UserSettings.set(null, false));
    assertThrows(IllegalArgumentException.class, () -> UserSettings.applySettings(null));
  }

  private <T> T withExternalSettingsFile(java.util.concurrent.Callable<T> action) {
    Files originalFiles = Gdx.files;
    Files files = mock(Files.class);
    FileHandle settingsFile = new FileHandle(temporaryDirectory.resolve("settings.json").toFile());
    when(files.external(anyString())).thenReturn(settingsFile);
    Gdx.files = files;
    try {
      return action.call();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    } finally {
      Gdx.files = originalFiles;
    }
  }

  @Test
  void shouldApplySettings() {
    Gdx.graphics = mock(Graphics.class);
    DisplayMode displayMode = mock(DisplayMode.class);
    when(Gdx.graphics.getDisplayMode()).thenReturn(displayMode);

    Settings settings = new Settings();
    settings.vsync = true;
    settings.displayMode = null;
    settings.fullscreen = true;
    settings.fps = 40;
    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setForegroundFPS(settings.fps);
    verify(Gdx.graphics).setFullscreenMode(displayMode);
    verify(Gdx.graphics).setVSync(settings.vsync);
  }

  @Test
  void shouldFindMatchingDisplay() {
    Gdx.graphics = mock(Graphics.class);
    DisplayMode correctMode = new CustomDisplayMode(200, 100, 60, 0);
    DisplayMode[] displayModes = {
      new CustomDisplayMode(100, 200, 30, 0), new CustomDisplayMode(100, 200, 60, 0), correctMode
    };
    when(Gdx.graphics.getDisplayModes()).thenReturn(displayModes);

    Settings settings = new Settings();
    settings.displayMode = new DisplaySettings();
    settings.displayMode.height = 100;
    settings.displayMode.width = 200;
    settings.displayMode.refreshRate = 60;
    settings.fullscreen = true;
    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setFullscreenMode(correctMode);
  }

  /** This exists to make the constructor public */
  static class CustomDisplayMode extends DisplayMode {
    public CustomDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
