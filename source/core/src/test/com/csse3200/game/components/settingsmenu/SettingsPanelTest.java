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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings.Settings;
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

    click(panel.getBackButton());
    assertEquals(1, backCount.get());
    assertNull(written.get());
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

  private static void click(Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
