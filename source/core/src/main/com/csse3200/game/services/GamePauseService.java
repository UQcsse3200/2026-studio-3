package com.csse3200.game.services;

/** Global paused/running state for gameplay screens. */
public class GamePauseService {
  private static final float PAUSED_TIME_SCALE = 0f;
  private static final float RUNNING_TIME_SCALE = 1f;

  private final GameTime timeSource;
  private boolean paused;

  public GamePauseService(GameTime timeSource) {
    if (timeSource == null) {
      throw new IllegalArgumentException("timeSource must not be null");
    }
    this.timeSource = timeSource;
  }

  /** Pauses gameplay time. Repeated calls are ignored. */
  public void pause() {
    if (paused) {
      return;
    }
    timeSource.setTimeScale(PAUSED_TIME_SCALE);
    paused = true;
  }

  /** Resumes gameplay time. Repeated calls are ignored. */
  public void resume() {
    if (!paused) {
      return;
    }
    timeSource.setTimeScale(RUNNING_TIME_SCALE);
    paused = false;
  }

  public boolean isPaused() {
    return paused;
  }
}
