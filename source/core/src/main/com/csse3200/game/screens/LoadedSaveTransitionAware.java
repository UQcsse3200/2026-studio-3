package com.csse3200.game.screens;

/**
 * Implemented by screens that normally capture live state while being disposed.
 *
 * <p>After a save has been restored, the outgoing screen must not write its older state back over
 * the restored run during the transition to the map.
 */
public interface LoadedSaveTransitionAware {
  /** Prevents disposal from overwriting a save that has just been restored. */
  void prepareForLoadedSaveTransition();
}
