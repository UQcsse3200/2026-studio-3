package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlayNarrationCommandTest {
  private Application previousApp;
  private Application application;
  private final GdxGame game = mock(GdxGame.class);
  private final PlayNarrationCommand command = new PlayNarrationCommand(game);

  @BeforeEach
  void setUp() {
    previousApp = Gdx.app;
    application = mock(Application.class);
    Gdx.app = application;
  }

  @AfterEach
  void restoreApp() {
    Gdx.app = previousApp;
  }

  @Test
  void nullGameThrows() {
    assertThrows(IllegalArgumentException.class, () -> new PlayNarrationCommand(null));
  }

  @Test
  void noArgumentsReturnsFalse() {
    assertFalse(command.action(new ArrayList<>()));
    verifyNoInteractions(game, application);
  }

  @Test
  void twoArgumentsReturnsFalse() {
    assertFalse(command.action(new ArrayList<>(List.of("victory", "defeat"))));
    verifyNoInteractions(game, application);
  }

  @Test
  void unknownIdReturnsFalse() {
    assertFalse(command.action(new ArrayList<>(List.of("credits"))));
    verifyNoInteractions(game, application);
  }

  @Test
  void validIdDefersNarrationAndReturnsToMap() {
    assertTrue(command.action(new ArrayList<>(List.of("victory"))));
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(application).postRunnable(captor.capture());
    verifyNoInteractions(game);

    captor.getValue().run();

    verify(game).showNarration("victory", GdxGame.ScreenType.MAP);
  }
}
