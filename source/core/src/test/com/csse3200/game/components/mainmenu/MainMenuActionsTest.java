package com.csse3200.game.components.mainmenu;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MainMenuActionsTest {
  private GdxGame game;
  private RunState runState;
  private Entity menu;

  @BeforeEach
  void setUp() {
    game = mock(GdxGame.class);
    runState = mock(RunState.class);
    when(game.getRunState()).thenReturn(runState);

    menu = new Entity().addComponent(new MainMenuActions(game));
    menu.create();
  }

  @Test
  void startBeginsANewRunOnTheMap() {
    menu.getEvents().trigger(MainMenuDisplay.START_EVENT);

    verify(runState).endRun();
    verify(game).setScreen(GdxGame.ScreenType.MAP);
  }

  @Test
  void loadWaitsForBackendRoute() {
    menu.getEvents().trigger(MainMenuDisplay.LOAD_EVENT);

    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
    verify(runState, never()).endRun();
  }

  @Test
  void bestiaryWaitsForBackendRoute() {
    menu.getEvents().trigger(MainMenuDisplay.BESTIARY_EVENT);

    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
    verify(runState, never()).endRun();
  }

  @Test
  void settingsOpensSettingsScreen() {
    menu.getEvents().trigger(MainMenuDisplay.SETTINGS_EVENT);

    verify(game).setScreen(GdxGame.ScreenType.SETTINGS);
  }

  @Test
  void exitClosesTheGame() {
    menu.getEvents().trigger(MainMenuDisplay.EXIT_EVENT);

    verify(game).exit();
  }

  @Test
  void debugRoutesAreNotRegistered() {
    menu.getEvents().trigger("map");
    menu.getEvents().trigger("shop");
    menu.getEvents().trigger("battle");

    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
    verify(game, never()).exit();
    verify(runState, never()).endRun();
  }
}
