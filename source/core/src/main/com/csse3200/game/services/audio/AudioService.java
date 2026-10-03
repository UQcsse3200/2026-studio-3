package com.csse3200.game.services.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.csse3200.game.services.ServiceLocator;

import java.util.Random;

public class AudioService {

    private static final int SFX_COOLDOWN = 100; // 100ms sound cooldown
    private static final float LOWER_PITCH_BOUND = 0.90f;
    private static final float UPPER_PITCH_BOUND = 1.10f;
    private static Music playing;
    private static long timestamp;
    private static float musicVolume = 0.5f; // TODO: decide default music volume
    private static final String[] soundPaths = {
            "sounds/Impact4.ogg",
            "sounds/menuHover.mp3",
            "sounds/itemPurchase.mp3",
            "sounds/swordSlice.mp3",
            "sounds/enterShop.mp3",
            "sounds/error.mp3",
            "sounds/enterEncounter.mp3",
            "sounds/cardHover.mp3",
            "sounds/cardShuffle.mp3"
    };
    private static final String[] musicPaths = {
            "music/BGM_03_mp3.mp3"
    };
    private static final Random rand = new Random();

    /**
     * Loads all the audio files defined in their respective path arrays. Called once in GdxGame at game start.
     **/
    public static void load() {

        timestamp = System.currentTimeMillis();
        ServiceLocator.getResourceService().loadSounds(soundPaths);
        ServiceLocator.getResourceService().loadMusic(musicPaths);
    }

    /**
     * Plays a chosen sound effect where it is called. Uses the ordinal of the SoundID enum parameter to find the sound
     * index from array. Additionally, it has a cooldown timer for all sound effects so they do not get spammed.
     * @param soundID Enumerator type code that translates to an index of the sound path array.
     * @param volume Floating point value that sets the volume. Likely varies between sfx.
     */
    public static void playSound(SoundId soundID, float volume) {

        if ( System.currentTimeMillis() - timestamp > SFX_COOLDOWN) {

            Sound sound = ServiceLocator.getResourceService().getAsset(soundPaths[soundID.ordinal()], Sound.class);
            sound.setPitch(sound.play(volume), rand.nextFloat(LOWER_PITCH_BOUND, UPPER_PITCH_BOUND));
            timestamp = System.currentTimeMillis();
        }
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

        if (playing != null && playing.isPlaying()) {
            playing.stop();
        }

        playing = ServiceLocator.getResourceService().getAsset(musicPaths[musicID.ordinal()], Music.class);
        playing.setVolume(musicVolume);
        playing.setLooping(true);
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
