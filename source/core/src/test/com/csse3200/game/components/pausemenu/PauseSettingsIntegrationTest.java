package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PauseSettingsIntegrationTest {
  private GdxGame game;
  private GamePauseService pauseService;
  private PauseMenuDisplay display;
  private Entity pauseMenu;

  @BeforeEach
  void setUp() {
    DisplayMode mode = new TestDisplayMode(1280, 800, 60, 32);
    Graphics graphics = mock(Graphics.class);
    when(graphics.getDisplayMode()).thenReturn(mode);
    when(graphics.getDisplayModes(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new DisplayMode[] {mode});
    Gdx.graphics = graphics;

    RenderService renderService = new RenderService();
    renderService.setStage(new Stage(new ScreenViewport(), mock(SpriteBatch.class)));
    ServiceLocator.registerRenderService(renderService);
    pauseService = new GamePauseService(new GameTime());
    ServiceLocator.registerPauseService(pauseService);

    game = mock(GdxGame.class);
    display = new PauseMenuDisplay();
    pauseMenu = new Entity().addComponent(display).addComponent(new PauseMenuActions(game));
    pauseMenu.create();
  }

  @Test
  void settingsBackKeepsGameplayPausedUntilResume() {
    pauseMenu.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
    click(display.getSettingsButton());

    assertTrue(pauseService.isPaused());
    assertTrue(display.isSettingsVisible());
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));

    click(display.getSettingsBackButton());

    assertTrue(pauseService.isPaused());
    assertTrue(display.isMenuVisible());
    assertFalse(display.isSettingsVisible());
    verify(game, never()).setScreen(any(GdxGame.ScreenType.class));

    click(display.getResumeButton());
    assertFalse(pauseService.isPaused());
    assertFalse(display.isMenuVisible());
  }

  private static void click(com.badlogic.gdx.scenes.scene2d.Actor actor) {
    actor.fire(new ChangeListener.ChangeEvent());
  }

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
