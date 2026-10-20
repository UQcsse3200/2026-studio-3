package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class TutorialVictoryScreenTest {
  private Stage stage;
  private Renderer renderer;
  private ResourceService resources;
  private MockedStatic<RenderFactory> factory;
  private MockedStatic<AudioService> audio;
  private TutorialVictoryScreen screen;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    renderer = mock(Renderer.class);
    when(renderer.getStage()).thenReturn(stage);
    factory = mockStatic(RenderFactory.class);
    factory
        .when(RenderFactory::createRenderer)
        .thenAnswer(
            invocation -> {
              ServiceLocator.getRenderService().setStage(stage);
              return renderer;
            });
    audio = mockStatic(AudioService.class);
    resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(1024);
    when(texture.getHeight()).thenReturn(626);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
  }

  @AfterEach
  void tearDown() {
    if (screen != null) {
      screen.dispose();
    }
    stage.dispose();
    factory.close();
    audio.close();
  }

  private TextButton continueButton() {
    return stage.getRoot().findActor("tutorial-victory-continue");
  }

  @Test
  void waitsForContinueAndStartsRunAfterTutorialOnlyOnceOutsideInputDispatch() {
    GdxGame game = mock(GdxGame.class);
    screen = new TutorialVictoryScreen(game);
    screen.render(0f);
    verifyNoInteractions(game);

    continueButton().fire(new ChangeEvent());
    continueButton().fire(new ChangeEvent());
    assertTrue(continueButton().isDisabled());
    verifyNoInteractions(game);

    screen.render(0f);
    screen.render(0f);
    continueButton().fire(new ChangeEvent());
    verify(game, times(1)).startRunAfterTutorial();
    verifyNoMoreInteractions(game);
  }

  @Test
  void disposalCancelsPendingContinueAndUnloadsAssetsOnlyOnce() {
    GdxGame game = mock(GdxGame.class);
    screen = new TutorialVictoryScreen(game);
    continueButton().fire(new ChangeEvent());
    screen.dispose();
    screen.dispose();
    screen.render(0f);

    verifyNoInteractions(game);
    verify(renderer, times(1)).dispose();
    verify(resources, times(1)).unloadAssets(any(String[].class));
  }

  @Test
  void continueResetsRunAndOpensMapWithoutRepeatingNarration() {
    GdxGame game = new GdxGame();
    game.getRunState()
        .restoreRun(new MapGraph(Map.of(7, new MapNode(7, RoomType.COMBAT)), false), 7);
    PlayerRunState oldPlayer = game.getRunState().getOrCreatePlayerState();
    oldPlayer.restore(1, 200, 999);
    game.getRunState().setPendingReward(RewardOption.gold(15));
    game.getRunState().setPendingEliteTempleReward(true);
    screen = new TutorialVictoryScreen(game);
    game.setScreen(screen);

    try (MockedConstruction<NarrationScreen> narrations = mockConstruction(NarrationScreen.class);
        MockedConstruction<MapScreen> maps = mockConstruction(MapScreen.class)) {
      continueButton().fire(new ChangeEvent());
      assertSame(screen, game.getScreen());
      screen.render(0f);

      assertTrue(narrations.constructed().isEmpty());
      assertEquals(1, maps.constructed().size());
      assertSame(maps.constructed().get(0), game.getScreen());
      assertFalse(game.getRunState().isRunActive());
      assertNull(game.getRunState().getActiveNodeId());
      assertNull(game.getRunState().getPendingReward());
      assertFalse(game.getRunState().hasPendingEliteTempleReward());
      assertNotSame(oldPlayer, game.getRunState().getOrCreatePlayerState());
      assertNotEquals(999, game.getRunState().getOrCreatePlayerState().getGold());
    }
  }

  @Test
  void panelAndContinueRemainInsideTheViewportAfterResize() {
    screen = new TutorialVictoryScreen(mock(GdxGame.class));
    Table root = (Table) stage.getActors().peek();
    root.validate();
    assertTrue(continueButton().getWidth() > 0f);

    stage.getViewport().update(1600, 900, true);
    screen.resize(1600, 900);
    root.validate();
    var panel = stage.getRoot().findActor("tutorial-victory-panel");
    assertTrue(panel.getX() >= 0f);
    assertTrue(panel.getY() >= 0f);
    assertTrue(panel.getRight() <= stage.getWidth());
    assertTrue(panel.getTop() <= stage.getHeight());
    verify(renderer).resize(1600, 900);
  }
}
