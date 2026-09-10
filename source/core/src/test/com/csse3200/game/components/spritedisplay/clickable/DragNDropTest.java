package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.ObjectMap;
import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragNDropTest {
  @Test
  void shouldSendCardAndTargetIdsToSourceEntityAfterDrop() throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);

    DragNDrop card =
        new DragNDrop(
            ClickableRecord.builder("playCard")
                .text("Strike")
                .args("strike")
                .variant("drag")
                .build());
    Entity battleUi = new Entity().addComponent(card);
    AtomicReference<CardPlayRequest> received = new AtomicReference<>();
    battleUi
        .getEvents()
        .addListener(
            "playCard",
            (String cardId, String targetId) ->
                received.set(new CardPlayRequest(cardId, targetId)));

    DragAndDrop.Source source = getOnlySource(dragService.getDragAndDrop());
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0f, 0f, 0);
    Actor targetActor = new Actor();
    targetActor.setUserObject("bone_crawler");
    DragAndDrop.Target target = targetFor(targetActor);

    source.dragStop(new InputEvent(), 0f, 0f, 0, payload, target);

    assertEquals(new CardPlayRequest("strike", "bone_crawler"), received.get());
  }

  private DragAndDrop.Target targetFor(Actor actor) {
    return new DragAndDrop.Target(actor) {
      @Override
      public boolean drag(
          DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
        return true;
      }

      @Override
      public void drop(
          DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {}
    };
  }

  @SuppressWarnings("unchecked")
  private DragAndDrop.Source getOnlySource(DragAndDrop dragAndDrop) throws Exception {
    Field field = DragAndDrop.class.getDeclaredField("sourceListeners");
    field.setAccessible(true);
    ObjectMap<DragAndDrop.Source, DragListener> sources =
        (ObjectMap<DragAndDrop.Source, DragListener>) field.get(dragAndDrop);
    return sources.keys().next();
  }
}
