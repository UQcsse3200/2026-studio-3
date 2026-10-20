package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings.Settings;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SettingsPanelTest {
  private Skin skin;
  private Graphics originalGraphics;
  private Settings initial;
  private AtomicInteger backCount;
  private AtomicReference<Settings> written;
  private AtomicInteger writeCount;

  @BeforeEach
  void setUp() {
    originalGraphics = Gdx.graphics;
    DisplayMode activeMode = new TestDisplayMode(1280, 800, 60, 32);
    Graphics graphics = mock(Graphics.class);
    when(graphics.getDisplayMode()).thenReturn(activeMode);
    when(graphics.getDisplayModes(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new DisplayMode[] {activeMode, new TestDisplayMode(1920, 1080, 60, 32)});
    Gdx.graphics = graphics;

    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    initial = new Settings();
    initial.masterVolume = 0.8f;
    initial.musicVolume = 0.6f;
    initial.soundEffectsVolume = 0.4f;
    initial.muted = true;
    initial.fps = 75;
    initial.fullscreen = true;
    initial.vsync = false;
    backCount = new AtomicInteger();
    written = new AtomicReference<>();
    writeCount = new AtomicInteger();
  }

  @AfterEach
  void tearDown() {
    try {
      if (skin != null) {
        skin.dispose();
      }
    } finally {
      Gdx.graphics = originalGraphics;
    }
  }

  @Test
  void initialControlsReflectCurrentSettings() {
    SettingsPanel panel = createPanel();

    assertEquals(0.8f, panel.getMasterVolumeSlider().getValue(), 0.001f);
    assertEquals(0.6f, panel.getMusicVolumeSlider().getValue(), 0.001f);
    assertEquals(0.4f, panel.getSoundEffectsVolumeSlider().getValue(), 0.001f);
    assertTrue(panel.getMuteCheck().isChecked());
    assertEquals("75", panel.getFpsText().getText());
    assertTrue(panel.getFullScreenCheck().isChecked());
    assertFalse(panel.getVsyncCheck().isChecked());
    assertNull(panel.findActor("ui-scale"));
    assertEquals("", feedback(panel).getText().toString());
  }

  @Test
  void applyWritesEverySelectedValueWithoutNavigatingBack() {
    SettingsPanel panel = createPanel();
    panel.getMasterVolumeSlider().setValue(0.5f);
    panel.getMusicVolumeSlider().setValue(0.3f);
    panel.getSoundEffectsVolumeSlider().setValue(0.2f);
    panel.getMuteCheck().setChecked(false);
    panel.getFpsText().setText("120");
    panel.getFullScreenCheck().setChecked(false);
    panel.getVsyncCheck().setChecked(true);

    click(panel.getApplyButton());

    Settings saved = written.get();
    assertEquals(0.5f, saved.masterVolume, 0.001f);
    assertEquals(0.3f, saved.musicVolume, 0.001f);
    assertEquals(0.2f, saved.soundEffectsVolume, 0.001f);
    assertFalse(saved.muted);
    assertEquals(120, saved.fps);
    assertFalse(saved.fullscreen);
    assertTrue(saved.vsync);
    assertEquals(0, backCount.get());
  }

  @Test
  void resetStagesDefaultsAndBackDiscardsWithoutWriting() {
    SettingsPanel panel = createPanel();

    click(panel.getResetButton());

    assertEquals(1f, panel.getMasterVolumeSlider().getValue());
    assertEquals(1f, panel.getMusicVolumeSlider().getValue());
    assertEquals(1f, panel.getSoundEffectsVolumeSlider().getValue());
    assertFalse(panel.getMuteCheck().isChecked());
    assertEquals("60", panel.getFpsText().getText());
    assertFalse(panel.getFullScreenCheck().isChecked());
    assertTrue(panel.getVsyncCheck().isChecked());
    assertNull(written.get());
    assertEquals("Defaults restored. Click Apply to save.", feedback(panel).getText().toString());

    click(panel.getBackButton());
    assertEquals(1, backCount.get());
    assertNull(written.get());
    assertEquals(0, writeCount.get());
    assertEquals(75, initial.fps);
    assertEquals(0.8f, initial.masterVolume, 0.001f);
    assertTrue(initial.muted);
  }

  @Test
  void resetReplacesErrorAndSuccessFeedbackWithoutSaving() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setText("-1");
    click(panel.getApplyButton());
    assertEquals(
        "Invalid FPS. Enter a positive whole number.", feedback(panel).getText().toString());

    click(panel.getResetButton());
    assertEquals("Defaults restored. Click Apply to save.", feedback(panel).getText().toString());
    assertEquals("60", panel.getFpsText().getText());
    assertEquals(0, writeCount.get());

    click(panel.getApplyButton());
    Settings saved = written.get();
    assertEquals("Settings applied.", feedback(panel).getText().toString());
    // Repeated reset must show the reminder even when the controls already contain defaults.
    click(panel.getResetButton());
    click(panel.getResetButton());
    assertEquals("Defaults restored. Click Apply to save.", feedback(panel).getText().toString());
    assertEquals(1, writeCount.get());
    assertSame(saved, written.get());
  }

  @Test
  void resetThenApplySavesAudioAndDisplayDefaultsOnce() {
    SettingsPanel panel = createPanel();
    click(panel.getResetButton());
    assertEquals(0, writeCount.get());

    click(panel.getApplyButton());

    Settings saved = written.get();
    assertEquals(60, saved.fps);
    assertEquals(1f, saved.masterVolume);
    assertEquals(1f, saved.musicVolume);
    assertEquals(1f, saved.soundEffectsVolume);
    assertFalse(saved.muted);
    assertFalse(saved.fullscreen);
    assertTrue(saved.vsync);
    // Preserve the existing Reset behaviour: select the current display mode.
    assertEquals(1280, saved.displayMode.width);
    assertEquals(800, saved.displayMode.height);
    assertEquals(60, saved.displayMode.refreshRate);
    assertEquals(1, writeCount.get());
    assertEquals(0, backCount.get());
    assertEquals("Settings applied.", feedback(panel).getText().toString());
  }

  @Test
  void editsAfterResetClearReminderAndApplyWithRemainingDefaults() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setProgrammaticChangeEvents(true);
    click(panel.getResetButton());

    panel.getFpsText().setText("120");
    assertEquals("", feedback(panel).getText().toString());
    click(panel.getResetButton());
    panel.getMusicVolumeSlider().setValue(0.3f);
    assertEquals("", feedback(panel).getText().toString());
    panel.getFpsText().setText("120");
    assertEquals(0, writeCount.get());
    click(panel.getApplyButton());

    Settings saved = written.get();
    assertEquals(120, saved.fps);
    assertEquals(0.3f, saved.musicVolume, 0.001f);
    assertEquals(1f, saved.masterVolume);
    assertEquals(1f, saved.soundEffectsVolume);
    assertFalse(saved.muted);
    assertFalse(saved.fullscreen);
    assertTrue(saved.vsync);
    assertEquals(1, writeCount.get());
    assertEquals("Settings applied.", feedback(panel).getText().toString());
  }

  @Test
  void invalidFpsBlocksAllEditsAndPreservesDraft() {
    SettingsPanel panel = createPanel();
    panel.getMasterVolumeSlider().setValue(0.5f);
    panel.getFullScreenCheck().setChecked(false);

    for (String input :
        new String[] {"-60", "0", "not-a-number", "", " ", "60.5", "+60", "2147483648"}) {
      panel.getFpsText().setText(input);
      click(panel.getApplyButton());

      assertNull(written.get(), input);
      assertEquals(0, writeCount.get(), input);
      assertEquals(input, panel.getFpsText().getText());
      assertEquals(0.5f, panel.getMasterVolumeSlider().getValue(), 0.001f);
      assertFalse(panel.getFullScreenCheck().isChecked());
      assertEquals(75, initial.fps);
      assertEquals(0.8f, initial.masterVolume, 0.001f);
      assertTrue(initial.fullscreen);
      assertEquals(
          "Invalid FPS. Enter a positive whole number.", feedback(panel).getText().toString());
      assertEquals(Color.SALMON, feedback(panel).getStyle().fontColor);
    }
    assertEquals(0, backCount.get());
  }

  @Test
  void correctingInvalidFpsWritesAllPendingEditsExactlyOnce() {
    SettingsPanel panel = createPanel();
    panel.getMasterVolumeSlider().setValue(0.5f);
    panel.getMusicVolumeSlider().setValue(0.3f);
    panel.getSoundEffectsVolumeSlider().setValue(0.2f);
    panel.getMuteCheck().setChecked(false);
    panel.getFullScreenCheck().setChecked(false);
    panel.getVsyncCheck().setChecked(true);
    panel.getFpsText().setText("-60");
    click(panel.getApplyButton());
    assertEquals(0, writeCount.get());

    panel.getFpsText().setText(" 00120 ");
    click(panel.getApplyButton());

    assertEquals(1, writeCount.get());
    Settings saved = written.get();
    assertEquals(120, saved.fps);
    assertEquals(0.5f, saved.masterVolume, 0.001f);
    assertEquals(0.3f, saved.musicVolume, 0.001f);
    assertEquals(0.2f, saved.soundEffectsVolume, 0.001f);
    assertFalse(saved.muted);
    assertFalse(saved.fullscreen);
    assertTrue(saved.vsync);
    assertEquals("120", panel.getFpsText().getText());
    assertEquals("Settings applied.", feedback(panel).getText().toString());
    assertEquals(Color.GREEN, feedback(panel).getStyle().fontColor);
    assertEquals(0, backCount.get());
  }

  @Test
  void invalidApplyAfterSuccessLeavesLastSavedSettingsUnchanged() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setText("120");
    click(panel.getApplyButton());
    Settings saved = written.get();

    panel.getFpsText().setText("-1");
    panel.getMasterVolumeSlider().setValue(0.2f);
    click(panel.getApplyButton());

    assertEquals(1, writeCount.get());
    assertSame(saved, written.get());
    assertEquals(120, saved.fps);
    assertEquals(0.8f, saved.masterVolume, 0.001f);
    assertEquals("-1", panel.getFpsText().getText());
  }

  private SettingsPanel createPanel() {
    return new SettingsPanel(
        skin,
        backCount::incrementAndGet,
        () -> initial,
        settings -> {
          writeCount.incrementAndGet();
          written.set(settings);
        });
  }

  @Test
  void editingAnyControlClearsSuccessFeedback() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setProgrammaticChangeEvents(true);
    SelectBox<?> resolution = panel.findActor("resolution");
    Runnable[] edits = {
      () -> panel.getFpsText().setText("120"),
      () -> panel.getFullScreenCheck().setChecked(false),
      () -> panel.getVsyncCheck().setChecked(true),
      () -> resolution.setSelectedIndex(1),
      () -> panel.getMuteCheck().setChecked(false),
      () -> panel.getMasterVolumeSlider().setValue(0.5f),
      () -> panel.getMusicVolumeSlider().setValue(0.3f),
      () -> panel.getSoundEffectsVolumeSlider().setValue(0.2f)
    };
    for (Runnable edit : edits) {
      click(panel.getApplyButton());
      assertEquals("Settings applied.", feedback(panel).getText().toString());
      edit.run();
      assertEquals("", feedback(panel).getText().toString());
    }
  }

  @Test
  void editingFpsClearsValidationErrorBeforeApplyingCorrection() {
    SettingsPanel panel = createPanel();
    // Programmatic edits simulate the ChangeEvents generated by user typing.
    panel.getFpsText().setProgrammaticChangeEvents(true);
    panel.getFpsText().setText("-1");
    click(panel.getApplyButton());
    assertEquals(
        "Invalid FPS. Enter a positive whole number.", feedback(panel).getText().toString());

    panel.getFpsText().setText("120");
    assertEquals("", feedback(panel).getText().toString());
    assertEquals(0, writeCount.get());
    click(panel.getApplyButton());
    assertEquals("Settings applied.", feedback(panel).getText().toString());
    assertEquals(1, writeCount.get());
  }

  @Test
  void failedWritePreservesDraftAndAllowsRetry() {
    AtomicBoolean failWrite = new AtomicBoolean(false);
    SettingsPanel panel =
        new SettingsPanel(
            skin,
            backCount::incrementAndGet,
            () -> initial,
            settings -> {
              writeCount.incrementAndGet();
              if (failWrite.get()) {
                throw new IllegalStateException("Simulated write failure");
              }
              written.set(settings);
            });
    click(panel.getApplyButton());
    Settings saved = written.get();
    assertEquals("Settings applied.", feedback(panel).getText().toString());
    panel.getFpsText().setText(" 00120 ");
    panel.getMasterVolumeSlider().setValue(0.5f);
    failWrite.set(true);

    click(panel.getApplyButton());

    assertEquals(
        "Could not apply settings. Please try again.", feedback(panel).getText().toString());
    assertEquals(Color.SALMON, feedback(panel).getStyle().fontColor);
    assertSame(saved, written.get());
    assertEquals(" 00120 ", panel.getFpsText().getText());
    assertEquals(0.5f, panel.getMasterVolumeSlider().getValue(), 0.001f);
    assertEquals(0, backCount.get());

    panel.getMusicVolumeSlider().setValue(0.3f);
    assertEquals("", feedback(panel).getText().toString());
    failWrite.set(false);
    click(panel.getApplyButton());
    assertEquals(3, writeCount.get());
    assertEquals(120, written.get().fps);
    assertEquals(0.5f, written.get().masterVolume, 0.001f);
    assertEquals(0.3f, written.get().musicVolume, 0.001f);
    assertEquals("Settings applied.", feedback(panel).getText().toString());
  }

  private static Label feedback(SettingsPanel panel) {
    return panel.findActor("settings-feedback");
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
