package com.csse3200.game.services.audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
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
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.components.settingsmenu.SettingsPanel;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/** Covers the music, sounds, button clicks, volume settings and pause music in AudioService. */
@ExtendWith(GameExtension.class)
class AudioServiceTest {
  private static final List<String> MENU_TRACKS =
      List.of("music/menuHearthlight.ogg", "music/menuStorybook.ogg", "music/menuHeroesGate.ogg");
  private static final List<String> MAP_TRACKS =
      List.of("music/mapWanderer.ogg", "music/mapFog.ogg", "music/mapCrossroads.ogg");
  private static final List<String> BATTLE_TRACKS =
      List.of("music/battleClash.ogg", "music/battleIronclad.ogg", "music/battleSkirmish.ogg");
  private static final List<String> LIBRARY_TRACKS =
      List.of("music/libraryArchive.ogg", "music/librarySpecimens.ogg", "music/libraryStudy.ogg");
  private static final String VICTORY = "sounds/victory.ogg";
  private static final String CLICK = "sounds/buttonClick.ogg";
  private static final String DEFEAT = "sounds/defeat.ogg";
  private static final String ERROR = "sounds/error.mp3";
  private static final String PAUSE = "music/pauseMenu.ogg";

  @TempDir Path folder;

  private final List<String> started = new ArrayList<>();
  private final Map<String, Music> tracks = new HashMap<>();
  private final Map<String, Sound> sounds = new HashMap<>();
  private ResourceService resources;
  private Files realFiles;
  private Graphics realGraphics;
  private Skin settingsSkin;

  @BeforeEach
  void setUp() {
    resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Music.class)))
        .thenAnswer(
            call -> {
              String path = call.getArgument(0);
              started.add(path);
              return tracks.computeIfAbsent(path, p -> mock(Music.class));
            });
    when(resources.getAsset(anyString(), eq(Sound.class)))
        .thenAnswer(call -> sounds.computeIfAbsent(call.getArgument(0), p -> mock(Sound.class)));
    ServiceLocator.registerResourceService(resources);
    AudioService.reset();

    // keep the tests away from the real settings file, starting from the default settings
    realFiles = Gdx.files;
    realGraphics = Gdx.graphics;
    Gdx.files = spy(realFiles);
    doReturn(new FileHandle(folder.resolve("settings.json").toFile()))
        .when(Gdx.files)
        .external(anyString());
    UserSettings.set(new Settings(), false);
  }

  @AfterEach
  void tearDown() {
    try {
      AudioService.reset();
      if (settingsSkin != null) {
        settingsSkin.dispose();
      }
    } finally {
      Gdx.files = realFiles;
      Gdx.graphics = realGraphics;
    }
  }

  @Test
  void mainMenuPlaysOneOfTheMenuTracks() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);

    assertTrue(MENU_TRACKS.contains(lastStarted()));
    Music track = tracks.get(lastStarted());
    verify(track).setLooping(true);
    verify(track).play();
  }

  @Test
  void goingToSettingsAndBackDoesNotRestartTheMenuMusic() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    AudioService.onScreenChanged(ScreenType.SETTINGS);
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);

    assertEquals(1, started.size());
  }

  @Test
  void mapMusicCarriesOnThroughEvents() {
    AudioService.onScreenChanged(ScreenType.MAP);
    AudioService.onScreenChanged(ScreenType.ENCOUNTER, runIn(RoomType.EVENT));
    AudioService.onScreenChanged(ScreenType.MAP);

    assertEquals(1, started.size());
  }

  @Test
  void shopsPlayShopMusic() {
    AudioService.onScreenChanged(ScreenType.MAP);

    AudioService.onScreenChanged(ScreenType.ENCOUNTER, runIn(RoomType.SHOP));

    assertEquals("music/shop.ogg", lastStarted());
  }

  @Test
  void normalFightsUseTheBattleTracks() {
    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN, runIn(RoomType.COMBAT));

    assertTrue(BATTLE_TRACKS.contains(lastStarted()));
  }

  @Test
  void eliteFightsPlayTheEliteTrack() {
    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN, runIn(RoomType.ELITE));

    assertEquals("music/eliteBattle.ogg", lastStarted());
  }

  @Test
  void theFinalBossPlaysTheBossTrack() {
    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN, runIn(RoomType.FINAL));

    assertEquals("music/bossBattle.ogg", lastStarted());
  }

  @Test
  void theLibraryScreensShareOneTrack() {
    AudioService.onScreenChanged(ScreenType.LIBRARY);
    AudioService.onScreenChanged(ScreenType.CARD_LIBRARY);
    AudioService.onScreenChanged(ScreenType.LIBRARY);
    AudioService.onScreenChanged(ScreenType.BESTIARY);

    assertEquals(1, started.size());
    assertTrue(LIBRARY_TRACKS.contains(lastStarted()));
  }

  @Test
  void leavingTheLibraryGoesBackToMenuMusic() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    AudioService.onScreenChanged(ScreenType.LIBRARY);
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);

    assertEquals(3, started.size());
    assertTrue(MENU_TRACKS.contains(lastStarted()));
  }

  @Test
  void enteringABattleStopsTheMapMusicAndStartsABattleTrack() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());

    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN);

    verify(mapTrack).stop();
    assertTrue(BATTLE_TRACKS.contains(lastStarted()));
  }

  @Test
  void winningStopsTheMusicAndPlaysTheVictorySound() {
    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN);
    Music battleTrack = tracks.get(lastStarted());

    AudioService.onScreenChanged(ScreenType.VICTORY);

    verify(battleTrack).stop();
    assertTrue(sounds.containsKey(VICTORY));
    verify(sounds.get(VICTORY)).play(anyFloat());
    assertFalse(AudioService.isMusicPlaying());
  }

  @Test
  void losingStopsTheMusicAndPlaysTheDefeatSound() {
    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN);
    Music battleTrack = tracks.get(lastStarted());

    AudioService.onScreenChanged(ScreenType.DEFEAT);

    verify(battleTrack).stop();
    assertTrue(sounds.containsKey(DEFEAT));
    verify(sounds.get(DEFEAT)).play(anyFloat());
    assertFalse(sounds.containsKey(VICTORY));
  }

  /** Coming back to the map after a fight picks again, and never the track you just had. */
  @Test
  void theMapPicksADifferentTrackAfterABattle() {
    AudioService.onScreenChanged(ScreenType.MAP);
    String first = lastStarted();

    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN);
    AudioService.onScreenChanged(ScreenType.VICTORY);
    AudioService.onScreenChanged(ScreenType.MAP);
    String second = lastStarted();

    assertTrue(MAP_TRACKS.contains(second));
    assertNotEquals(first, second);
  }

  @Test
  void buttonsOnMenuScreensClick() {
    Group root = stageRoot();
    Button button = new Button(new Button.ButtonStyle());
    root.addActor(button);

    AudioService.onScreenChanged(ScreenType.SETTINGS);
    button.fire(new ChangeEvent());

    assertTrue(sounds.containsKey(CLICK));
    verify(sounds.get(CLICK)).play(anyFloat());
  }

  /**
   * The save screen sets up its own resource service with no audio in it. Clicking there crashed.
   */
  @Test
  void clickingOnTheSaveScreenLoadsTheSoundFirst() {
    Group root = stageRoot();
    Button button = new Button(new Button.ButtonStyle());
    root.addActor(button);

    AudioService.onScreenChanged(ScreenType.SAVE_LOAD);
    button.fire(new ChangeEvent());

    verify(resources).loadSounds(new String[] {CLICK});
    verify(sounds.get(CLICK)).play(anyFloat());
  }

  @Test
  void otherWidgetsChangingDoNotClick() {
    Group root = stageRoot();
    Actor notAButton = new Actor();
    root.addActor(notAButton);

    AudioService.onScreenChanged(ScreenType.SETTINGS);
    notAButton.fire(new ChangeEvent());

    assertFalse(sounds.containsKey(CLICK));
  }

  @Test
  void gameplayScreensDoNotAddClicks() {
    Group root = stageRoot();

    AudioService.onScreenChanged(ScreenType.MAP);
    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN);

    assertEquals(0, root.getListeners().size);
  }

  /** 100% on the sliders is the normal music level of 0.5, so 60% plays at 0.3. */
  @Test
  void tracksStartAtTheSavedVolume() {
    UserSettings.set(volumes(0.6f, 1f), false);

    AudioService.onScreenChanged(ScreenType.MAP);

    verify(tracks.get(lastStarted())).setVolume(0.3f);
  }

  /** Audio updates change the track already playing without applying display settings. */
  @Test
  void changingTheVolumeAffectsTheTrackAlreadyPlaying() {
    AudioService.onScreenChanged(ScreenType.MAP);

    UserSettings.applyAudioSettings(volumes(0.4f, 1f));

    verify(tracks.get(lastStarted())).setVolume(0.2f);
  }

  @Test
  void soundEffectsFollowTheirSlider() {
    UserSettings.set(volumes(1f, 0.5f), false);
    AudioService.onScreenChanged(ScreenType.MAP);

    AudioService.playSound(SoundId.ERROR, 0.8f);

    verify(sounds.get(ERROR)).play(0.4f);
  }

  @Test
  void muteSilencesMusicAndSoundEffects() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Settings muted = new Settings();
    muted.muted = true;

    UserSettings.applyAudioSettings(muted);
    AudioService.playSound(SoundId.ERROR, 0.8f);

    verify(tracks.get(lastStarted())).setVolume(0f);
    verify(sounds.get(ERROR)).play(0f);
  }

  /** Some screens clear the ServiceLocator when they close, which unhooks the settings. */
  @Test
  void theSettingsStayConnectedAfterTheServiceLocatorIsCleared() {
    AudioService.onScreenChanged(ScreenType.MAP);
    ServiceLocator.clear();
    ServiceLocator.registerResourceService(resources);

    AudioService.onScreenChanged(ScreenType.BATTLE_SCREEN);
    UserSettings.applyAudioSettings(volumes(0.4f, 1f));

    verify(tracks.get(lastStarted())).setVolume(0.2f);
  }

  @Test
  void volumeStaysBetweenZeroAndOne() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music track = tracks.get(lastStarted());

    AudioService.setMusicVolume(1.5f);
    AudioService.setMusicVolume(-1f);

    verify(track).setVolume(1f);
    verify(track).setVolume(0f);
  }

  @Test
  void settingTheVolumeWithNoMusicPlayingIsFine() {
    assertDoesNotThrow(() -> AudioService.setMusicVolume(0.4f));
  }

  @Test
  void soundEffectsCanBeSwitchedOff() {
    AudioService.setSoundEffectsOn(false);

    AudioService.playSound(SoundId.ERROR, 0.5f);

    assertFalse(AudioService.areSoundEffectsOn());
    assertTrue(sounds.isEmpty());
  }

  @Test
  void musicStillPlaysWithSoundEffectsOff() {
    AudioService.setSoundEffectsOn(false);

    AudioService.onScreenChanged(ScreenType.MAIN_MENU);

    verify(tracks.get(lastStarted())).play();
  }

  @Test
  void pausingSwapsToThePauseMusic() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());

    AudioService.onGamePaused();

    verify(mapTrack).pause();
    assertEquals(PAUSE, lastStarted());
    verify(tracks.get(PAUSE)).play();
  }

  @Test
  void resumingCarriesOnTheSameSong() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());

    AudioService.onGamePaused();
    AudioService.onGameResumed();

    verify(tracks.get(PAUSE)).stop();
    verify(mapTrack, times(2)).play();
    assertEquals(2, started.size());
  }

  @Test
  void pausingTwiceOnlyStartsThePauseMusicOnce() {
    AudioService.onScreenChanged(ScreenType.MAP);

    AudioService.onGamePaused();
    AudioService.onGamePaused();

    assertEquals(2, started.size());
  }

  @Test
  void resumingWithoutPausingLeavesTheMusicAlone() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());

    AudioService.onGameResumed();

    verify(mapTrack, never()).stop();
    verify(mapTrack, times(1)).play();
  }

  @Test
  void quittingFromThePauseMenuGoesToTheMenuMusic() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());
    AudioService.onGamePaused();

    AudioService.onScreenChanged(ScreenType.MAIN_MENU);

    verify(tracks.get(PAUSE)).stop();
    verify(mapTrack).stop();
    assertTrue(MENU_TRACKS.contains(lastStarted()));
  }

  @Test
  void theVolumeCanChangeWhilePaused() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());
    AudioService.onGamePaused();

    UserSettings.applyAudioSettings(volumes(0.4f, 1f));
    AudioService.onGameResumed();

    verify(tracks.get(PAUSE)).setVolume(0.2f);
    verify(mapTrack).setVolume(0.2f);
  }

  @Test
  void panelChangesScaleCurrentMusicAndFutureSoundsWithoutApplyingDisplay() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    Music track = tracks.get(lastStarted());
    SettingsPanel panel = settingsPanel();
    panel.<TextField>findActor("fps-cap").setText("invalid");
    panel.<CheckBox>findActor("fullscreen").setChecked(true);
    clearInvocations(track);

    panel.<Slider>findActor("master-volume").setValue(0.5f);
    panel.<Slider>findActor("music-volume").setValue(0.6f);
    panel.<Slider>findActor("sound-effects-volume").setValue(0.4f);
    AudioService.playSound(SoundId.ERROR, 0.8f);

    verify(track).setVolume(0.25f);
    verify(track, times(2)).setVolume(0.15f);
    verify(track, never()).play();
    verify(track, never()).stop();
    ArgumentCaptor<Float> volume = ArgumentCaptor.forClass(Float.class);
    verify(sounds.get(ERROR)).play(volume.capture());
    assertEquals(0.8f * 0.5f * 0.4f, volume.getValue(), 0.0001f);
    Settings saved = UserSettings.get();
    assertEquals(0.5f, saved.masterVolume);
    assertEquals(0.6f, saved.musicVolume);
    assertEquals(0.4f, saved.soundEffectsVolume);
    assertEquals(60, saved.fps);
    assertFalse(saved.fullscreen);
    verify(Gdx.graphics, never()).setForegroundFPS(org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void panelMutePreservesPreferencesAndUnmuteUsesLatestChannelValues() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    Music track = tracks.get(lastStarted());
    SettingsPanel panel = settingsPanel();
    panel.<Slider>findActor("master-volume").setValue(0.5f);
    panel.<Slider>findActor("music-volume").setValue(0.6f);
    panel.<Slider>findActor("sound-effects-volume").setValue(0.4f);
    clearInvocations(track);

    panel.<CheckBox>findActor("mute").setChecked(true);
    panel.<Slider>findActor("music-volume").setValue(0.2f);
    AudioService.playSound(SoundId.ERROR, 0.8f);

    verify(track, times(2)).setVolume(0f);
    verify(sounds.get(ERROR)).play(0f);
    Settings saved = UserSettings.get();
    assertTrue(saved.muted);
    assertEquals(0.5f, saved.masterVolume);
    assertEquals(0.2f, saved.musicVolume);
    assertEquals(0.4f, saved.soundEffectsVolume);
    panel.<CheckBox>findActor("mute").setChecked(false);
    verify(track).setVolume(0.05f);
    assertFalse(UserSettings.get().muted);
    verify(track, never()).play();
  }

  @Test
  void panelSoundEffectsRetainOverlapDamping() throws ReflectiveOperationException {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    SettingsPanel panel = settingsPanel();
    panel.<Slider>findActor("master-volume").setValue(0.5f);
    panel.<Slider>findActor("sound-effects-volume").setValue(0.4f);
    // Force the existing cooldown path without depending on test execution speed or sleeping.
    Field cooldown = AudioService.class.getDeclaredField("cooldownTimestamp");
    cooldown.setAccessible(true);
    cooldown.setLong(null, Long.MAX_VALUE);

    AudioService.playSound(SoundId.ERROR, 0.8f);

    ArgumentCaptor<Float> volume = ArgumentCaptor.forClass(Float.class);
    verify(sounds.get(ERROR)).play(volume.capture());
    assertEquals((0.8f - 0.2f) * 0.5f * 0.4f, volume.getValue(), 0.0001f);
  }

  @Test
  void panelChangesUpdatePauseMusicAndTheHeldTrackOnResume() {
    AudioService.onScreenChanged(ScreenType.MAP);
    Music mapTrack = tracks.get(lastStarted());
    AudioService.onGamePaused();
    Music pauseTrack = tracks.get(PAUSE);
    SettingsPanel panel = settingsPanel();

    panel.<Slider>findActor("master-volume").setValue(0.5f);
    panel.<Slider>findActor("music-volume").setValue(0.4f);
    panel.<CheckBox>findActor("mute").setChecked(true);
    verify(pauseTrack).setVolume(0.1f);
    verify(pauseTrack).setVolume(0f);

    AudioService.onGameResumed();

    verify(mapTrack).setVolume(0f);
    verify(mapTrack, times(2)).play();
    verify(pauseTrack).stop();
    panel.<CheckBox>findActor("mute").setChecked(false);
    verify(mapTrack).setVolume(0.1f);
    assertEquals(0.5f, UserSettings.get().masterVolume);
    assertEquals(0.4f, UserSettings.get().musicVolume);
    assertFalse(UserSettings.get().muted);
  }

  @Test
  void panelPreferencesSurviveLocatorClearingAndAudioRuntimeRestart() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    Music track = tracks.get(lastStarted());
    SettingsPanel panel = settingsPanel();
    panel.<Slider>findActor("master-volume").setValue(0.5f);
    panel.<Slider>findActor("music-volume").setValue(0.6f);
    panel.<Slider>findActor("sound-effects-volume").setValue(0.4f);
    clearInvocations(track);
    ServiceLocator.clear();
    ServiceLocator.registerResourceService(resources);

    AudioService.onScreenChanged(ScreenType.SETTINGS);

    verify(track).setVolume(0.15f);
    verify(track, never()).play();
    assertEquals(1, started.size());
    AudioService.reset();
    ServiceLocator.clear();
    ServiceLocator.registerResourceService(resources);
    tracks.clear();
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    verify(tracks.get(lastStarted())).setVolume(0.15f);
    SettingsPanel reopened = settingsPanel();
    assertEquals(0.5f, reopened.<Slider>findActor("master-volume").getValue());
    assertEquals(0.6f, reopened.<Slider>findActor("music-volume").getValue());
    assertEquals(0.4f, reopened.<Slider>findActor("sound-effects-volume").getValue());
    AudioService.playSound(SoundId.ERROR, 0.8f);
    ArgumentCaptor<Float> volume = ArgumentCaptor.forClass(Float.class);
    verify(sounds.get(ERROR)).play(volume.capture());
    assertEquals(0.16f, volume.getValue(), 0.0001f);
  }

  @Test
  void panelMuteSurvivesAudioRuntimeRestartWithoutLosingSliderValues() {
    AudioService.onScreenChanged(ScreenType.MAIN_MENU);
    SettingsPanel panel = settingsPanel();
    panel.<Slider>findActor("master-volume").setValue(0.5f);
    panel.<Slider>findActor("music-volume").setValue(0.6f);
    panel.<Slider>findActor("sound-effects-volume").setValue(0.4f);
    panel.<CheckBox>findActor("mute").setChecked(true);
    AudioService.reset();
    ServiceLocator.clear();
    ServiceLocator.registerResourceService(resources);
    tracks.clear();

    AudioService.onScreenChanged(ScreenType.MAIN_MENU);

    Music track = tracks.get(lastStarted());
    verify(track).setVolume(0f);
    SettingsPanel reopened = settingsPanel();
    assertEquals(0.5f, reopened.<Slider>findActor("master-volume").getValue());
    assertEquals(0.6f, reopened.<Slider>findActor("music-volume").getValue());
    assertEquals(0.4f, reopened.<Slider>findActor("sound-effects-volume").getValue());
    assertTrue(reopened.<CheckBox>findActor("mute").isChecked());
    reopened.<CheckBox>findActor("mute").setChecked(false);
    verify(track).setVolume(0.15f);
    assertFalse(UserSettings.get().muted);
  }

  private SettingsPanel settingsPanel() {
    if (settingsSkin == null) {
      settingsSkin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
      Graphics graphics = mock(Graphics.class);
      DisplayMode mode = mock(DisplayMode.class);
      when(graphics.getDisplayMode()).thenReturn(mode);
      when(graphics.getDisplayModes(org.mockito.ArgumentMatchers.any()))
          .thenReturn(new DisplayMode[] {mode});
      Gdx.graphics = graphics;
    }
    return new SettingsPanel(settingsSkin, () -> {});
  }

  /** The IDs match the file lists by position, so a missing or out of order entry shows up here. */
  @Test
  void everyMusicAndSoundPointsAtARealFile() {
    for (MusicId id : MusicId.values()) {
      AudioService.playMusic(id);
    }
    for (SoundId id : SoundId.values()) {
      AudioService.playSound(id, 0.5f);
    }

    assertEquals(MusicId.values().length, started.size());
    assertEquals(SoundId.values().length, sounds.size());
    for (String path : started) {
      assertTrue(Gdx.files.internal(path).exists(), path);
    }
    for (String path : sounds.keySet()) {
      assertTrue(Gdx.files.internal(path).exists(), path);
    }
  }

  /** A run where the player has just walked into a room of the given type. */
  private static RunState runIn(RoomType room) {
    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(0, new MapNode(0, RoomType.START));
    nodes.put(1, new MapNode(1, room));
    MapGraph graph = new MapGraph(nodes, false);
    graph.getNode(0).addConnection(graph.getNode(1));
    RunState run = new RunState();
    run.startRun(graph, 0);
    run.enterEncounter(1);
    return run;
  }

  private static Settings volumes(float music, float soundEffects) {
    Settings settings = new Settings();
    settings.musicVolume = music;
    settings.soundEffectsVolume = soundEffects;
    return settings;
  }

  private String lastStarted() {
    return started.get(started.size() - 1);
  }

  /** Stands in for the current screen's stage, with a real root so events can be fired at it. */
  private Group stageRoot() {
    Group root = new Group();
    Stage stage = mock(Stage.class);
    when(stage.getRoot()).thenReturn(root);
    RenderService render = mock(RenderService.class);
    when(render.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(render);
    return root;
  }
}
