package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.narration.NarrationConfigLoader;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class NarrationScreenTest {
  private static String expectedPassage(String sequenceId, int index) {
    return String.join("\n", NarrationConfigLoader.loadSequence(sequenceId).get(index));
  }

  private Stage stage;
  private MockedStatic<RenderFactory> factory;
  private NarrationScreen screen;
  private Runnable callback;
  private Renderer renderer;

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
    callback = mock(Runnable.class);
  }

  @AfterEach
  void tearDown() {
    if (screen != null) {
      screen.dispose();
    }
    stage.dispose();
    factory.close();
  }

  private Label passage() {
    return (Label) ((Table) stage.getActors().get(0)).getChildren().first();
  }

  private void advance(float seconds) {
    for (int i = 0; i < Math.round(seconds * 100); i++) {
      stage.act(0.01f);
      screen.render(0.01f);
    }
  }

  @Test
  void fadesHoldsAdvancesAndCompletesExactlyOnce() {
    screen = new NarrationScreen("opening", callback);
    advance(0.3f);
    assertEquals(expectedPassage("opening", 0), passage().getText().toString());
    assertTrue(passage().getColor().a > 0f && passage().getColor().a < 1f);
    advance(1f);
    assertEquals(1f, passage().getColor().a, 0.01f);
    advance(2.1f);
    assertTrue(passage().getColor().a < 1f);
    advance(0.7f);
    assertEquals(expectedPassage("opening", 1), passage().getText().toString());
    verifyNoInteractions(callback);
    advance(8f);
    verify(callback, times(1)).run();
  }

  @Test
  void anyKeySkipsWholeSequence() {
    screen = new NarrationScreen("opening", callback);
    stage.keyDown(Input.Keys.A);
    screen.render(0f);
    advance(15f);
    stage.keyDown(Input.Keys.SPACE);
    screen.render(0f);
    verify(callback, times(1)).run();
    assertEquals(0, passage().getActions().size);
  }

  @Test
  void clickSkipsWholeSequence() {
    screen = new NarrationScreen("pre_boss", callback);
    advance(1f);
    float initialAlpha = passage().getColor().a;
    click();
    assertEquals(initialAlpha, passage().getColor().a);
    advance(NarrationScreen.FADE_SECONDS / 2f);
    assertTrue(passage().getColor().a > 0f && passage().getColor().a < initialAlpha);
    verifyNoInteractions(callback);
    click();
    advance(NarrationScreen.FADE_SECONDS / 2f + 0.05f);
    verify(callback, times(1)).run();
    assertEquals(0, passage().getActions().size);
    assertEquals(expectedPassage("pre_boss", 0), passage().getText().toString());
    advance(15f);
    verify(callback, times(1)).run();
  }

  private void click() {
    InputEvent click = new InputEvent();
    click.setType(InputEvent.Type.touchDown);
    stage.getRoot().fire(click);
  }

  @Test
  void repeatedInputDuringNaturalFadeOutDoesNotRestartSkipFade() {
    screen = new NarrationScreen("opening", callback);
    advance(3.4f);
    float alpha = passage().getColor().a;
    assertTrue(alpha > 0f && alpha < 1f);
    click();
    advance(0.3f);
    click();
    stage.keyDown(Input.Keys.A);
    advance(0.35f);
    verify(callback, times(1)).run();
    advance(10f);
    verify(callback, times(1)).run();
  }

  @Test
  void disposalDuringSkipFadeCancelsCompletion() {
    screen = new NarrationScreen("opening", callback);
    advance(1f);
    click();
    advance(0.3f);
    screen.dispose();
    advance(1f);
    verifyNoInteractions(callback);
  }

  @Test
  void disposalInsideRenderSuppressesPendingCompletion() {
    screen = new NarrationScreen("unknown", callback);
    doAnswer(
            invocation -> {
              screen.dispose();
              return null;
            })
        .when(renderer)
        .render();
    screen.render(0f);
    verifyNoInteractions(callback);
  }

  @Test
  void unknownIdCompletesOnFirstRenderNotDuringConstruction() {
    screen = new NarrationScreen("unknown", callback);
    verifyNoInteractions(callback);
    screen.render(0f);
    screen.render(0f);
    verify(callback, times(1)).run();
  }

  @Test
  void disposalCancelsPlaybackWithoutCallingCompletion() {
    screen = new NarrationScreen("opening", callback);
    screen.dispose();
    stage.keyDown(Input.Keys.A);
    screen.render(0f);
    verifyNoInteractions(callback);
  }

  @Test
  void callbackCanDisposeScreenSafely() {
    screen = new NarrationScreen("unknown", () -> screen.dispose());
    assertDoesNotThrow(() -> screen.render(0f));
    assertDoesNotThrow(() -> screen.render(0f));
  }
}
