package com.csse3200.game.components.mainmenu;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import java.nio.file.Path;
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
    menu.getEvents().trigger("start");

    verify(runState).endRun();
    verify(game).setScreen(GdxGame.ScreenType.MAP);
  }

  @Test
  void loadReturnsToAnActiveRunOnTheMap() {
    when(runState.isRunActive()).thenReturn(true);

    menu.getEvents().trigger("load");

    verify(game).setScreen(GdxGame.ScreenType.MAP);
  }

  @Test
  void loadDoesNothingWithoutAnActiveRun() {
    when(runState.isRunActive()).thenReturn(false);

    menu.getEvents().trigger("load");

    verify(game, never()).setScreen(GdxGame.ScreenType.MAP);
  }

  @Test
  void mainMenuConfigurationProvidesAStartButton() {
    assertTrue(
        ClickableFactory.loadRecordsFromJson(Path.of("sprites/MainMenuUi.json")).stream()
            .anyMatch(record -> "start".equals(record.trigger())));
  }
}
