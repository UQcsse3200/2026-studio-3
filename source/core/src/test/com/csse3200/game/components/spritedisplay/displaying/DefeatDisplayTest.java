package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DefeatDisplayTest {
  private Stage stage;
  private Entity entity;
  private AtomicInteger returnEvents;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());

    Texture scene = mock(Texture.class);
    Texture defeat = mock(Texture.class);
    when(scene.getWidth()).thenReturn(1672);
    when(scene.getHeight()).thenReturn(941);
    when(defeat.getWidth()).thenReturn(1672);
    when(defeat.getHeight()).thenReturn(941);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/ui/victory-background-approved.png", Texture.class))
        .thenReturn(scene);
    when(resources.getAsset("images/ui/defeat-screen-mythic-small-title.png", Texture.class))
        .thenReturn(defeat);
    when(resources.getAsset("images/ui/defeat-screen-mythic-intact.png", Texture.class))
        .thenReturn(defeat);
    ServiceLocator.registerResourceService(resources);

    entity = new Entity().addComponent(new DefeatDisplay());
    returnEvents = new AtomicInteger();
    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, returnEvents::incrementAndGet);
    entity.create();
    stage.act(0f);
  }

  @AfterEach
  void tearDown() {
    if (entity != null) {
      entity.dispose();
    }
    stage.dispose();
  }

  @Test
  void backgroundInputDoesNotReturnButButtonReturnsOnlyOnce() {
    InputEvent backgroundClick = new InputEvent();
    backgroundClick.setType(InputEvent.Type.touchDown);
    stage.getRoot().fire(backgroundClick);
    stage.keyDown(com.badlogic.gdx.Input.Keys.A);
    assertEquals(0, returnEvents.get());

    Actor button = stage.getRoot().findActor("returnToMenu");
    Actor highlight = stage.getRoot().findActor("returnButtonHighlight");
    assertNotNull(button);
    assertNotNull(highlight);
    click(button);
    click(button);

    assertEquals(0, returnEvents.get());
    stage.act(0.04f);
    assertEquals(0.18f, highlight.getColor().a, 0.001f);
    stage.act(0.12f);
    stage.act(0.01f);
    assertEquals(1, returnEvents.get());
  }

  @Test
  void focusedButtonAcceptsEnterButIgnoresOtherKeys() {
    Actor button = stage.getRoot().findActor("returnToMenu");
    assertNotNull(button);
    stage.keyDown(com.badlogic.gdx.Input.Keys.A);
    assertEquals(0, returnEvents.get());

    stage.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
    stage.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
    assertEquals(0, returnEvents.get());
    stage.act(0.16f);
    stage.act(0.01f);
    assertEquals(1, returnEvents.get());
  }

  @Test
  void buttonHitTargetKeepsApprovedArtworkDimensions() {
    Actor button = stage.getRoot().findActor("returnToMenu");
    assertNotNull(button);
    float scale = 800f / 941f;
    assertEquals(595f * scale, button.getX(), 0.01f);
    assertEquals(211f * scale, button.getY(), 0.01f);
    assertEquals(482f * scale, button.getWidth(), 0.01f);
    assertEquals(130f * scale, button.getHeight(), 0.01f);

    entity.dispose();
    entity = null;
    assertNull(stage.getRoot().findActor("returnToMenu"));
  }

  @Test
  void defeatArtworkAndReturnButtonKeepLogicalSizeAfterWindowResize() {
    Actor composition = stage.getRoot().findActor("defeatComposition");
    Actor button = stage.getRoot().findActor("returnToMenu");
    assertNotNull(composition);
    assertNotNull(button);
    float width = composition.getWidth();
    float height = composition.getHeight();
    float buttonWidth = button.getWidth();
    float buttonHeight = button.getHeight();

    stage.getViewport().update(1920, 1080, true);
    stage.act(0f);

    assertEquals(1672f * 800f / 941f, composition.getWidth(), 0.01f);
    assertEquals(800f, composition.getHeight(), 0.01f);
    assertEquals(width, composition.getWidth(), 0.01f);
    assertEquals(height, composition.getHeight(), 0.01f);
    assertEquals(buttonWidth, button.getWidth(), 0.01f);
    assertEquals(buttonHeight, button.getHeight(), 0.01f);
  }

  @Test
  void buttonHighlightFadesInAndOutInsteadOfAppearingAbruptly() {
    Actor button = stage.getRoot().findActor("returnToMenu");
    Actor highlight = stage.getRoot().findActor("returnButtonHighlight");
    assertNotNull(button);
    assertNotNull(highlight);
    assertEquals(0f, highlight.getColor().a, 0.001f);

    InputEvent enter = new InputEvent();
    enter.setType(InputEvent.Type.enter);
    button.fire(enter);
    stage.act(0.12f);
    assertEquals(0.08f, highlight.getColor().a, 0.001f);

    InputEvent exit = new InputEvent();
    exit.setType(InputEvent.Type.exit);
    button.fire(exit);
    stage.act(0.12f);
    assertEquals(0f, highlight.getColor().a, 0.001f);
  }

  @Test
  void intactWingHoldsThenBreaksWithDampedShiverAndFragments() {
    Actor intact = stage.getRoot().findActor("intactWingFrame");
    Actor composition = stage.getRoot().findActor("defeatComposition");
    assertNotNull(intact);
    assertNotNull(composition);
    assertTrue(intact.isVisible());
    float originalX = composition.getX();

    for (int i = 0; i < 45; i++) {
      stage.act(1f / 60f);
    }
    assertTrue(intact.isVisible());
    assertEquals(originalX, composition.getX(), 0.01f);
    assertEquals(0, returnEvents.get());

    float smallestX = originalX;
    float largestX = originalX;
    for (int i = 0; i < 42; i++) {
      stage.act(1f / 60f);
      smallestX = Math.min(smallestX, composition.getX());
      largestX = Math.max(largestX, composition.getX());
    }
    assertFalse(intact.isVisible());
    assertNotNull(stage.getRoot().findActor("wingFragment0"));

    for (int i = 0; i < 66; i++) {
      stage.act(1f / 60f);
      smallestX = Math.min(smallestX, composition.getX());
      largestX = Math.max(largestX, composition.getX());
    }
    assertTrue(smallestX < originalX);
    assertTrue(largestX > originalX);
    assertEquals(originalX, composition.getX(), 0.01f);
    assertNull(stage.getRoot().findActor("wingFragment0"));
    assertEquals(0, returnEvents.get());
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
