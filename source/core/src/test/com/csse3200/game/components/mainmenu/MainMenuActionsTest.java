package com.csse3200.game.components.mainmenu;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MainMenuActionsTest {
  private GdxGame game;
  private Entity menu;

  @BeforeEach
  void setUp() {
    game = mock(GdxGame.class);

    menu = new Entity().addComponent(new MainMenuActions(game));
    menu.create();
  }

  @Test
  void startBeginsANewRunOnTheMap() {
    menu.getEvents().trigger(MainMenuDisplay.START_EVENT);

    verify(game).startNewRun();
  }

  @Test
  void enterTutorialStartsTheDedicatedBattleOnly() {
    menu.getEvents().trigger(MainMenuDisplay.ENTER_TUTORIAL_EVENT);

    verify(game).startTutorialBattle();
    verify(game, never()).startNewRun();
  }

  @Test
  void loadOpensSaveLoadScreen() {
    menu.getEvents().trigger(MainMenuDisplay.LOAD_EVENT);

    verify(game).setScreen(GdxGame.ScreenType.SAVE_LOAD);
    verify(game, never()).startNewRun();
  }

  @Test
  void bestiaryOpensLibraryScreen() {
    menu.getEvents().trigger(MainMenuDisplay.BESTIARY_EVENT);

    verify(game).setScreen(GdxGame.ScreenType.LIBRARY);
    verify(game, never()).startNewRun();
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
    menu.getEvents().trigger("demoEvent");
    menu.getEvents().trigger("demoShop");
    menu.getEvents().trigger("demoCampfire");
    menu.getEvents().trigger("demoFusion");

    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
    verify(game, never()).exit();
    verify(game, never()).openDemoEvent();
    verify(game, never()).openDemoShop();
    verify(game, never()).openDemoCampfire();
    verify(game, never()).openDemoCardFusion();
    verify(game, never()).startNewRun();
  }
}
