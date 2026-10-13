package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import java.util.List;
import org.junit.jupiter.api.Test;

class VictoryAnimationControllerTest {
  @Test
  void entranceBecomesInteractiveOnlyAfterPresentationCompletes() {
    Actor background = actor();
    Actor scrim = actor();
    Actor panel = actor();
    Actor title = actor();
    Actor firstCard = actor();
    Actor secondCard = actor();

    VictoryAnimationController controller =
        new VictoryAnimationController(
            background, scrim, panel, title, List.of(firstCard, secondCard));

    controller.playEntrance();

    assertEquals(VictoryAnimationController.State.ENTERING, controller.getState());
    assertEquals(Touchable.disabled, firstCard.getTouchable());
    assertEquals(Touchable.disabled, secondCard.getTouchable());

    advance(1.5f, background, scrim, panel, title, firstCard, secondCard);

    assertEquals(VictoryAnimationController.State.READY, controller.getState());
    assertEquals(Touchable.enabled, firstCard.getTouchable());
    assertEquals(Touchable.enabled, secondCard.getTouchable());
    assertEquals(1f, firstCard.getColor().a, 0.001f);
    assertEquals(1f, firstCard.getScaleX(), 0.001f);
  }

  @Test
  void entranceCannotBeRestarted() {
    Actor background = actor();
    Actor scrim = actor();
    Actor panel = actor();
    Actor title = actor();
    Actor card = actor();

    VictoryAnimationController controller =
        new VictoryAnimationController(background, scrim, panel, title, List.of(card));

    controller.playEntrance();
    advance(1.5f, background, scrim, panel, title, card);
    controller.playEntrance();

    assertEquals(VictoryAnimationController.State.READY, controller.getState());
    assertEquals(0, panel.getActions().size);
    assertEquals(0, card.getActions().size);
  }

  @Test
  void hoverHighlightsOneChoiceAndRestoresAllChoices() {
    Actor background = actor();
    Actor scrim = actor();
    Actor panel = actor();
    Actor title = actor();
    Actor firstCard = actor();
    Actor secondCard = actor();

    VictoryAnimationController controller =
        new VictoryAnimationController(
            background, scrim, panel, title, List.of(firstCard, secondCard));
    controller.playEntrance();
    advance(1.5f, background, scrim, panel, title, firstCard, secondCard);

    controller.showHover(firstCard);
    advance(0.2f, firstCard, secondCard);

    assertEquals(1.045f, firstCard.getScaleX(), 0.001f);
    assertEquals(1f, firstCard.getColor().a, 0.001f);
    assertEquals(0.68f, secondCard.getColor().a, 0.001f);

    controller.clearHover(firstCard);
    advance(0.2f, firstCard, secondCard);

    assertEquals(1f, firstCard.getScaleX(), 0.001f);
    assertEquals(1f, firstCard.getColor().a, 0.001f);
    assertEquals(1f, secondCard.getColor().a, 0.001f);
  }

  @Test
  void disposeStopsActionsAndPreventsReadyTransition() {
    Actor background = actor();
    Actor scrim = actor();
    Actor panel = actor();
    Actor title = actor();
    Actor card = actor();

    VictoryAnimationController controller =
        new VictoryAnimationController(background, scrim, panel, title, List.of(card));
    controller.playEntrance();

    controller.dispose();
    advance(1f, background, scrim, panel, title, card);

    assertEquals(VictoryAnimationController.State.DISPOSED, controller.getState());
    assertEquals(0, background.getActions().size);
    assertEquals(0, panel.getActions().size);
    assertEquals(0, card.getActions().size);
  }

  @Test
  void titleLettersRevealFromLeftToRight() {
    Actor background = actor();
    Actor scrim = actor();
    Actor panel = actor();
    Actor title = actor();
    Actor firstLetter = actor();
    Actor secondLetter = actor();
    Actor subtitle = actor();
    Actor sunPulse = actor();

    VictoryAnimationController controller =
        new VictoryAnimationController(
            background,
            scrim,
            panel,
            title,
            List.of(firstLetter, secondLetter),
            subtitle,
            sunPulse,
            List.of(),
            List.of());

    controller.playEntrance();
    advance(0.26f, background, scrim, panel, firstLetter, secondLetter, subtitle, sunPulse);

    assertTrue(firstLetter.getColor().a > 0f);
    assertEquals(0f, secondLetter.getColor().a, 0.001f);

    advance(0.5f, firstLetter, secondLetter, subtitle, sunPulse);

    assertEquals(1f, firstLetter.getColor().a, 0.001f);
    assertEquals(1f, secondLetter.getColor().a, 0.001f);
  }

  private static Actor actor() {
    Actor actor = new Actor();
    actor.setSize(100f, 160f);
    actor.setTouchable(Touchable.enabled);
    return actor;
  }

  private static void advance(float duration, Actor... actors) {
    float elapsed = 0f;
    while (elapsed < duration) {
      float delta = Math.min(1f / 60f, duration - elapsed);
      for (Actor actor : actors) {
        actor.act(delta);
      }
      elapsed += delta;
    }
  }
}
