package com.csse3200.game.services.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.csse3200.game.services.ServiceLocator;

public class AudioService {

    private static Music playing;
    private static float musicVolume = 0.5f; // TODO: decide default music volume
    private static final String[] soundPaths = {
            "sounds/Impact4.ogg",
            "sounds/menuClick.mp3",
            "sounds/itemPurchase.mp3"
    };
    private static final String[] musicPaths = {
            "music/BGM_03_mp3.mp3"
    };

    /**
     * Loads all the audio files defined in their respective path arrays. Called once in GdxGame at game start.
     **/
    public static void load() {

        playing.setVolume(musicVolume);
        playing.setLooping(true);
        ServiceLocator.getResourceService().loadSounds(soundPaths);
        ServiceLocator.getResourceService().loadMusic(musicPaths);
    }

    /**
     * Plays a chosen sound effect where it is called. Uses the ordinal of the SoundID enum parameter to find the sound
     * index from array.
     * @param soundID Enumerator type code that translates to an index of the sound path array.
     * @param volume Floating point value that sets the volume. Likely varies between sfx.
     */
    public static void playSound(SoundId soundID, float volume) {

        Sound sound = ServiceLocator.getResourceService().getAsset(soundPaths[soundID.ordinal()], Sound.class);
        sound.play(volume);
    }

    public static boolean isMusicPlaying() {

        return playing != null && playing.isPlaying();
    }

    /**
     * Plays a chosen song where it is called. Uses the ordinal of the MusicID enum parameter to find the music
     * index from array. Functions within AudioService can control and configure the music.
     * @param musicID Enumerator type code that translates to an index of the music path array.
     */
    public static void playMusic(MusicId musicID) {

        if (playing.isPlaying()) {
            playing.stop();
        }

        playing = ServiceLocator.getResourceService().getAsset(musicPaths[musicID.ordinal()], Music.class);
        playing.play();
    }

    /**
     * Pauses the currently playing music track.
     */
    public static void pauseMusic() {

        playing.pause();
    }

    /**
     * Sets whether the currently playing song is looping.
     */
    public static void setMusicLooping(boolean looping) {

        playing.setLooping(looping);
    }

    /**
     * Sets the volume of the currently playing music.
     */
    public void setMusicVolume(float volume) {

        musicVolume = volume;
        playing.setVolume(musicVolume);
    }


}
