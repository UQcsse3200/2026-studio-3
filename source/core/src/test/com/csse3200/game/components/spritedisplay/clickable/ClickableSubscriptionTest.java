package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A rebuilt widget must stop reacting to the shared entity's "up"/"down" events once removed,
 * otherwise every hand rebuild leaves the old cards subscribed (and alive) for the whole battle.
 */
@ExtendWith(GameExtension.class)
class ClickableSubscriptionTest {

  @Test
  void inOutReactsToDownWhileLive() {
    Entity entity = new Entity();
    InOutOnTrigger card = created(new InOutOnTrigger(record()), entity);

    entity.getEvents().trigger("down");

    assertEquals(Touchable.disabled, card.getBtn().getTouchable());
  }

  @Test
  void inOutIgnoresDownAfterRemove() {
    Entity entity = new Entity();
    InOutOnTrigger card = created(new InOutOnTrigger(record()), entity);

    card.remove();
    entity.getEvents().trigger("down");

    assertEquals(Touchable.enabled, card.getBtn().getTouchable());
  }

  @Test
  void gatedReactsToDownWhileLive() {
    Entity entity = new Entity();
    GatedClickable button = created(new GatedClickable(record()), entity);

    entity.getEvents().trigger("down");

    assertEquals(Touchable.disabled, button.getBtn().getTouchable());
  }

  @Test
  void gatedIgnoresDownAfterRemove() {
    Entity entity = new Entity();
    GatedClickable button = created(new GatedClickable(record()), entity);

    button.remove();
    entity.getEvents().trigger("down");

    assertEquals(Touchable.enabled, button.getBtn().getTouchable());
  }

  private static ClickableRecord record() {
    return ClickableRecord.builder("playCard").text("card").build();
  }

  private static <T extends Clickable> T created(T clickable, Entity entity) {
    clickable.setEntity(entity);
    clickable.create();
    return clickable;
  }
}
