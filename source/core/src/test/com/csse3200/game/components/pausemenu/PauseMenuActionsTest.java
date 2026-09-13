package com.csse3200.game.components.pausemenu;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

@ExtendWith(GameExtension.class)
class PauseMenuActionsTest {
  private GdxGame game;
  private GamePauseService pauseService;
  private Entity pauseMenu;

  @BeforeEach
  void setUp() {
    game = mock(GdxGame.class);
    pauseService = mock(GamePauseService.class);
    ServiceLocator.registerPauseService(pauseService);

    pauseMenu = new Entity().addComponent(new PauseMenuActions(game));
    pauseMenu.create();
  }

  @Test
  void pauseEventPausesGameplay() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);

    verify(pauseService).pause();
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void resumeEventResumesGameplay() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.RESUME_EVENT);

    verify(pauseService).resume();
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void settingsEventDoesNotDisposeGameplayScreen() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.SETTINGS_EVENT);

    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void exitToMenuResumesBeforeOpeningMainMenu() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.EXIT_TO_MENU_EVENT);

    InOrder inOrder = inOrder(pauseService, game);
    inOrder.verify(pauseService).resume();
    inOrder.verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
  }
}
