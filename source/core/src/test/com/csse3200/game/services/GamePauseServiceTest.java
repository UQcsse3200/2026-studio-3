package com.csse3200.game.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;

class GamePauseServiceTest {
  @Test
  void pauseStopsGameTime() {
    GameTime gameTime = mock(GameTime.class);
    GamePauseService pauseService = new GamePauseService(gameTime);

    pauseService.pause();

    assertTrue(pauseService.isPaused());
    verify(gameTime).setTimeScale(0f);
  }

  @Test
  void resumeRestoresGameTime() {
    GameTime gameTime = mock(GameTime.class);
    GamePauseService pauseService = new GamePauseService(gameTime);

    pauseService.pause();
    pauseService.resume();

    assertFalse(pauseService.isPaused());
    verify(gameTime).setTimeScale(0f);
    verify(gameTime).setTimeScale(1f);
  }

  @Test
  void repeatedPauseAndResumeCallsAreIgnored() {
    GameTime gameTime = mock(GameTime.class);
    GamePauseService pauseService = new GamePauseService(gameTime);

    pauseService.pause();
    pauseService.pause();
    pauseService.resume();
    pauseService.resume();

    assertFalse(pauseService.isPaused());
    verify(gameTime).setTimeScale(0f);
    verify(gameTime).setTimeScale(1f);
    verifyNoMoreInteractions(gameTime);
  }

  @Test
  void resumeBeforePauseDoesNothing() {
    GameTime gameTime = mock(GameTime.class);
    GamePauseService pauseService = new GamePauseService(gameTime);

    pauseService.resume();

    assertFalse(pauseService.isPaused());
    verify(gameTime, never()).setTimeScale(1f);
  }

  @Test
  void requiresTimeSource() {
    assertThrows(IllegalArgumentException.class, () -> new GamePauseService(null));
  }
}
