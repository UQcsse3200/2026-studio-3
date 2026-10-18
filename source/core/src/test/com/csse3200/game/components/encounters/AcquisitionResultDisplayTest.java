package com.csse3200.game.components.encounters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.encounters.results.AcquisitionPresentation;
import com.csse3200.game.encounters.results.AcquisitionResult;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class AcquisitionResultDisplayTest {
  @Test
  void mixedConfirmedResultsAreShownAndDismissedWithoutChangingThem() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    try {
      AcquisitionResult result =
          new AcquisitionResult(
              "event:123",
              List.of(
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.CARD, "strike", 2, "Strike", "Added to deck", null),
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.ITEM, "relic", 1, "Relic", "Owned item", null),
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.NEGATIVE_EFFECT,
                      "curse",
                      3,
                      "Curse",
                      "Lose health each turn",
                      2)));
      AcquisitionResultDisplay view =
          new AcquisitionResultDisplay(stage, UIComponent.getSharedSkin(), null, null);
      AcquisitionPresentation presentation = new AcquisitionPresentation(view);
      List<AcquisitionPresentation.Dismissal> reasons = new ArrayList<>();

      presentation.show(result, reasons::add);
      Table overlay = stage.getRoot().findActor("acquisition-result-overlay");
      assertNotNull(overlay);
      assertNotNull(overlay.findActor("acquisition-continue"));
      assertNotNull(overlay.findActor("acquisition-close"));
      assertEquals(3, result.entries().size());

      overlay.findActor("acquisition-continue").fire(new ChangeEvent());
      assertNull(stage.getRoot().findActor("acquisition-result-overlay"));
      assertEquals(List.of(AcquisitionPresentation.Dismissal.CONTINUE), reasons);
      presentation.close();
      assertEquals(3, result.entries().size());
    } finally {
      stage.dispose();
    }
  }

  @Test
  void resourceResultUsesSignedChangeAndClosingDoesNotGrantItAgain() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    try {
      AcquisitionResult result =
          new AcquisitionResult(
              "event:mine",
              List.of(AcquisitionResult.Entry.gold(60), AcquisitionResult.Entry.hp(-8)));
      AcquisitionResultDisplay view =
          new AcquisitionResultDisplay(stage, UIComponent.getSharedSkin(), null, null);
      AcquisitionPresentation presentation = new AcquisitionPresentation(view);
      List<AcquisitionPresentation.Dismissal> reasons = new ArrayList<>();
      assertTrue(presentation.show(result, reasons::add));
      Table overlay = stage.getRoot().findActor("acquisition-result-overlay");
      assertEquals(
          "Gold +60",
          ((com.badlogic.gdx.scenes.scene2d.ui.Label) overlay.findActor("acquisition-entry-gold"))
              .getText()
              .toString());
      assertEquals(
          "HP -8",
          ((com.badlogic.gdx.scenes.scene2d.ui.Label) overlay.findActor("acquisition-entry-hp"))
              .getText()
              .toString());
      overlay.findActor("acquisition-close").fire(new ChangeEvent());
      assertEquals(List.of(AcquisitionPresentation.Dismissal.CLOSE), reasons);
      assertNull(stage.getRoot().findActor("acquisition-result-overlay"));
      assertEquals(
          List.of(AcquisitionResult.Entry.gold(60), AcquisitionResult.Entry.hp(-8)),
          result.entries());
    } finally {
      stage.dispose();
    }
  }
}
