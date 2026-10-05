package com.csse3200.game.services.audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Covers the music, victory sound, button clicks and settings added to AudioService. */
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

  private final List<String> started = new ArrayList<>();
  private final Map<String, Music> tracks = new HashMap<>();
  private final Map<String, Sound> sounds = new HashMap<>();
  private ResourceService resources;

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
  }

  @AfterEach
  void tearDown() {
    AudioService.reset();
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

  @Test
  void tracksStartAtTheChosenVolume() {
    AudioService.setMusicVolume(0.3f);

    AudioService.onScreenChanged(ScreenType.MAP);

    verify(tracks.get(lastStarted())).setVolume(0.3f);
  }

  /** Dragging the settings slider has to change the track you can already hear. */
  @Test
  void changingTheVolumeAffectsTheTrackAlreadyPlaying() {
    AudioService.onScreenChanged(ScreenType.MAP);

    AudioService.setMusicVolume(0.2f);

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
