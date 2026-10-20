package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.services.ServiceLocator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class SettingsPanelAudioTest {
  private Graphics originalGraphics;
  private Skin skin;
  private Settings initial;
  private final List<Settings> audioUpdates = new ArrayList<>();
  private final List<Settings> displayUpdates = new ArrayList<>();
  private final List<Settings> writes = new ArrayList<>();
  private boolean failWrites;
  private boolean failDisplayApply;
  private int writeAttempts;
  private int backCount;

  @BeforeEach
  void setUp() {
    originalGraphics = Gdx.graphics;
    Graphics graphics = mock(Graphics.class);
    DisplayMode activeMode = new TestDisplayMode(1280, 800);
    when(graphics.getDisplayMode()).thenReturn(activeMode);
    when(graphics.getDisplayModes(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new DisplayMode[] {activeMode, new TestDisplayMode(1920, 1080)});
    Gdx.graphics = graphics;
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    initial = new Settings();
    initial.masterVolume = 0.8f;
    initial.musicVolume = 0.6f;
    initial.soundEffectsVolume = 0.4f;
    initial.fps = 75;
    initial.fullscreen = true;
    initial.vsync = false;
    initial.uiScale = 1.25f;
  }

  @AfterEach
  void tearDown() {
    try {
      skin.dispose();
    } finally {
      Gdx.graphics = originalGraphics;
    }
  }

  @Test
  void productionPanelPersistsAudioAndReloadsItWithoutApply(@TempDir Path folder) {
    Files originalFiles = Gdx.files;
    Gdx.files = spy(originalFiles);
    doReturn(new FileHandle(folder.resolve("settings.json").toFile()))
        .when(Gdx.files)
        .external(anyString());
    try {
      UserSettings.set(initial, false);
      List<float[]> effectiveUpdates = new ArrayList<>();
      ServiceLocator.registerAudioSettingsApplier(
          (music, effects) -> effectiveUpdates.add(new float[] {music, effects}));
      effectiveUpdates.clear();
      SettingsPanel panel = new SettingsPanel(skin, () -> {});
      panel.getFpsText().setText("invalid");
      panel.getFullScreenCheck().setChecked(false);

      panel.getMasterVolumeSlider().setValue(0.5f);

      assertEquals(0.3f, effectiveUpdates.get(0)[0], 0.0001f);
      assertEquals(0.2f, effectiveUpdates.get(0)[1], 0.0001f);
      panel.getMuteCheck().setChecked(true);
      assertEquals(0f, effectiveUpdates.get(1)[0]);
      assertEquals(0f, effectiveUpdates.get(1)[1]);
      Settings saved = UserSettings.get();
      assertEquals(0.5f, saved.masterVolume);
      assertEquals(0.6f, saved.musicVolume);
      assertEquals(0.4f, saved.soundEffectsVolume);
      assertTrue(saved.muted);
      assertEquals(75, saved.fps);
      assertTrue(saved.fullscreen);
      SettingsPanel reopened = new SettingsPanel(skin, () -> {});
      assertEquals(0.5f, reopened.getMasterVolumeSlider().getValue());
      assertTrue(reopened.getMuteCheck().isChecked());
      assertEquals("75", reopened.getFpsText().getText());
      assertEquals(2, effectiveUpdates.size());
    } finally {
      Gdx.files = originalFiles;
    }
  }

  @Test
  void slidersApplyAndSaveWithoutApplyButton() {
    SettingsPanel panel = createPanel();
    panel.getMasterVolumeSlider().setValue(0.5f);
    assertEffectiveVolumes(0.3f, 0.2f);
    panel.getMusicVolumeSlider().setValue(0.3f);
    assertEffectiveVolumes(0.15f, 0.2f);
    panel.getSoundEffectsVolumeSlider().setValue(0.2f);
    assertEffectiveVolumes(0.15f, 0.1f);

    assertEquals(3, audioUpdates.size());
    assertEquals(3, writes.size());
    assertTrue(displayUpdates.isEmpty());
    assertEquals(0.5f, saved().masterVolume, 0.0001f);
    assertEquals(0.3f, saved().musicVolume, 0.0001f);
    assertEquals(0.2f, saved().soundEffectsVolume, 0.0001f);
    assertEquals(0.8f, initial.masterVolume, 0.0001f);
  }

  @Test
  void mutePreservesSliderPreferencesAndUnmuteRestoresLatestLevels() {
    SettingsPanel panel = createPanel();
    panel.getMuteCheck().setChecked(true);
    assertEffectiveVolumes(0f, 0f);
    assertEquals(0.8f, saved().masterVolume, 0.0001f);
    assertEquals(0.6f, saved().musicVolume, 0.0001f);
    assertEquals(0.4f, saved().soundEffectsVolume, 0.0001f);
    panel.getMusicVolumeSlider().setValue(0.5f);
    assertEffectiveVolumes(0f, 0f);
    panel.getMuteCheck().setChecked(false);
    assertEffectiveVolumes(0.4f, 0.32f);
    assertEquals(3, writes.size());
    assertFalse(saved().muted);
    assertEquals(0.5f, panel.getMusicVolumeSlider().getValue(), 0.0001f);
  }

  @Test
  void audioSavesExcludeAllPendingDisplayChangesEvenWithInvalidFps() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setText("invalid");
    panel.getFullScreenCheck().setChecked(false);
    panel.getVsyncCheck().setChecked(true);
    SelectBox<?> resolution = panel.findActor("resolution");
    resolution.setSelectedIndex(1);
    panel.getMusicVolumeSlider().setValue(0.2f);
    panel.getMuteCheck().setChecked(true);

    for (Settings written : writes) {
      assertEquals(75, written.fps);
      assertTrue(written.fullscreen);
      assertFalse(written.vsync);
      assertNull(written.displayMode);
      assertEquals(1.25f, written.uiScale);
    }
    assertEquals("invalid", panel.getFpsText().getText());
    assertFalse(panel.getFullScreenCheck().isChecked());
    assertTrue(panel.getVsyncCheck().isChecked());
    assertEquals(1, resolution.getSelectedIndex());
    assertTrue(displayUpdates.isEmpty());
  }

  @Test
  void eachSliderUpdatesDuringDraggingButWritesOnlyOnRelease() {
    SettingsPanel panel = createPanel();
    for (Slider slider : sliders(panel)) {
      int previousWrites = writes.size();
      int previousUpdates = audioUpdates.size();
      slider.setSize(300f, 30f);
      InputListener input = sliderInput(slider);
      assertTrue(input.touchDown(new InputEvent(), 0f, 15f, 0, 0));
      input.touchDragged(new InputEvent(), 150f, 15f, 0);
      input.touchDragged(new InputEvent(), 300f, 15f, 0);
      assertTrue(slider.isDragging());
      assertEquals(previousWrites, writes.size());
      assertEquals(previousUpdates + 3, audioUpdates.size());
      assertEquals(1f, slider.getValue());

      input.touchUp(new InputEvent(), 300f, 15f, 0, 0);

      assertFalse(slider.isDragging());
      assertEquals(previousWrites + 1, writes.size());
      assertEquals(previousUpdates + 3, audioUpdates.size());
    }
    assertTrue(panel.flushPendingAudioChanges());
    assertEquals(3, writes.size());
  }

  @Test
  void returningToSavedValueDuringDragDoesNotWrite() {
    initial.masterVolume = 1f;
    SettingsPanel panel = createPanel();
    Slider slider = panel.getMasterVolumeSlider();
    slider.setSize(300f, 30f);
    InputListener input = sliderInput(slider);
    input.touchDown(new InputEvent(), 0f, 15f, 0, 0);
    input.touchDragged(new InputEvent(), 300f, 15f, 0);
    input.touchUp(new InputEvent(), 300f, 15f, 0, 0);

    assertEquals(2, audioUpdates.size());
    assertTrue(writes.isEmpty());
    Label feedback = panel.findActor("settings-feedback");
    assertEquals("Audio saved. Display changes require Apply.", feedback.getText().toString());
    assertTrue(panel.flushPendingAudioChanges());
  }

  @Test
  void cancelledTouchFlushesTheLastLiveValue() {
    SettingsPanel panel = createPanel();
    Slider slider = panel.getMasterVolumeSlider();
    slider.setSize(300f, 30f);
    InputListener input = sliderInput(slider);
    input.touchDown(new InputEvent(), 0f, 15f, 0, 0);
    InputEvent cancelled = new InputEvent();
    cancelled.setStageX(Integer.MIN_VALUE);
    cancelled.setStageY(Integer.MIN_VALUE);
    assertTrue(cancelled.isTouchFocusCancel());

    input.touchUp(cancelled, 300f, 15f, 0, 0);

    assertFalse(slider.isDragging());
    assertEquals(1, writes.size());
    assertEquals(0f, saved().masterVolume);
    assertEquals(1, audioUpdates.size());
  }

  @Test
  void duplicateEventsDoNotWriteOrApplyAgain() {
    SettingsPanel panel = createPanel();
    panel.getMasterVolumeSlider().setValue(0.5f);
    fireChange(panel.getMasterVolumeSlider());
    fireChange(panel.getMuteCheck());
    panel.flushPendingAudioChanges();

    assertEquals(1, writes.size());
    assertEquals(1, audioUpdates.size());
  }

  @Test
  void unchangedReleaseAndMuteDoNotRoundUntouchedPreferences() {
    initial.masterVolume = 0.73f;
    initial.musicVolume = 0.61f;
    SettingsPanel panel = createPanel();
    fireChange(panel.getMasterVolumeSlider());
    assertTrue(writes.isEmpty());
    assertTrue(audioUpdates.isEmpty());
    panel.getMuteCheck().setChecked(true);

    assertEquals(0.73f, saved().masterVolume);
    assertEquals(0.61f, saved().musicVolume);
    panel.getFpsText().setText("120");
    fireChange(panel.getApplyButton());
    assertEquals(0.73f, saved().masterVolume);
    assertEquals(0.61f, saved().musicVolume);
    assertEquals(1, audioUpdates.size());
  }

  @Test
  void applyPreservesSavedAudioAndDoesNotMutatePreviousPayloads() {
    SettingsPanel panel = createPanel();
    panel.getMusicVolumeSlider().setValue(0.2f);
    Settings audioSave = saved();
    panel.getFpsText().setText("120");
    fireChange(panel.getApplyButton());

    assertEquals(2, writes.size());
    assertEquals(120, saved().fps);
    assertEquals(0.2f, saved().musicVolume, 0.0001f);
    assertEquals(75, audioSave.fps);
    panel.getMuteCheck().setChecked(true);
    assertEquals(120, saved().fps);
    assertFalse(audioSave.muted);
    assertEquals(1, displayUpdates.size());
  }

  @Test
  void failedAudioSaveRetainsLiveAudioAndCanRetryWithoutRecursiveEvents() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setText("invalid");
    failWrites = true;
    panel.getMusicVolumeSlider().setValue(0.2f);
    assertEquals(1, writeAttempts);
    assertTrue(writes.isEmpty());
    assertEquals(1, audioUpdates.size());
    assertEffectiveVolumes(0.16f, 0.32f);
    Label feedback = panel.findActor("settings-feedback");
    assertEquals(
        "Audio updated, but could not be saved. Try again.", feedback.getText().toString());
    assertFalse(panel.flushPendingAudioChanges());
    assertEquals(2, writeAttempts);
    failWrites = false;

    assertTrue(panel.flushPendingAudioChanges());
    assertTrue(panel.flushPendingAudioChanges());

    assertEquals(3, writeAttempts);
    assertEquals(1, writes.size());
    assertEquals(1, audioUpdates.size());
    assertEquals(75, saved().fps);
    assertEquals("invalid", panel.getFpsText().getText());
  }

  @Test
  void applyFlushesAnUnfinishedDragEvenWhenFpsIsInvalid() {
    SettingsPanel panel = createPanel();
    Slider slider = panel.getMasterVolumeSlider();
    slider.setSize(300f, 30f);
    InputListener input = sliderInput(slider);
    input.touchDown(new InputEvent(), 0f, 15f, 0, 0);
    panel.getFpsText().setText("invalid");
    assertTrue(writes.isEmpty());

    fireChange(panel.getApplyButton());

    assertEquals(1, writes.size());
    assertEquals(0f, saved().masterVolume);
    assertEquals(75, saved().fps);
    assertTrue(displayUpdates.isEmpty());
    assertEquals(1, audioUpdates.size());
    assertEquals("Invalid FPS. Enter a positive whole number.", feedback(panel));
    input.touchUp(new InputEvent(), 0f, 15f, 0, 0);
    assertEquals(1, writes.size());
  }

  @Test
  void applyFlushesAudioSeparatelyBeforeSavingDisplay() {
    SettingsPanel panel = createPanel();
    Slider slider = panel.getMasterVolumeSlider();
    slider.setSize(300f, 30f);
    sliderInput(slider).touchDown(new InputEvent(), 0f, 15f, 0, 0);
    panel.getFpsText().setText(" 00120 ");

    fireChange(panel.getApplyButton());

    assertEquals(2, writes.size());
    assertEquals(75, writes.get(0).fps);
    assertEquals(0f, writes.get(0).masterVolume);
    assertEquals(120, saved().fps);
    assertEquals(0f, saved().masterVolume);
    assertEquals(1, audioUpdates.size());
    assertEquals(1, displayUpdates.size());
    assertEquals("120", panel.getFpsText().getText());
    assertEquals("Display settings applied and saved.", feedback(panel));
  }

  @Test
  void resetThenBackRetainsAudioDefaultsAndDiscardsDisplayDefaults() {
    initial.muted = true;
    SettingsPanel panel = createPanel();
    fireChange(panel.getResetButton());

    assertEffectiveVolumes(1f, 1f);
    assertEquals(1, audioUpdates.size());
    assertEquals(1, writes.size());
    assertEquals(75, saved().fps);
    assertTrue(saved().fullscreen);
    assertFalse(saved().vsync);
    assertNull(saved().displayMode);
    assertEquals("60", panel.getFpsText().getText());
    assertTrue(displayUpdates.isEmpty());

    fireChange(panel.getBackButton());

    assertEquals(1, backCount);
    assertEquals(1, writes.size());
    Settings saved = saved();
    SettingsPanel reopened =
        new SettingsPanel(
            skin, () -> {}, () -> saved, writes::add, audioUpdates::add, displayUpdates::add);
    assertEquals("75", reopened.getFpsText().getText());
    assertTrue(reopened.getFullScreenCheck().isChecked());
    assertFalse(reopened.getVsyncCheck().isChecked());
    assertEquals(1f, reopened.getMasterVolumeSlider().getValue());
    assertEquals(1f, reopened.getMusicVolumeSlider().getValue());
    assertEquals(1f, reopened.getSoundEffectsVolumeSlider().getValue());
    assertFalse(reopened.getMuteCheck().isChecked());
  }

  @Test
  void failedResetKeepsAudioLiveAndBackWaitsForSuccessfulRetry() {
    SettingsPanel panel = createPanel();
    failWrites = true;
    fireChange(panel.getResetButton());
    assertEffectiveVolumes(1f, 1f);
    assertTrue(writes.isEmpty());
    assertEquals("Audio updated, but could not be saved. Try again.", feedback(panel));

    fireChange(panel.getBackButton());
    assertEquals(0, backCount);
    assertEquals(2, writeAttempts);
    assertEquals("60", panel.getFpsText().getText());
    assertTrue(displayUpdates.isEmpty());
    failWrites = false;
    panel.requestBack();

    assertEquals(1, backCount);
    assertEquals(3, writeAttempts);
    assertEquals(1, writes.size());
    assertEquals(1f, saved().masterVolume);
    assertEquals(75, saved().fps);
    assertEquals(1, audioUpdates.size());
  }

  @Test
  void backFlushesAnUnfinishedDragWithoutValidatingOrSavingDisplayDrafts() {
    SettingsPanel panel = createPanel();
    Slider slider = panel.getMasterVolumeSlider();
    slider.setSize(300f, 30f);
    sliderInput(slider).touchDown(new InputEvent(), 0f, 15f, 0, 0);
    panel.getFpsText().setText("invalid");
    panel.getFullScreenCheck().setChecked(false);

    panel.requestBack();

    assertEquals(1, writes.size());
    assertEquals(0f, saved().masterVolume);
    assertEquals(75, saved().fps);
    assertTrue(saved().fullscreen);
    assertEquals(1, backCount);
    assertTrue(displayUpdates.isEmpty());
  }

  @Test
  void displaySaveFailurePreservesDraftAndRetryDoesNotReapplyAudio() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setText(" 00120 ");
    panel.getFullScreenCheck().setChecked(false);
    failWrites = true;

    fireChange(panel.getApplyButton());

    assertEquals("Could not save display settings. Please try again.", feedback(panel));
    assertEquals(" 00120 ", panel.getFpsText().getText());
    assertTrue(writes.isEmpty());
    assertTrue(displayUpdates.isEmpty());
    assertTrue(audioUpdates.isEmpty());
    failWrites = false;
    fireChange(panel.getApplyButton());
    assertEquals(2, writeAttempts);
    assertEquals(120, saved().fps);
    assertFalse(saved().fullscreen);
    assertEquals("120", panel.getFpsText().getText());
    assertEquals(1, displayUpdates.size());
    assertTrue(audioUpdates.isEmpty());
  }

  @Test
  void displayApplicationFailureKeepsSavedBaselineForAudioUpdatesAndRetry() {
    SettingsPanel panel = createPanel();
    panel.getFpsText().setText("120");
    panel.getFullScreenCheck().setChecked(false);
    failDisplayApply = true;

    fireChange(panel.getApplyButton());

    assertEquals("Display settings saved, but could not be applied.", feedback(panel));
    assertEquals(1, writes.size());
    assertTrue(audioUpdates.isEmpty());
    panel.getMusicVolumeSlider().setValue(0.2f);
    assertEquals(120, saved().fps);
    assertFalse(saved().fullscreen);
    assertEquals(0.2f, saved().musicVolume, 0.0001f);
    failDisplayApply = false;
    fireChange(panel.getApplyButton());
    assertEquals(3, writes.size());
    assertEquals(1, displayUpdates.size());
    assertEquals(1, audioUpdates.size());
    assertEquals("Display settings applied and saved.", feedback(panel));
  }

  private SettingsPanel createPanel() {
    return new SettingsPanel(
        skin,
        () -> backCount++,
        () -> initial,
        settings -> {
          writeAttempts++;
          if (failWrites) {
            throw new IllegalStateException("Simulated storage failure");
          }
          writes.add(settings);
        },
        audioUpdates::add,
        settings -> {
          if (failDisplayApply) {
            throw new IllegalStateException("Simulated display failure");
          }
          displayUpdates.add(settings);
        });
  }

  private static String feedback(SettingsPanel panel) {
    Label label = panel.findActor("settings-feedback");
    return label.getText().toString();
  }

  private Settings saved() {
    return writes.get(writes.size() - 1);
  }

  private void assertEffectiveVolumes(float music, float effects) {
    Settings latest = audioUpdates.get(audioUpdates.size() - 1);
    assertEquals(music, latest.getEffectiveMusicVolume(), 0.0001f);
    assertEquals(effects, latest.getEffectiveSoundEffectsVolume(), 0.0001f);
  }

  private static List<Slider> sliders(SettingsPanel panel) {
    return List.of(
        panel.getMasterVolumeSlider(),
        panel.getMusicVolumeSlider(),
        panel.getSoundEffectsVolumeSlider());
  }

  private static InputListener sliderInput(Slider slider) {
    for (EventListener listener : slider.getListeners()) {
      if (listener instanceof InputListener input) {
        return input;
      }
    }
    throw new AssertionError("Slider input listener missing");
  }

  private static void fireChange(Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height) {
      super(width, height, 60, 32);
    }
  }
}
