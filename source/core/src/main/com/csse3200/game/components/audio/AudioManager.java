package com.csse3200.game.components.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

public class AudioManager {

    private Music playing;
    private ResourceService resourceService;
    private float musicVolume, sfxVolume;
    private static final String[] soundPaths = {"sounds/Impact4.ogg", "sounds/menuClick.mp3"};
    private static final String[] musicPaths = {"music/BGM_03_mp3.mp3"};

    AudioManager(ResourceService resourceService) {
        this.musicVolume = 0.4f; // TODO: decide if 1.0f should be default
        this.sfxVolume = 0.4f;
        this.resourceService = resourceService;

        resourceService.loadSounds(soundPaths);
        resourceService.loadMusic(musicPaths);
    }

    public void playSound(int soundID) {
        Sound sound = ServiceLocator.getResourceService().getAsset(soundPaths[soundID], Sound.class);
        sound.play();
    }

    public void playMusic(int musicID) {
        playing = ServiceLocator.getResourceService().getAsset(musicPaths[musicID], Music.class);
        playing.play();
    }

    public void stopMusic() {
        playing.pause();
    }

    public void setMusicLooping(boolean looping) {
        playing.setLooping(looping);
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    public void setSfxVolume(float sfxVolume) {
        this.sfxVolume = sfxVolume;
    }

}
