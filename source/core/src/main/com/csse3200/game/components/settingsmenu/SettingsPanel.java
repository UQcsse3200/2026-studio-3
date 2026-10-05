package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.utils.StringDecorator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reusable settings controls for both the standalone screen and in-game pause overlay. */
public class SettingsPanel extends Table {
  private static final Logger logger = LoggerFactory.getLogger(SettingsPanel.class);
  private static final float VOLUME_STEP = 0.05f;

  private final Skin skin;
  private final Runnable backAction;
  private final Consumer<Settings> settingsWriter;
  private Settings appliedSettings;

  private Slider masterVolumeSlider;
  private Slider musicVolumeSlider;
  private Slider soundEffectsVolumeSlider;
  private Label masterVolumeValue;
  private Label musicVolumeValue;
  private Label soundEffectsVolumeValue;
  private CheckBox muteCheck;
  private TextField fpsText;
  private CheckBox fullScreenCheck;
  private CheckBox vsyncCheck;
  private SelectBox<StringDecorator<DisplayMode>> displayModeSelect;
  private TextButton resetButton;
  private TextButton backButton;
  private TextButton applyButton;
  private Label feedbackLabel;

  /** Creates a panel backed by the global user-settings file. */
  public SettingsPanel(Skin skin, Runnable backAction) {
    this(skin, backAction, UserSettings::get, settings -> UserSettings.set(settings, true));
  }

  SettingsPanel(
      Skin skin,
      Runnable backAction,
      Supplier<Settings> settingsLoader,
      Consumer<Settings> settingsWriter) {
    this.skin = skin;
    this.backAction = backAction;
    this.settingsWriter = settingsWriter;
    appliedSettings = copyOf(settingsLoader.get());
    build();
    populate(appliedSettings);
  }

  private void build() {
    setName("settings-panel");
    setBackground(skin.newDrawable("white", new Color(0.105f, 0.07f, 0.065f, 0.94f)));
    pad(24f, 38f, 24f, 38f);
    defaults().pad(4f);

    add(themedLabel("Settings", "title")).colspan(3).padBottom(8f).row();

    addSectionTitle("Audio");
    masterVolumeSlider = volumeSlider("master-volume");
    masterVolumeValue = themedLabel("100%");
    addControlRow("Master Volume", masterVolumeSlider, masterVolumeValue);

    musicVolumeSlider = volumeSlider("music-volume");
    musicVolumeValue = themedLabel("100%");
    addControlRow("Music Volume", musicVolumeSlider, musicVolumeValue);

    soundEffectsVolumeSlider = volumeSlider("sound-effects-volume");
    soundEffectsVolumeValue = themedLabel("100%");
    addControlRow("Sound Effects", soundEffectsVolumeSlider, soundEffectsVolumeValue);

    muteCheck = new CheckBox("", skin);
    muteCheck.setName("mute");
    addControlRow("Mute", muteCheck, null);

    addSectionTitle("Display");
    fpsText = new TextField("60", skin);
    fpsText.setName("fps-cap");
    addControlRow("FPS Cap", fpsText, null);

    fullScreenCheck = new CheckBox("", skin);
    fullScreenCheck.setName("fullscreen");
    addControlRow("Fullscreen", fullScreenCheck, null);

    vsyncCheck = new CheckBox("", skin);
    vsyncCheck.setName("vsync");
    addControlRow("VSync", vsyncCheck, null);

    displayModeSelect = new SelectBox<>(skin);
    displayModeSelect.setName("resolution");
    displayModeSelect.setItems(getDisplayModes(Gdx.graphics.getMonitor()));
    addControlRow("Resolution", displayModeSelect, null);

    feedbackLabel = themedLabel("");
    feedbackLabel.setName("settings-feedback");
    feedbackLabel.setWrap(true);
    feedbackLabel.setAlignment(Align.center);
    add(feedbackLabel).colspan(3).growX().minHeight(40f).padTop(8f).row();

    resetButton = themedButton("Reset", "reset-defaults");
    backButton = themedButton("Back", "back");
    applyButton = themedButton("Apply", "apply");
    wireActions();

    Table buttons = new Table();
    buttons.defaults().height(58f).pad(5f);
    buttons.add(resetButton).width(210f);
    buttons.add(backButton).width(145f);
    buttons.add(applyButton).width(145f);
    add(buttons).colspan(3).padTop(8f);
  }

  private void addSectionTitle(String text) {
    add(themedLabel(text, "large")).colspan(3).left().padTop(5f).row();
  }

  private void addControlRow(String label, Actor control, Actor value) {
    add(themedLabel(label)).right().padRight(14f);
    add(control).width(control instanceof CheckBox ? 40f : 300f).left();
    if (value == null) {
      add().width(60f);
    } else {
      add(value).width(60f).left();
    }
    row();
  }

  private Slider volumeSlider(String name) {
    Slider slider = new Slider(0f, 1f, VOLUME_STEP, false, skin);
    slider.setName(name);
    return slider;
  }

  private void wireActions() {
    addVolumeListener(masterVolumeSlider, masterVolumeValue);
    addVolumeListener(musicVolumeSlider, musicVolumeValue);
    addVolumeListener(soundEffectsVolumeSlider, soundEffectsVolumeValue);

    ChangeListener clearFeedback =
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            feedbackLabel.setText("");
          }
        };
    for (Actor control :
        new Actor[] {
          fpsText,
          fullScreenCheck,
          vsyncCheck,
          displayModeSelect,
          muteCheck,
          masterVolumeSlider,
          musicVolumeSlider,
          soundEffectsVolumeSlider
        }) {
      control.addListener(clearFeedback);
    }

    resetButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            populate(new Settings());
            showFeedback("Defaults restored. Click Apply to save.", MenuTheme.warmParchment());
          }
        });
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            backAction.run();
          }
        });
    applyButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            applyChanges();
          }
        });
  }

  private void addVolumeListener(Slider slider, Label valueLabel) {
    slider.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            updateVolumeLabel(slider, valueLabel);
          }
        });
  }

  private void populate(Settings settings) {
    masterVolumeSlider.setValue(settings.masterVolume);
    musicVolumeSlider.setValue(settings.musicVolume);
    soundEffectsVolumeSlider.setValue(settings.soundEffectsVolume);
    updateVolumeLabel(masterVolumeSlider, masterVolumeValue);
    updateVolumeLabel(musicVolumeSlider, musicVolumeValue);
    updateVolumeLabel(soundEffectsVolumeSlider, soundEffectsVolumeValue);
    muteCheck.setChecked(settings.muted);
    fpsText.setText(Integer.toString(settings.fps));
    fullScreenCheck.setChecked(settings.fullscreen);
    vsyncCheck.setChecked(settings.vsync);
    selectDisplayMode(settings.displayMode);
  }

  private void applyChanges() {
    Integer parsedFps = FpsValidator.parse(fpsText.getText());
    if (parsedFps == null) {
      showFeedback("Invalid FPS. Enter a positive whole number.", Color.SALMON);
      return;
    }

    Settings updated = copyOf(appliedSettings);
    updated.fps = parsedFps;
    updated.masterVolume = masterVolumeSlider.getValue();
    updated.musicVolume = musicVolumeSlider.getValue();
    updated.soundEffectsVolume = soundEffectsVolumeSlider.getValue();
    updated.muted = muteCheck.isChecked();
    updated.fullscreen = fullScreenCheck.isChecked();
    updated.vsync = vsyncCheck.isChecked();

    StringDecorator<DisplayMode> selectedMode = displayModeSelect.getSelected();
    if (selectedMode != null) {
      updated.displayMode = new DisplaySettings(selectedMode.object);
    }

    try {
      settingsWriter.accept(updated);
    } catch (RuntimeException e) {
      logger.error("Could not apply settings", e);
      showFeedback("Could not apply settings. Please try again.", Color.SALMON);
      return;
    }

    appliedSettings = copyOf(updated);
    populate(appliedSettings);
    showFeedback("Settings applied.", Color.GREEN);
  }

  private void showFeedback(String message, Color color) {
    feedbackLabel.setText(message);
    // Set the label's own font colour
    feedbackLabel.getStyle().fontColor = new Color(color);
  }

  private void selectDisplayMode(DisplaySettings desired) {
    StringDecorator<DisplayMode> selected = findDisplayMode(desired);
    DisplayMode active = Gdx.graphics.getDisplayMode();
    if (selected == null && active != null) {
      selected = findDisplayMode(new DisplaySettings(active));
    }
    if (selected != null) {
      displayModeSelect.setSelected(selected);
    }
  }

  private StringDecorator<DisplayMode> findDisplayMode(DisplaySettings desired) {
    if (desired == null) {
      return null;
    }
    for (StringDecorator<DisplayMode> candidate : displayModeSelect.getItems()) {
      DisplayMode mode = candidate.object;
      if (mode.width == desired.width
          && mode.height == desired.height
          && mode.refreshRate == desired.refreshRate) {
        return candidate;
      }
    }
    return null;
  }

  private Array<StringDecorator<DisplayMode>> getDisplayModes(Monitor monitor) {
    Array<StringDecorator<DisplayMode>> modes = new Array<>();
    DisplayMode[] availableModes = Gdx.graphics.getDisplayModes(monitor);
    if (availableModes != null) {
      for (DisplayMode mode : availableModes) {
        modes.add(new StringDecorator<>(mode, SettingsPanel::prettyPrint));
      }
    }
    return modes;
  }

  private static String prettyPrint(DisplayMode displayMode) {
    return displayMode.width + "x" + displayMode.height + ", " + displayMode.refreshRate + "hz";
  }

  private void updateVolumeLabel(Slider slider, Label label) {
    label.setText(Math.round(slider.getValue() * 100f) + "%");
  }

  private Label themedLabel(String text) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    style.fontColor = MenuTheme.warmParchment();
    return new Label(text, style);
  }

  private Label themedLabel(String text, String styleName) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(styleName, Label.LabelStyle.class));
    style.fontColor = MenuTheme.warmParchment();
    return new Label(text, style);
  }

  private TextButton themedButton(String text, String name) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null
        && resources.containsAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class)) {
      style =
          MenuTheme.createButtonStyle(
              skin, resources.getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class));
    }
    TextButton button = new TextButton(text, style);
    button.setName(name);
    return button;
  }

  private static Settings copyOf(Settings source) {
    Settings copy = new Settings();
    copy.fps = source.fps;
    copy.fullscreen = source.fullscreen;
    copy.vsync = source.vsync;
    copy.uiScale = source.uiScale;
    copy.masterVolume = source.masterVolume;
    copy.musicVolume = source.musicVolume;
    copy.soundEffectsVolume = source.soundEffectsVolume;
    copy.muted = source.muted;
    if (source.displayMode != null) {
      copy.displayMode = new DisplaySettings();
      copy.displayMode.width = source.displayMode.width;
      copy.displayMode.height = source.displayMode.height;
      copy.displayMode.refreshRate = source.displayMode.refreshRate;
    }
    return copy;
  }

  Slider getMasterVolumeSlider() {
    return masterVolumeSlider;
  }

  Slider getMusicVolumeSlider() {
    return musicVolumeSlider;
  }

  Slider getSoundEffectsVolumeSlider() {
    return soundEffectsVolumeSlider;
  }

  CheckBox getMuteCheck() {
    return muteCheck;
  }

  TextField getFpsText() {
    return fpsText;
  }

  CheckBox getFullScreenCheck() {
    return fullScreenCheck;
  }

  CheckBox getVsyncCheck() {
    return vsyncCheck;
  }

  TextButton getResetButton() {
    return resetButton;
  }

  public TextButton getBackButton() {
    return backButton;
  }

  /** Buttons used by the pause menu's keyboard-navigation controller, in visual order. */
  public List<TextButton> getNavigationButtons() {
    return List.of(resetButton, backButton, applyButton);
  }

  TextButton getApplyButton() {
    return applyButton;
  }
}
