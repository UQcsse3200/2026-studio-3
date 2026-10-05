package com.csse3200.game.components.settingsmenu;

import static org.mockito.Mockito.mock;
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
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SettingsMenuDisplayTest {
  private GdxGame game;
  private SettingsMenuDisplay display;

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

    game = mock(GdxGame.class);
    display = new SettingsMenuDisplay(game);
    new Entity().addComponent(display).create();
  }

  @Test
  void hostsReusablePanelAndBackReturnsToMainMenu() {
    display.getSettingsPanel().getBackButton().fire(new ChangeListener.ChangeEvent());

    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
  }

  private static class TestDisplayMode extends DisplayMode {
    TestDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
