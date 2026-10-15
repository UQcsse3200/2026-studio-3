package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class EndBattleScreenTest {
  private Stage stage;
  private Renderer renderer;
  private MockedStatic<RenderFactory> factory;
  private EndBattleScreen realScreen;
  private GdxGame game;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    renderer = mock(Renderer.class);
    factory = mockStatic(RenderFactory.class);
    factory
        .when(RenderFactory::createRenderer)
        .thenAnswer(
            invocation -> {
              ServiceLocator.getRenderService().setStage(stage);
              return renderer;
            });

    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(1672);
    when(texture.getHeight()).thenReturn(941);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    when(resources.containsAsset(anyString(), eq(Texture.class))).thenReturn(true);
    ServiceLocator.registerResourceService(resources);
    game = mock(GdxGame.class);
  }

  @AfterEach
  void tearDown() {
    if (realScreen != null) {
      realScreen.dispose();
    }
    stage.dispose();
    factory.close();
  }

  @Test
  void defeatRequiresExplicitButtonAndRoutesActiveRunToNarrationOnce() {
    RunState runState = mock(RunState.class);
    when(runState.isRunActive()).thenReturn(true);
    when(game.getRunState()).thenReturn(runState);
    realScreen = new EndBattleScreen(game, false);

    stage.keyDown(com.badlogic.gdx.Input.Keys.A);
    stage.getRoot().fire(new InputEvent());
    verify(game, never()).showNarration(anyString(), eq(GdxGame.ScreenType.MAIN_MENU));

    Actor button = stage.getRoot().findActor("returnToMenu");
    assertNotNull(button);
    click(button);
    click(button);
    stage.act(0.16f);
    stage.act(0.01f);

    verify(runState, times(1)).endRun();
    verify(game, times(1)).showNarration("defeat", GdxGame.ScreenType.MAIN_MENU);
    realScreen.render(0f);
    realScreen.resize(1600, 900);
    verify(renderer).render();
    verify(renderer).resize(1600, 900);
  }

  @Test
  void victoryBuildsRewardUiAndCannotBeDismissedBeforeClaim() {
    when(game.getRunState()).thenReturn(new RunState());
    when(game.getCardDiscoveryService())
        .thenReturn(new CardDiscoveryService(CardConfigLoader.loadCards()));
    realScreen = new EndBattleScreen(game, true);

    stage.keyDown(com.badlogic.gdx.Input.Keys.A);
    realScreen.render(0f);
    verify(game, never()).setScreen(GdxGame.ScreenType.MAIN_MENU);
    verify(game, never()).setScreen(GdxGame.ScreenType.MAP);
    verify(renderer).render();
  }

  @Test
  void victoryCannotLeaveUntilRewardClaimedThenContinuesToMapOnce() throws Exception {
    RunState runState = mock(RunState.class);
    when(runState.isRunActive()).thenReturn(true);
    when(game.getRunState()).thenReturn(runState);
    EndBattleScreen screen = screenWithoutUi(true);

    invoke(screen, "returnToMenu");
    verify(game, never()).setScreen(GdxGame.ScreenType.MAP);

    invoke(screen, "onRewardClaimed");
    invoke(screen, "returnToMenu");
    invoke(screen, "returnToMenu");
    verify(game, times(1)).setScreen(GdxGame.ScreenType.MAP);
    verify(runState, never()).endRun();
  }

  @Test
  void eliteVictoryOpensPortalAndFinalVictoryEndsRun() throws Exception {
    RunState elite = mock(RunState.class);
    when(elite.isRunActive()).thenReturn(true);
    when(elite.hasPendingEliteTempleReward()).thenReturn(true);
    when(game.getRunState()).thenReturn(elite);
    EndBattleScreen eliteScreen = screenWithoutUi(true);
    invoke(eliteScreen, "onRewardClaimed");
    invoke(eliteScreen, "returnToMenu");
    verify(game).setScreen(GdxGame.ScreenType.ELITE_PORTAL);

    RunState finalRun = mock(RunState.class);
    when(finalRun.isFinalEncounterCompleted()).thenReturn(true);
    when(game.getRunState()).thenReturn(finalRun);
    EndBattleScreen finalScreen = screenWithoutUi(true);
    invoke(finalScreen, "onRewardClaimed");
    invoke(finalScreen, "returnToMenu");
    verify(finalRun).endRun();
    verify(game).showNarration("victory", GdxGame.ScreenType.MAIN_MENU);
  }

  private EndBattleScreen screenWithoutUi(boolean won) throws Exception {
    EndBattleScreen screen = mock(EndBattleScreen.class, CALLS_REAL_METHODS);
    field("game").set(screen, game);
    field("won").setBoolean(screen, won);
    return screen;
  }

  private static Field field(String name) throws Exception {
    Field field = EndBattleScreen.class.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  private static void invoke(EndBattleScreen screen, String methodName) throws Exception {
    Method method = EndBattleScreen.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(screen);
  }

  private void click(Actor actor) {
    Vector2 position = actor.localToStageCoordinates(new Vector2(10f, 10f));
    InputEvent down = new InputEvent();
    down.setType(InputEvent.Type.touchDown);
    down.setStageX(position.x);
    down.setStageY(position.y);
    down.setPointer(0);
    down.setButton(0);
    actor.fire(down);

    InputEvent up = new InputEvent();
    up.setType(InputEvent.Type.touchUp);
    up.setStageX(position.x);
    up.setStageY(position.y);
    up.setPointer(0);
    up.setButton(0);
    actor.fire(up);
  }
}
