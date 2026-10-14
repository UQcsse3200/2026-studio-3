package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EndBattleDisplayTest {
  private Stage stage;
  private Entity entity;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
  }

  @AfterEach
  void tearDown() {
    entity.dispose();
    stage.dispose();
  }

  @Test
  void victoryCannotBypassRewardSelectionWithBackgroundInput() {
    AtomicInteger returnEvents = createDisplays();

    entity.getEvents().trigger(EndBattleDisplay.RESULT_EVENT, "VICTORY");
    fireTouchDown();

    assertEquals(0, returnEvents.get());
  }

  @Test
  void defeatBackgroundInputReturnsExactlyOnce() {
    AtomicInteger returnEvents = createDisplays();

    entity.getEvents().trigger(EndBattleDisplay.RESULT_EVENT, "DEFEAT");
    fireTouchDown();

    assertEquals(1, returnEvents.get());
  }

  @Test
  void disposedDisplayCannotHandleStaleBackgroundInput() {
    AtomicInteger returnEvents = createDisplays();
    entity.getEvents().trigger(EndBattleDisplay.RESULT_EVENT, "DEFEAT");

    entity.dispose();
    fireTouchDown();

    assertEquals(0, returnEvents.get());
  }

  private AtomicInteger createDisplays() {
    DisplayingRecord heading =
        DisplayingRecord.builder("")
            .trigger(EndBattleDisplay.RESULT_EVENT)
            .variant("endBattle")
            .build();
    DisplayingRecord hint =
        DisplayingRecord.builder("Click anywhere to continue").variant("endBattle").build();
    DisplayingFactory factory = new DisplayingFactory(List.of(heading, hint));
    entity = new Entity().addComponent(factory);
    AtomicInteger returnEvents = new AtomicInteger();
    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, returnEvents::incrementAndGet);
    entity.create();
    return returnEvents;
  }

  private void fireTouchDown() {
    InputEvent event = new InputEvent();
    event.setType(InputEvent.Type.touchDown);
    stage.getRoot().fire(event);
  }
}
