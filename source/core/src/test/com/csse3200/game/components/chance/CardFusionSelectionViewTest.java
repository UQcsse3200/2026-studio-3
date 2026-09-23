package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
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

    view.getFuseButton().fire(new ChangeEvent());

    assertTrue(runState.hasUsedCardFusion());
    assertEquals(originalSize - 2, deck.size());
    assertEquals(1, completions.get());
    assertTrue(view.getFeedbackText().contains("Fusion complete"));
    assertTrue(view.getFuseButton().isDisabled());
    assertTrue(view.getLeaveButton().isDisabled());
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

  private static void select(TextButton button) {
    button.getClickListener().clicked(null, 10f, 10f);
  }
}
