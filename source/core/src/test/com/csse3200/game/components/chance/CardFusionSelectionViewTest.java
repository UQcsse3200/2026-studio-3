package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.fusion.CardFusionService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.chance.CardFusionEncounterBehaviour;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.CardFusionEncounterFlow;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceOutcomeApplier;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardFusionSelectionViewTest {
  private Stage stage;
  private Entity entity;
  private CardLibrary cardLibrary;
  private RunState runState;
  private AtomicInteger completions;
  private ChanceEncounterSession session;
  private ChanceEncounterDisplay display;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);

    cardLibrary = new CardLibrary(CardConfigLoader.loadCards());
    ServiceLocator.registerCardLibrary(cardLibrary);
    runState = new RunState();
    completions = new AtomicInteger();
    ChanceEncounter encounter =
        new ChanceEncounter(
            CardFusionEncounterBehaviour.ENCOUNTER_ID,
            "Fuse three Common cards.",
            List.of(
                new ChanceChoice("fuse", "Choose three cards.", new ChanceOutcome(0, 0)),
                new ChanceChoice("leave", "Leave the forge.", new ChanceOutcome(0, 0))));
    session =
        new ChanceEncounterSession(
            7,
            encounter,
            new CardFusionEncounterBehaviour(),
            new ChanceOutcomeApplier(new MockPlayerStateGateway(100, 50)),
            (nodeId, success) -> completions.incrementAndGet());
    CardFusionEncounterFlow flow =
        new CardFusionEncounterFlow(session, runState, new CardFusionService(cardLibrary));
    display = new ChanceEncounterDisplay(session, flow);
    entity = new Entity().addComponent(display);
    entities.register(entity);

    display.getChoiceButtons().get(0).fire(new ChangeEvent());
    assertTrue(session.isAwaitingDelegatedCompletion());
    assertNotNull(display.getCardFusionView());
  }

  @AfterEach
  void tearDown() {
    entity.dispose();
    stage.dispose();
  }

  @Test
  void subtitleStaysOnOneLine() {
    Label subtitle = display.getCardFusionView().getInstructionLabel();
    GlyphLayout measured = new GlyphLayout(subtitle.getStyle().font, subtitle.getText());

    assertFalse(subtitle.getWrap());
    assertTrue(measured.width * subtitle.getFontScaleX() < subtitle.getWidth());
  }

  @Test
  void bottomControlsKeepBalancedSpacingAndAlignedSlots() {
    CardFusionSelectionView view = display.getCardFusionView();
    Actor panel = view.getStatusPanel();
    TextButton fuse = view.getFuseButton();
    TextButton leave = view.getLeaveButton();
    Label selected = view.getSelectionLabel();
    Label feedback = view.getFeedbackLabel();

    assertTrue(panel.getWidth() <= 680f * 0.94f);
    assertTrue(panel.getWidth() >= 680f * 0.90f);
    assertTrue(fuse.getX() - (panel.getX() + panel.getWidth()) > 40f);
    assertTrue(fuse.getX() - (panel.getX() + panel.getWidth()) < 70f);
    assertTrue(Math.abs(panel.getX() - (1280f - fuse.getX() - fuse.getWidth())) <= 4f);
    assertEquals(fuse.getX(), leave.getX());
    assertEquals(fuse.getWidth(), leave.getWidth());
    assertEquals(selected.getX(), feedback.getX());
    assertTrue(
        Math.abs(
                selected.getY()
                    + selected.getHeight() / 2f
                    - view.getSelectedPreview().getY()
                    - view.getSelectedPreview().getHeight() / 2f)
            <= 2f);

    Table previews = view.getSelectedPreview();
    previews.validate();
    assertEquals(3, previews.getChildren().size);
    for (int i = 0; i < previews.getChildren().size; i++) {
      Actor slot = previews.getChildren().get(i);
      assertEquals(56f, slot.getWidth());
      assertEquals(47f, slot.getHeight());
      if (i > 0) {
        Actor previous = previews.getChildren().get(i - 1);
        assertEquals(10f, slot.getX() - previous.getX() - previous.getWidth());
        assertEquals(previous.getY(), slot.getY());
      }
    }
    GlyphLayout feedbackMeasure = new GlyphLayout(feedback.getStyle().font, feedback.getText());
    assertTrue(feedbackMeasure.width * feedback.getFontScaleX() <= feedback.getWidth());
  }

  @Test
  void cardGridAndSelectedSlotsKeepStableGeometry() {
    CardFusionSelectionView view = display.getCardFusionView();
    Table grid = view.getCardsGrid();
    grid.pack();
    grid.validate();
    assertEquals(8, grid.getChildren().size);
    assertTrue(grid.getPrefWidth() <= 880f);
    assertTrue(grid.getPrefHeight() <= 374f);
    for (int i = 0; i < grid.getChildren().size; i++) {
      Actor a = grid.getChildren().get(i);
      for (int j = i + 1; j < grid.getChildren().size; j++) {
        Actor b = grid.getChildren().get(j);
        assertTrue(
            a.getX() + a.getWidth() <= b.getX()
                || b.getX() + b.getWidth() <= a.getX()
                || a.getY() + a.getHeight() <= b.getY()
                || b.getY() + b.getHeight() <= a.getY());
      }
    }

    Actor firstSlot = grid.getChildren().first();
    float originalX = firstSlot.getX();
    float originalY = firstSlot.getY();
    float originalWidth = firstSlot.getWidth();
    float originalHeight = firstSlot.getHeight();
    String firstId = view.getCardButtons().keySet().iterator().next();
    select(view.getCardButtons().get(firstId));
    grid.validate();
    assertEquals(originalX, firstSlot.getX());
    assertEquals(originalY, firstSlot.getY());
    assertEquals(originalWidth, firstSlot.getWidth());
    assertEquals(originalHeight, firstSlot.getHeight());

    Table previews = view.getSelectedPreview();
    previews.pack();
    previews.validate();
    assertEquals(3, previews.getChildren().size);
    for (Actor preview : previews.getChildren()) {
      assertEquals(56f, preview.getWidth());
      assertEquals(47f, preview.getHeight());
    }
  }

  @Test
  void sameNamedCopiesCanBeSelectedAndFusedOnce() {
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardLibrary);
    int originalSize = deck.size();
    List<String> strikes =
        deck.getCards().stream()
            .filter(card -> card.cardId().equals("strike"))
            .limit(3)
            .map(CardInstance::instanceId)
            .toList();
    assertEquals(3, strikes.size());

    CardFusionSelectionView view = display.getCardFusionView();
    assertTrue(view.getFuseButton().isDisabled());
    strikes.forEach(id -> select(view.getCardButtons().get(id)));
    assertEquals(strikes, view.getSelectedInstanceIds());
    assertTrue(view.isCardGlowVisible(strikes.get(0)));
    assertFalse(view.getFuseButton().isDisabled());
    String fourthId =
        deck.getCards().stream()
            .map(CardInstance::instanceId)
            .filter(id -> !strikes.contains(id))
            .findFirst()
            .orElseThrow();
    select(view.getCardButtons().get(fourthId));
    assertEquals(strikes, view.getSelectedInstanceIds());
    assertFalse(view.getCardButtons().get(fourthId).isChecked());
    assertFalse(view.isCardGlowVisible(fourthId));

    view.getFuseButton().fire(new ChangeEvent());

    assertTrue(runState.hasUsedCardFusion());
    assertEquals(originalSize - 2, deck.size());
    assertEquals(1, completions.get());
    assertTrue(view.getFeedbackText().contains("Fusion complete"));
    assertTrue(view.getFuseButton().isDisabled());
    assertTrue(view.getLeaveButton().isDisabled());
  }

  @Test
  void fusionPresentationFlashesDirectlyToACompleteRewardCard() {
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardLibrary);
    CardFusionSelectionView view = display.getCardFusionView();
    deck.getCards().stream()
        .filter(card -> card.cardId().equals("strike"))
        .limit(3)
        .map(CardInstance::instanceId)
        .forEach(id -> select(view.getCardButtons().get(id)));
    view.getFuseButton().fire(new ChangeEvent());

    Group sequence = view.getSequenceLayer();
    List<Group> floatingCards = new ArrayList<>();
    int spreadingRays = 0;
    Image spark = null;
    int oversizedHalos = 0;
    for (Actor actor : sequence.getChildren()) {
      if (actor instanceof Group group) {
        if (group.getWidth() == 150f) {
          floatingCards.add(group);
        }
        if ("fusion-light-ray".equals(group.getName())) {
          spreadingRays++;
          assertEquals(640f, group.getX());
          assertEquals(296f, group.getY() + group.getOriginY());
          assertEquals(20, group.getChildren().size);
        }
      }
      if (actor instanceof Image image && image.getWidth() == 10f) {
        spark = image;
      }
      if (actor instanceof Image image && image.getWidth() == 100f) {
        oversizedHalos++;
      }
    }
    assertEquals(3, floatingCards.size());
    assertEquals(11, spreadingRays);
    assertNotNull(spark);
    assertEquals(640f, spark.getX() + spark.getOriginX());
    assertEquals(296f, spark.getY() + spark.getOriginY());
    assertEquals(45f, spark.getRotation());
    assertEquals(0, oversizedHalos);
    assertEquals(12f, floatingCards.get(0).getRotation());
    assertEquals(0f, floatingCards.get(1).getRotation());
    assertEquals(-12f, floatingCards.get(2).getRotation());

    advanceFusionAnimation();
    assertNotNull(view.getContinueButton());
    assertTrue(sequence.getChildren().contains(view.getContinueButton(), true));
    Group rewardCard = null;
    for (Actor actor : sequence.getChildren()) {
      if (actor instanceof Group group && group.getWidth() == 356f) {
        rewardCard = group;
      }
    }
    assertNotNull(rewardCard);
    assertEquals(462f, rewardCard.getX());
    assertEquals(196f, rewardCard.getY());
    assertEquals(520f, rewardCard.getHeight());
    CardConfig reward =
        cardLibrary.getCard(deck.getCards().get(deck.size() - 1).cardId()).orElseThrow();
    boolean hasEnergy = false;
    boolean hasCost = false;
    boolean hasName = false;
    boolean hasRulesText = false;
    boolean hasRarity = false;
    boolean hasType = false;
    Label cardRarity = null;
    int largeArtworkCount = 0;
    for (Actor actor : rewardCard.getChildren()) {
      if (actor instanceof Label label) {
        hasEnergy |= label.getText().toString().equals("ENERGY");
        hasCost |= label.getText().toString().equals(Integer.toString(reward.cost));
        hasName |= label.getText().toString().equals(reward.name);
        hasRulesText |= label.getText().toString().equals(reward.description);
        hasRarity |= label.getText().toString().equals(reward.rarity.toString());
        if (label.getText().toString().equals(reward.rarity.toString())) {
          cardRarity = label;
        }
        hasType |= label.getText().toString().equals(reward.type.toString());
      } else if (actor instanceof Image image && image.getWidth() == 316f) {
        if (image.getHeight() == 260f) {
          largeArtworkCount++;
        }
      }
    }
    assertTrue(hasEnergy);
    assertTrue(hasCost);
    assertTrue(hasName);
    assertTrue(hasRulesText);
    assertTrue(hasRarity);
    assertTrue(hasType);
    assertNotNull(cardRarity);
    assertEquals(1, largeArtworkCount);
    assertEquals(34f, view.getContinueButton().getY());

    Label prefix = null;
    Label highlightedRare = null;
    Label suffix = null;
    for (Actor actor : sequence.getChildren()) {
      if (actor instanceof Label label) {
        switch (label.getText().toString()) {
          case "A new " -> prefix = label;
          case "Rare" -> highlightedRare = label;
          case " card joins your deck." -> suffix = label;
          default -> {}
        }
      }
    }
    assertNotNull(prefix);
    assertNotNull(highlightedRare);
    assertNotNull(suffix);
    assertEquals(prefix.getX() + prefix.getWidth(), highlightedRare.getX());
    assertEquals(highlightedRare.getX() + highlightedRare.getWidth(), suffix.getX());
    assertEquals(640f, (prefix.getX() + suffix.getX() + suffix.getWidth()) / 2f);
    assertEquals(
        (rewardCard.getY() + view.getContinueButton().getY() + view.getContinueButton().getHeight())
            / 2f,
        highlightedRare.getY() + highlightedRare.getHeight() / 2f);
    assertEquals(cardRarity.getStyle().fontColor, highlightedRare.getStyle().fontColor);
  }

  @Test
  void staleSelectionCanBeRetriedWithoutEndingTheEvent() {
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardLibrary);
    CardFusionSelectionView view = display.getCardFusionView();
    List<String> selected =
        deck.getCards().stream().limit(3).map(CardInstance::instanceId).toList();
    selected.forEach(id -> select(view.getCardButtons().get(id)));
    deck.removeCardInstance(selected.get(0));

    view.getFuseButton().fire(new ChangeEvent());

    assertEquals(0, completions.get());
    assertFalse(runState.hasUsedCardFusion());
    assertTrue(view.getFeedbackText().contains("no longer in your deck"));
    assertTrue(view.getSelectedInstanceIds().isEmpty());

    List<String> retry = deck.getCards().stream().limit(3).map(CardInstance::instanceId).toList();
    retry.forEach(id -> select(view.getCardButtons().get(id)));
    view.getFuseButton().fire(new ChangeEvent());
    assertEquals(1, completions.get());
    assertTrue(runState.hasUsedCardFusion());
  }

  @Test
  void unavailableFusionAllowsOnlyDelegatedLeave() {
    runState.markCardFusionUsed();
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardLibrary);
    int originalSize = deck.size();
    CardFusionSelectionView view = display.getCardFusionView();
    deck.getCards().stream()
        .limit(3)
        .map(CardInstance::instanceId)
        .forEach(id -> select(view.getCardButtons().get(id)));

    view.getFuseButton().fire(new ChangeEvent());

    assertTrue(view.getFeedbackText().contains("already used"));
    assertTrue(view.getFuseButton().isDisabled());
    assertFalse(view.getLeaveButton().isDisabled());
    assertEquals(0, completions.get());
    view.getLeaveButton().fire(new ChangeEvent());
    assertEquals(1, completions.get());
    assertEquals(originalSize, deck.size());
  }

  @Test
  void successSceneWaitsForContinueWithoutChangingTheFusionResult() {
    CardFusionSelectionView view = display.getCardFusionView();
    runState.getOrCreatePlayerDeck(cardLibrary).getCards().stream()
        .limit(3)
        .map(CardInstance::instanceId)
        .forEach(id -> select(view.getCardButtons().get(id)));

    view.getFuseButton().fire(new ChangeEvent());

    assertTrue(runState.hasUsedCardFusion());
    assertEquals(1, completions.get());
    assertFalse(view.isExitRequested());
    advanceFusionAnimation();
    assertNotNull(view.getContinueButton());
    view.getContinueButton().fire(new ChangeEvent());
    assertTrue(view.isExitRequested());
  }

  @Test
  void delegatedLeaveSceneWaitsForContinueWithoutUsingFusion() {
    CardFusionSelectionView view = display.getCardFusionView();
    view.getLeaveButton().fire(new ChangeEvent());

    assertEquals(1, completions.get());
    assertFalse(runState.hasUsedCardFusion());
    assertFalse(view.isExitRequested());
    view.getContinueButton().fire(new ChangeEvent());
    assertTrue(view.isExitRequested());
  }

  private static void select(TextButton button) {
    button.getClickListener().clicked(null, 10f, 10f);
  }

  private void advanceFusionAnimation() {
    for (int frame = 0; frame < 70; frame++) {
      stage.act(0.05f);
    }
  }
}
