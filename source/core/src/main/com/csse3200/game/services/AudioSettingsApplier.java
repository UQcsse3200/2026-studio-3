package com.csse3200.game.services;

/**
 * Applies already-combined output volumes to the game's audio system.
 *
 * <p>The settings feature owns persistence, muting, and master/channel volume calculation. An audio
 * implementation only needs to update current and future music and sound-effect playback.
 */
@FunctionalInterface
public interface AudioSettingsApplier {
  /**
   * Applies effective output volumes in the inclusive range {@code [0, 1]}.
   *
   * @param effectiveMusicVolume combined master and music volume, or zero while muted
   * @param effectiveSoundEffectsVolume combined master and sound-effects volume, or zero while
   *     muted
   */
  void applyVolumes(float effectiveMusicVolume, float effectiveSoundEffectsVolume);
}
