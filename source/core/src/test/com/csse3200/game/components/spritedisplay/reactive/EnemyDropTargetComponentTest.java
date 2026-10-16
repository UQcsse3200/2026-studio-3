package com.csse3200.game.components.spritedisplay.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyDropTargetComponentTest {

  // Twice the 1280x800 UI size, so window pixels and stage units differ by a factor of two.
  private static final int WINDOW_WIDTH = 2560;
  private static final int WINDOW_HEIGHT = 1600;

  private Graphics previousGraphics;
  private Stage stage;
  private OrthographicCamera worldCamera;
  private DragAndDrop dragAndDrop;

  @BeforeEach
  void setUp() {
    previousGraphics = Gdx.graphics;
    Graphics graphics = mock(Graphics.class);
    when(graphics.getWidth()).thenReturn(WINDOW_WIDTH);
    when(graphics.getHeight()).thenReturn(WINDOW_HEIGHT);
    when(graphics.getBackBufferWidth()).thenReturn(WINDOW_WIDTH);
    when(graphics.getBackBufferHeight()).thenReturn(WINDOW_HEIGHT);
    Gdx.graphics = graphics;

    // Same viewport as Renderer.
    stage = new Stage(new ExtendViewport(1280, 800), mock(Batch.class));
    stage.getViewport().update(WINDOW_WIDTH, WINDOW_HEIGHT, true);
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);

    // World camera centred on (10, 5), spanning the whole window.
    worldCamera = new OrthographicCamera(20f, 12.5f);
    worldCamera.position.set(10f, 5f, 0f);
    worldCamera.update();

    dragAndDrop = mock(DragAndDrop.class);
  }

  @AfterEach
  void tearDown() {
    Gdx.graphics = previousGraphics;
  }

  @Test
  void hitboxIsInStageUnitsWhenWindowIsNotUiSize() {
    Entity enemy = new Entity();
    enemy.setPosition(9.5f, 4.5f); // default scale 1, so the centre is (10, 5): mid-window
    EnemyDropTargetComponent target = new EnemyDropTargetComponent(dragAndDrop, worldCamera, "7");
    target.setEntity(enemy);
    target.create();

    Actor hitbox = stage.getActors().first();

    // Mid-window is (1280, 800) in window pixels but (640, 400) in stage units.
    assertEquals(640f, hitbox.getX() + hitbox.getWidth() / 2f, 0.01f);
    assertEquals(400f, hitbox.getY() + hitbox.getHeight() / 2f, 0.01f);
    assertEquals("7", hitbox.getUserObject());
  }

  @Test
  void disposeRemovesTheDropTarget() {
    EnemyDropTargetComponent target = new EnemyDropTargetComponent(dragAndDrop, worldCamera, "7");
    target.setEntity(new Entity());
    target.create();

    target.dispose();

    verify(dragAndDrop).addTarget(any(DragAndDrop.Target.class));
    verify(dragAndDrop).removeTarget(any(DragAndDrop.Target.class));
    assertEquals(0, stage.getActors().size);
  }
}
