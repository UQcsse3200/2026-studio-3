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
  void cardRewardOpensAllCopiesReadOnlyAndBackPreservesTheResult() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    AcquisitionResultDisplay view =
        new AcquisitionResultDisplay(stage, UIComponent.getSharedSkin(), null, null);
    try {
      var result =
          new AcquisitionResult(
              "cards",
              List.of(
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.CARD, "bandage", 2, "Bandage", "Heal 6 health.", null),
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.CARD, "strike", 1, "Strike", "Deal 6 damage.", null)));
      List<Boolean> completed = new ArrayList<>();
      view.show(result, () -> completed.add(true), () -> {});
      Table root = stage.getRoot().findActor("acquisition-result-overlay");
      var tile = root.findActor("acquisition-card-reward-bandage");
      assertNull(root.findActor("acquisition-card-reward-strike"));
      assertEquals(
          "CARD REWARD",
          ((com.badlogic.gdx.scenes.scene2d.ui.Label) root.findActor("acquisition-entry-bandage"))
              .getText()
              .toString());
      assertEquals(
          "click to check",
          ((com.badlogic.gdx.scenes.scene2d.ui.Label)
                  root.findActor("acquisition-description-bandage"))
              .getText()
              .toString());
      for (int repeat = 0; repeat < 2; repeat++) {
        for (var listener : tile.getListeners()) {
          if (listener instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener click)
            click.clicked(null, 0f, 0f);
        }
        assertNotNull(root.findActor("acquisition-card-details"));
        assertTrue(!root.findActor("acquisition-result-panel").isVisible());
        for (String id : List.of("bandage-0", "bandage-1", "strike-0")) {
          var card = root.findActor("acquisition-preview-" + id);
          assertNotNull(card);
          assertEquals(com.badlogic.gdx.scenes.scene2d.Touchable.disabled, card.getTouchable());
        }
        root.findActor("acquisition-continue").fire(new ChangeEvent());
        assertTrue(completed.isEmpty());
        root.findActor("acquisition-card-back").fire(new ChangeEvent());
        assertNull(root.findActor("acquisition-card-details"));
        assertTrue(root.findActor("acquisition-result-panel").isVisible());
      }
      root.findActor("acquisition-continue").fire(new ChangeEvent());
      for (int i = 0; i < 40; i++) stage.act(.1f);
      assertEquals(List.of(true), completed);
      assertEquals(2, result.entries().get(0).quantity());
    } finally {
      view.hide();
      stage.dispose();
    }
  }

  @Test
  void cardPreviewUsesThreeReadOnlyCardsPerPageAndBackRestoresResults() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    AcquisitionResultDisplay view =
        new AcquisitionResultDisplay(
            stage, UIComponent.getSharedSkin(), null, null, "SHOP RESULTS");
    try {
      view.show(
          new AcquisitionResult(
              "shop-pages",
              List.of(
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.CARD,
                      "bandage",
                      5,
                      "Bandage",
                      "Heal 6 health.",
                      null))),
          () -> {},
          () -> {});
      Table root = stage.getRoot().findActor("acquisition-result-overlay");
      var tile = root.findActor("acquisition-card-reward-bandage");
      for (var listener : tile.getListeners()) {
        if (listener instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener click)
          click.clicked(null, 0, 0);
      }
      assertNotNull(root.findActor("acquisition-preview-bandage-2"));
      Table preview = root.findActor("acquisition-card-details");
      assertNotNull(preview.getBackground());
      assertNull(root.findActor("acquisition-preview-bandage-3"));
      root.findActor("acquisition-card-next").fire(new ChangeEvent());
      assertNull(root.findActor("acquisition-preview-bandage-0"));
      assertNotNull(root.findActor("acquisition-preview-bandage-4"));
      Table nextPage = root.findActor("acquisition-card-details");
      assertNotNull(nextPage.getBackground());
      assertEquals(
          com.badlogic.gdx.scenes.scene2d.Touchable.disabled,
          root.findActor("acquisition-preview-bandage-4").getTouchable());
      root.findActor("acquisition-card-previous").fire(new ChangeEvent());
      assertNotNull(root.findActor("acquisition-preview-bandage-0"));
      root.findActor("acquisition-card-back").fire(new ChangeEvent());
      assertTrue(root.findActor("acquisition-result-panel").isVisible());
    } finally {
      view.hide();
      stage.dispose();
    }
  }

  @Test
  void spentGoldFliesFromResultToLeftGoldIcon() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    AcquisitionResultDisplay view =
        new AcquisitionResultDisplay(stage, UIComponent.getSharedSkin(), null, null);
    try {
      view.show(
          new AcquisitionResult("payment", List.of(AcquisitionResult.Entry.gold(-10))),
          () -> {},
          () -> {});
      Table root = stage.getRoot().findActor("acquisition-result-overlay");
      root.setSize(1280f, 800f);
      root.validate();
      var source = root.findActor("acquisition-entry-gold");
      var start =
          source.localToActorCoordinates(
              root,
              new com.badlogic.gdx.math.Vector2(source.getWidth() / 2f, source.getHeight() / 2f));
      root.findActor("acquisition-continue").fire(new ChangeEvent());
      var flight = root.findActor("acquisition-flight-gold");
      assertNotNull(flight);
      assertEquals(start.x, flight.getX() + 25f, .01f);
      assertEquals(start.y, flight.getY() + 25f, .01f);
      for (int i = 0; i < 12; i++) stage.act(.1f);
      var destination = root.findActor("acquisition-destination-2");
      var end =
          destination.localToActorCoordinates(
              root,
              new com.badlogic.gdx.math.Vector2(
                  destination.getWidth() / 2f, destination.getHeight() / 2f));
      assertEquals(end.x, flight.getX() + 25f, .01f);
      assertEquals(end.y, flight.getY() + 25f, .01f);
    } finally {
      view.hide();
      stage.dispose();
    }
  }

  @Test
  void actualArtworkRemainsVisibleAndFlightStartsAtTheDisplayedGoldBag() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    var resources = Mockito.mock(com.csse3200.game.services.ResourceService.class);
    var texture = Mockito.mock(com.badlogic.gdx.graphics.Texture.class);
    Mockito.when(texture.getWidth()).thenReturn(192);
    Mockito.when(texture.getHeight()).thenReturn(192);
    Mockito.when(
            resources.getAsset(
                Mockito.anyString(), Mockito.eq(com.badlogic.gdx.graphics.Texture.class)))
        .thenReturn(texture);
    AcquisitionResultDisplay view =
        new AcquisitionResultDisplay(stage, UIComponent.getSharedSkin(), null, resources);
    try {
      var result =
          new AcquisitionResult(
              "art-check",
              List.of(
                  AcquisitionResult.Entry.gold(50),
                  AcquisitionResult.Entry.hp(-10),
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.ITEM,
                      "BRAND_OF_THE_DAMNED",
                      1,
                      "Brand of the Damned",
                      "Every Strength gain is reduced by 1 per copy (minimum 0).",
                      null)));
      List<Boolean> completed = new ArrayList<>();
      view.show(result, () -> completed.add(true), () -> {});
      Table overlay = stage.getRoot().findActor("acquisition-result-overlay");
      overlay.setSize(1280f, 800f);
      overlay.validate();
      var continueButton = overlay.findActor("acquisition-continue");
      assertEquals(360f, continueButton.getWidth(), .01f);
      assertEquals(84f, continueButton.getHeight(), .01f);
      for (String id : List.of("gold", "hp", "BRAND_OF_THE_DAMNED")) {
        var description =
            (com.badlogic.gdx.scenes.scene2d.ui.Label)
                overlay.findActor("acquisition-description-" + id);
        assertTrue(description.getFontScaleX() <= .88f);
        assertTrue(description.getFontScaleX() >= .70f);
        assertTrue(description.getHeight() >= 100f);
      }
      var bag =
          (com.badlogic.gdx.scenes.scene2d.ui.Image) overlay.findActor("acquisition-art-gold");
      var coin =
          (com.badlogic.gdx.scenes.scene2d.ui.Image) overlay.findActor("acquisition-destination-2");
      assertTrue(bag.getDrawable().getMinWidth() > 0);
      assertTrue(coin.getDrawable().getMinWidth() > 0);
      var heart =
          (com.badlogic.gdx.scenes.scene2d.ui.Image) overlay.findActor("acquisition-art-hp");
      assertNotNull(heart);
      assertTrue(heart.getDrawable().getMinWidth() > 0);
      Mockito.verify(resources)
          .getAsset("images/heart.png", com.badlogic.gdx.graphics.Texture.class);
      assertNotNull(overlay.findActor("acquisition-art-BRAND_OF_THE_DAMNED"));
      Mockito.verify(resources)
          .getAsset(
              "images/ui/item-cutouts/brand-of-the-damned.png",
              com.badlogic.gdx.graphics.Texture.class);
      var start = bag.localToActorCoordinates(overlay, new com.badlogic.gdx.math.Vector2(50f, 50f));
      overlay.findActor("acquisition-continue").fire(new ChangeEvent());
      var flight = overlay.findActor("acquisition-flight-gold");
      assertEquals(start.x, flight.getX() + 25f, .01f);
      assertEquals(start.y, flight.getY() + 25f, .01f);
      assertNotNull(overlay.findActor("acquisition-flight-BRAND_OF_THE_DAMNED"));
      assertNull(overlay.findActor("acquisition-flight-hp"));
      for (int i = 0; i < 30; i++) stage.act(.1f);
      assertEquals(List.of(true), completed);
    } finally {
      view.hide();
      stage.dispose();
    }
  }

  @Test
  void mixedConfirmedResultsAreShownAndDismissedWithoutChangingThem() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), Mockito.mock(Batch.class));
    try {
      com.badlogic.gdx.scenes.scene2d.Actor eventBackground =
          new com.badlogic.gdx.scenes.scene2d.Actor();
      eventBackground.setName("original-event-background");
      stage.addActor(eventBackground);
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
      try (AcquisitionPresentation presentation = new AcquisitionPresentation(view)) {
        List<AcquisitionPresentation.Dismissal> reasons = new ArrayList<>();

        presentation.show(result, reasons::add);
        Table overlay = stage.getRoot().findActor("acquisition-result-overlay");
        assertNotNull(overlay);
        assertNotNull(overlay.findActor("acquisition-continue"));
        assertNull(overlay.findActor("acquisition-close"));
        assertEquals(3, result.entries().size());

        overlay.findActor("acquisition-continue").fire(new ChangeEvent());
        overlay.findActor("acquisition-continue").fire(new ChangeEvent());
        assertNotNull(stage.getRoot().findActor("acquisition-result-overlay"));
        for (int i = 0; i < 30; i++) stage.act(.1f);
        assertNull(stage.getRoot().findActor("acquisition-result-overlay"));
        assertEquals(List.of(AcquisitionPresentation.Dismissal.CONTINUE), reasons);
        assertEquals(eventBackground, stage.getRoot().findActor("original-event-background"));
        presentation.close();
        assertEquals(3, result.entries().size());
      }
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
      try (AcquisitionPresentation presentation = new AcquisitionPresentation(view)) {
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
        presentation.close();
        assertTrue(reasons.isEmpty());
        assertNull(stage.getRoot().findActor("acquisition-result-overlay"));
        assertEquals(
            List.of(AcquisitionResult.Entry.gold(60), AcquisitionResult.Entry.hp(-8)),
            result.entries());
      }
    } finally {
      stage.dispose();
    }
  }
}
