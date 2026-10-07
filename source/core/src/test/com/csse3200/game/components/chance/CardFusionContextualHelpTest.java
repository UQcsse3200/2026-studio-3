package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class CardFusionContextualHelpTest {
  private Stage stage;
  private Entity entity;
  private CardLibrary cardLibrary;
  private RunState runState;
  private ChanceEncounterSession session;
  private ChanceEncounterDisplay display;
  private AtomicInteger completions;

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
  }

  @AfterEach
  void tearDown() {
    entity.dispose();
    stage.dispose();
  }

  @Test
  void introHelpClosesWithoutDelegatingOrChangingTheRun() {
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardLibrary);
    List<CardInstance> originalCards = deck.getCards();
    TextButton helpButton = display.getHelpButton();
    assertNotNull(helpButton);
    assertEquals(stage, helpButton.getStage());

    helpButton.fire(new ChangeEvent());
    ContextualHelpDialog help = display.getHelpDialog();
    assertEquals("Card Fusion Rules", help.getDialog().getTitleLabel().getText().toString());
    assertTrue(help.getBodyLabel().getText().toString().contains("three different Common"));
    assertEquals(stage, help.getDialog().getStage());
    closeHelp(help);

    assertEquals(2, display.getChoiceButtons().size());
    assertFalse(session.isAwaitingDelegatedCompletion());
    assertFalse(session.isResolved());
    assertFalse(runState.hasUsedCardFusion());
    assertEquals(originalCards, deck.getCards());
    assertEquals(0, completions.get());
    assertEquals(stage, helpButton.getStage());
  }

  @Test
  void selectionHelpPreservesCardsAndDelegatedSession() {
    display.getChoiceButtons().get(0).fire(new ChangeEvent());
    assertTrue(session.isAwaitingDelegatedCompletion());
    CardFusionSelectionView view = display.getCardFusionView();
    assertNotNull(view);
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardLibrary);
    List<CardInstance> originalCards = deck.getCards();
    List<String> selectedIds =
        originalCards.stream().limit(2).map(CardInstance::instanceId).toList();
    selectedIds.forEach(
        id -> view.getCardButtons().get(id).getClickListener().clicked(null, 10f, 10f));
    assertEquals(selectedIds, view.getSelectedInstanceIds());

    TextButton helpButton = view.getHelpButton();
    assertEquals(stage, helpButton.getStage());
    helpButton.fire(new ChangeEvent());
    ContextualHelpDialog help = view.getHelpDialog();
    assertEquals("Card Fusion Rules", help.getDialog().getTitleLabel().getText().toString());
    assertTrue(help.getBodyLabel().getText().toString().contains("once per run"));
    assertEquals(stage, help.getDialog().getStage());
    closeHelp(help);

    assertEquals(selectedIds, view.getSelectedInstanceIds());
    selectedIds.forEach(id -> assertTrue(view.getCardButtons().get(id).isChecked()));
    assertEquals(originalCards, deck.getCards());
    assertFalse(runState.hasUsedCardFusion());
    assertTrue(session.isAwaitingDelegatedCompletion());
    assertFalse(session.isResolved());
    assertEquals(0, completions.get());
    assertEquals(stage, helpButton.getStage());
  }

  private void closeHelp(ContextualHelpDialog help) {
    help.getCloseButton().fire(new ChangeEvent());
    for (int tick = 0; tick < 3; tick++) {
      stage.act(1f);
    }
    assertNull(help.getDialog().getStage());
  }
}
