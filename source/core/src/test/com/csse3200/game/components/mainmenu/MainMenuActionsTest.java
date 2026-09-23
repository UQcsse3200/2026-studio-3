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
  void loadOpensSaveLoadScreen() {
    menu.getEvents().trigger(MainMenuDisplay.LOAD_EVENT);

    verify(game).setScreen(GdxGame.ScreenType.SAVE_LOAD);
    verify(runState, never()).endRun();
  }

  @Test
  void bestiaryOpensLibraryScreen() {
    menu.getEvents().trigger(MainMenuDisplay.BESTIARY_EVENT);

    verify(game).setScreen(GdxGame.ScreenType.LIBRARY);
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
  void demoEventOpensMapFreePreviewWithoutResettingTheRun() {
    menu.getEvents().trigger(MainMenuDisplay.DEMO_EVENT_EVENT);

    verify(game).openDemoEvent();
    verify(runState, never()).endRun();
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void demoCampfireOpensMapFreePreviewWithoutResettingTheRun() {
    menu.getEvents().trigger(MainMenuDisplay.DEMO_CAMPFIRE_EVENT);

    verify(game).openDemoCampfire();
    verify(runState, never()).endRun();
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void demoFusionOpensMapFreePreviewWithoutResettingTheRun() {
    menu.getEvents().trigger(MainMenuDisplay.DEMO_FUSION_EVENT);

    verify(game).openDemoCardFusion();
    verify(runState, never()).endRun();
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
  }

  @Test
  void debugRoutesAreNotRegistered() {
    menu.getEvents().trigger("map");
    menu.getEvents().trigger("shop");
    menu.getEvents().trigger("battle");
    menu.getEvents().trigger("demoShop");

    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
    verify(game, never()).exit();
    verify(runState, never()).endRun();
  }
}
