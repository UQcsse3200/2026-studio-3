package com.csse3200.game.services.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class AudioService {

  private static final int SFX_COOLDOWN = 100; // 100ms sound cooldown
  private static final float LOWER_PITCH_BOUND = 0.90f, UPPER_PITCH_BOUND = 1.10f;
  private static final float OVERLAP_DAMPING = 0.2f;
  private static final float VICTORY_VOLUME = 0.7f;
  private static final float DEFEAT_VOLUME = 0.7f;
  private static final float CLICK_VOLUME = 0.5f;
  private static final float FULL_MUSIC_VOLUME = 0.5f; // music volume with the sliders at 100%
  private static Music playing;
  private static SoundId lastSound = SoundId.MENU_HOVER;
  private static long cooldownTimestamp;
  private static float musicVolume = 0.5f; // TODO: decide default music volume
  private static boolean soundEffectsOn = true;
  private static float soundEffectsVolume = 1f;
  private static Playlist nowPlaying;
  private static Music heldTrack;
  private static boolean gamePaused;
  private static final Random pitchModifier = new Random();
  private static final Random shuffle = new Random();

  /** Screens whose buttons click. Battles and the shop already have their own sounds. */
  private static final Set<ScreenType> MENU_SCREENS =
      EnumSet.of(
          ScreenType.MAIN_MENU,
          ScreenType.SETTINGS,
          ScreenType.SAVE_LOAD,
          ScreenType.LIBRARY,
          ScreenType.CARD_LIBRARY,
          ScreenType.BESTIARY);

  // this may appear to be a lot to load into ram, but these files are kilobytes big
  private static final String[] soundPaths = {
    "sounds/menuHover.mp3",
    "sounds/itemPurchase.mp3",
    "sounds/enterCombat.mp3",
    "sounds/enterShop.mp3",
    "sounds/error.mp3",
    "sounds/enterEncounter.mp3",
    "sounds/cardHover.mp3",
    "sounds/cardShuffle.mp3",
    "sounds/enterElite.mp3",
    "sounds/itemPickup.mp3",
    "sounds/swordSwing.mp3",
    "sounds/shieldGuard.mp3",
    "sounds/bandage.mp3",
    "sounds/poison.mp3",
    "sounds/armourBreak.mp3",
    "sounds/magicChime.mp3",
    "sounds/leaveShop.mp3",
    "sounds/victory.ogg",
    "sounds/buttonClick.ogg",
    "sounds/defeat.ogg",
  };
  private static final String[] musicPaths = {
    "music/BGM_03_mp3.mp3",
    "music/menuHearthlight.ogg",
    "music/menuStorybook.ogg",
    "music/menuHeroesGate.ogg",
    "music/mapWanderer.ogg",
    "music/mapFog.ogg",
    "music/mapCrossroads.ogg",
    "music/battleClash.ogg",
    "music/battleIronclad.ogg",
    "music/battleSkirmish.ogg",
    "music/libraryArchive.ogg",
    "music/librarySpecimens.ogg",
    "music/libraryStudy.ogg",
    "music/eliteBattle.ogg",
    "music/bossBattle.ogg",
    "music/shop.ogg",
    "music/pauseMenu.ogg",
  };

  /** The music for each kind of screen. Arriving on one picks a random track from its list. */
  private enum Playlist {
    MENU(MusicId.MENU_HEARTHLIGHT, MusicId.MENU_STORYBOOK, MusicId.MENU_HEROES_GATE),
    MAP(MusicId.MAP_WANDERER, MusicId.MAP_FOG, MusicId.MAP_CROSSROADS),
    BATTLE(MusicId.BATTLE_CLASH, MusicId.BATTLE_IRONCLAD, MusicId.BATTLE_SKIRMISH),
    LIBRARY(MusicId.LIBRARY_ARCHIVE, MusicId.LIBRARY_SPECIMENS, MusicId.LIBRARY_STUDY),
    ELITE(MusicId.ELITE_BATTLE),
    BOSS(MusicId.BOSS_BATTLE),
    SHOP(MusicId.SHOP);

    private final MusicId[] tracks;
    private MusicId last;

    Playlist(MusicId... tracks) {
      this.tracks = tracks;
    }

    /** Picks a random track from the list, never the one it picked last time. */
    private MusicId next() {
      MusicId pick;
      do {
        pick = tracks[shuffle.nextInt(tracks.length)];
      } while (pick == last && tracks.length > 1);
      last = pick;
      return pick;
    }
  }

  /**
   * Loads all the audio files defined in their respective path arrays. Called once in GdxGame at
   * game start.
   */
  public static void load() {

    cooldownTimestamp = System.currentTimeMillis();
    ServiceLocator.getResourceService().loadSounds(soundPaths);
    ServiceLocator.getResourceService().loadMusic(musicPaths);
  }

  /**
   * Plays a chosen sound effect where it is called. Uses the ordinal of the SoundID enum parameter
   * to find the sound index from array. Additionally, it has a cooldown timer for all sound effects
   * so they do not get spammed.
   *
   * @param soundID Enumerator type code that translates to an index of the sound path array.
   * @param volume Floating point value that sets the volume. Likely varies between sfx.
   */
  public static void playSound(SoundId soundID, float volume) {

    if (!soundEffectsOn) {
      return;
    }

    if (System.currentTimeMillis() - cooldownTimestamp > SFX_COOLDOWN) {

      Sound sound =
          ServiceLocator.getResourceService().getAsset(soundPaths[soundID.ordinal()], Sound.class);
      sound.setPitch(
          sound.play(volume * soundEffectsVolume),
          pitchModifier.nextFloat(LOWER_PITCH_BOUND, UPPER_PITCH_BOUND));
      lastSound = soundID;
      cooldownTimestamp = System.currentTimeMillis();

    } else if (soundID != lastSound) { // allows for different sound effects to overlap

      Sound sound =
          ServiceLocator.getResourceService().getAsset(soundPaths[soundID.ordinal()], Sound.class);
      sound.setPitch(
          sound.play((volume - OVERLAP_DAMPING) * soundEffectsVolume),
          pitchModifier.nextFloat(LOWER_PITCH_BOUND, UPPER_PITCH_BOUND));
    }
  }

  public static boolean isMusicPlaying() {

    return playing != null && playing.isPlaying();
  }

  /**
   * Plays a chosen song where it is called. Uses the ordinal of the MusicID enum parameter to find
   * the music index from array. Functions within AudioService can control and configure the music.
   *
   * @param musicID Enumerator type code that translates to an index of the music path array.
   */
  public static void playMusic(MusicId musicID) {

    if (playing != null && playing.isPlaying()) {
      playing.stop();
    }

    playing =
        ServiceLocator.getResourceService().getAsset(musicPaths[musicID.ordinal()], Music.class);
    playing.setVolume(musicVolume);
    playing.setLooping(true);
    playing.play();
  }

  /** Pauses the currently playing music track. */
  public static void pauseMusic() {

    playing.pause();
  }

  /**
   * Stops the music completely, unlike {@link #pauseMusic()} which can carry on from the same spot.
   */
  public static void stopMusic() {
    if (playing != null) {
      playing.stop();
      playing = null;
    }
    nowPlaying = null;
  }

  /** Sets whether the currently playing song is looping. */
  public static void setMusicLooping(boolean looping) {

    playing.setLooping(looping);
  }

  /**
   * Sets the volume of the music. The song that is playing changes straight away, and every song
   * after it starts at this volume.
   *
   * @param volume from 0 to 1
   */
  public static void setMusicVolume(float volume) {

    musicVolume = Math.max(0f, Math.min(1f, volume));
    if (playing != null) {
      playing.setVolume(musicVolume);
    }
  }

  /**
   * Switches all sound effects on or off. Music isn't affected, it has its own volume.
   *
   * @param on true to play sound effects
   */
  public static void setSoundEffectsOn(boolean on) {
    soundEffectsOn = on;
  }

  /**
   * Whether sound effects are switched on.
   *
   * @return false if they have been turned off in the settings
   */
  public static boolean areSoundEffectsOn() {
    return soundEffectsOn;
  }

  /**
   * Called by GdxGame every time the screen changes. Starts the music for the screen, plays the
   * victory or defeat sound when a battle ends, makes the buttons on menu screens click and keeps
   * the volume settings connected.
   *
   * <p>Battles check the room they were entered from, so elite fights and the final boss get their
   * own tracks, and the encounter screen only switches to shop music when it is a shop. The
   * library, card library and bestiary share one set of tracks, so moving between them keeps the
   * same song going. Screens without their own music (settings, events, the save screen and so on)
   * leave the current track playing, so going into the settings and back doesn't restart it.
   *
   * @param screen the screen that just opened
   * @param run the current run, used to tell what kind of room the player has walked into
   */
  public static void onScreenChanged(ScreenType screen, RunState run) {
    followVolumeSettings();
    leavePauseMenu();
    switch (screen) {
      case MAIN_MENU -> startPlaylist(Playlist.MENU);
      case MAP -> startPlaylist(Playlist.MAP);
      case BATTLE_SCREEN -> startPlaylist(battlePlaylist(currentRoom(run)));
      case ENCOUNTER -> {
        // events keep the map music, only shops change it
        if (currentRoom(run) == RoomType.SHOP) {
          startPlaylist(Playlist.SHOP);
        }
      }
      case LIBRARY, CARD_LIBRARY, BESTIARY -> startPlaylist(Playlist.LIBRARY);
      case VICTORY -> {
        stopMusic();
        playLoadedSound(SoundId.VICTORY, VICTORY_VOLUME);
      }
      case DEFEAT -> {
        stopMusic();
        playLoadedSound(SoundId.DEFEAT, DEFEAT_VOLUME);
      }
      default -> {
        // keep whatever is already playing
      }
    }
    if (MENU_SCREENS.contains(screen)) {
      addButtonClicks();
    }
  }

  /** The same as {@link #onScreenChanged(ScreenType, RunState)} for screens outside a run. */
  static void onScreenChanged(ScreenType screen) {
    onScreenChanged(screen, null);
  }

  /**
   * Called when the pause menu opens. The song that was playing is paused so it can carry on from
   * the same spot, and the pause menu music plays instead.
   */
  public static void onGamePaused() {
    if (gamePaused) {
      return;
    }
    gamePaused = true;
    heldTrack = playing;
    playing = null;
    if (heldTrack != null) {
      heldTrack.pause();
    }
    if (ensureLoaded(musicPaths[MusicId.PAUSE_MENU.ordinal()], Music.class)) {
      playMusic(MusicId.PAUSE_MENU);
    }
  }

  /** Called when the game is resumed. Stops the pause menu music and carries on the held song. */
  public static void onGameResumed() {
    if (!gamePaused) {
      return;
    }
    gamePaused = false;
    if (playing != null) {
      playing.stop();
    }
    playing = heldTrack;
    heldTrack = null;
    if (playing != null) {
      playing.setVolume(musicVolume);
      playing.play();
    }
  }

  /** Leaving a screen from its pause menu, like quitting to the main menu, ends the pause music. */
  private static void leavePauseMenu() {
    if (!gamePaused) {
      return;
    }
    gamePaused = false;
    stopMusic();
    if (heldTrack != null) {
      heldTrack.stop();
      heldTrack = null;
    }
  }

  /**
   * Connects the volume settings to this class. Registering also applies the saved volumes, so the
   * first song starts at the right level. Some screens clear the ServiceLocator when they close,
   * which drops the connection, so this checks again on every screen change.
   */
  private static void followVolumeSettings() {
    if (ServiceLocator.getAudioSettingsApplier() == null) {
      ServiceLocator.registerAudioSettingsApplier(AudioService::applyVolumes);
    }
  }

  /**
   * Sets the volumes picked in the settings, with the master volume and mute already applied.
   *
   * @param music music volume from 0 to 1
   * @param soundEffects sound effects volume from 0 to 1
   */
  private static void applyVolumes(float music, float soundEffects) {
    setMusicVolume(music * FULL_MUSIC_VOLUME);
    soundEffectsVolume = Math.max(0f, Math.min(1f, soundEffects));
  }

  private static Playlist battlePlaylist(RoomType room) {
    if (room == RoomType.FINAL) {
      return Playlist.BOSS;
    }
    return room == RoomType.ELITE ? Playlist.ELITE : Playlist.BATTLE;
  }

  /** The type of room the player is in, or null if they aren't in one. */
  private static RoomType currentRoom(RunState run) {
    if (run == null || run.getMapGraph() == null || run.getActiveNodeId() == null) {
      return null;
    }
    MapNode node = run.getMapGraph().getNode(run.getActiveNodeId());
    return node == null ? null : node.getRoomType();
  }

  private static void startPlaylist(Playlist list) {
    if (list == nowPlaying) {
      return;
    }
    stopMusic();
    MusicId track = list.next();
    if (ensureLoaded(musicPaths[track.ordinal()], Music.class)) {
      playMusic(track);
    }
    nowPlaying = list;
  }

  /** Plays one of the sounds added for screens, loading it first if this screen hasn't. */
  private static void playLoadedSound(SoundId soundID, float volume) {
    if (ensureLoaded(soundPaths[soundID.ordinal()], Sound.class)) {
      playSound(soundID, volume);
    }
  }

  /**
   * Makes sure a file is loaded before it plays. The save screen sets up its own resource service
   * without loading any audio, so a button click there used to crash the game.
   *
   * @return false if there is no resource service to load into, as in tests
   */
  private static boolean ensureLoaded(String path, Class<?> type) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null) {
      return false;
    }
    if (!resources.containsAsset(path, type)) {
      if (type == Music.class) {
        resources.loadMusic(new String[] {path});
      } else {
        resources.loadSounds(new String[] {path});
      }
      resources.loadAll();
    }
    return true;
  }

  /**
   * Makes every button on the current screen click when pressed. Buttons send a change event that
   * bubbles up to the stage's root, so one listener there covers all of them. The sound cooldown
   * stops a double click when a button group unchecks its old button in the same press.
   */
  private static void addButtonClicks() {
    RenderService render = ServiceLocator.getRenderService();
    if (render == null || render.getStage() == null) {
      return;
    }
    Group root = render.getStage().getRoot();
    root.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (actor instanceof Button) {
              playLoadedSound(SoundId.BUTTON_CLICK, CLICK_VOLUME);
            }
          }
        });
  }

  /** Puts everything back to how the game starts. The state here is static, so tests need this. */
  static void reset() {
    stopMusic();
    lastSound = SoundId.MENU_HOVER;
    cooldownTimestamp = 0;
    musicVolume = 0.5f;
    soundEffectsOn = true;
    soundEffectsVolume = 1f;
    heldTrack = null;
    gamePaused = false;
  }
}
