package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ClickableVisualContentTest {
  @Test
  void shouldEmbedPresentationInsideExistingInteractionButton() {
    InOutOnTrigger clickable =
        new InOutOnTrigger(ClickableRecord.builder("playCard").text("legacy image").build());
    Actor content = new Actor();

    clickable.setVisualContent(() -> content);

    assertEquals(1, clickable.getBtn().getChildren().size);
    assertSame(content, clickable.getBtn().getChildren().first());
  }

  @Test
  void shouldShadeEveryEmbeddedActorWhenClickableIsDisabled() {
    InOutOnTrigger clickable =
        new InOutOnTrigger(
            ClickableRecord.builder("playCard").text("legacy image").disabled(true).build());
    Group content = new Group();
    Actor nested = new Actor();
    content.addActor(nested);

    clickable.setVisualContent(() -> content);

    assertEquals(Touchable.disabled, clickable.getBtn().getTouchable());
    assertEquals(0.35f, content.getColor().r);
    assertEquals(0.35f, nested.getColor().r);
  }
}
