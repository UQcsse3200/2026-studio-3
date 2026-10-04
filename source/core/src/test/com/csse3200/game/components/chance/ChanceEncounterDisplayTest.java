package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.chance.DiceEncounterBehaviour;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceOutcomeApplier;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceEncounterDisplayTest {
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
    if (entity != null) {
      entity.dispose();
    }
    stage.dispose();
  }

  @Test
  void abandonedMineSceneKeepsChoicesAndCompletionWorking() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);

    ChanceEncounter encounter =
        new ChanceEncounter(
            "abandoned-mine",
            "The mouth of an abandoned mine promises danger and forgotten riches.",
            List.of(
                new ChanceChoice(
                    "search-tunnels",
                    "Search the unstable tunnels for valuables.",
                    new ChanceOutcome(-12, 30)),
                new ChanceChoice("leave", "Leave the mine undisturbed.", new ChanceOutcome(0, 0))));
    AtomicInteger completions = new AtomicInteger();
    ChanceEncounterDisplay display =
        new ChanceEncounterDisplay(
            encounter, (nodeId, success) -> completions.incrementAndGet(), 7);
    Entity entity = new Entity().addComponent(display);
    entities.register(entity);

    try {
      assertEquals(2, display.getChoiceButtons().size());
      assertEquals(
          "1.  Search the unstable tunnels for valuables.",
          display.getChoiceButtons().get(0).getText().toString());
      assertEquals(
          "2.  Leave the mine undisturbed.",
          display.getChoiceButtons().get(1).getText().toString());
      ((Table) display.getChoiceButtons().get(0).getParent()).layout();
      assertTrue(
          display.getChoiceButtons().get(0).getX() < display.getChoiceButtons().get(1).getX());

      display.getChoiceButtons().get(0).fire(new ChangeEvent());
      assertEquals("OUTCOME\nYou lose 12 health.\nYou gain 30 gold.", display.getResultText());
      assertTrue(display.getContinueButton().isVisible());
      assertTrue(display.getChoiceButtons().get(0).isDisabled());
      assertEquals(0, completions.get());

      display.getContinueButton().fire(new ChangeEvent());
      stage.act(0.3f);
      stage.act(0.3f);
      assertEquals(1, completions.get());
    } finally {
      entity.dispose();
      stage.dispose();
    }
  }

  @Test
  void shouldRefreshDiceButtonsAfterEachStagedChoice() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);

    ChanceEncounter encounter =
        new ChanceEncounter(
            "dice-game",
            "A dice keeper offers a wager.",
            List.of(
                diceChoice("low"),
                diceChoice("high"),
                diceChoice("take"),
                diceChoice("double-down"),
                diceChoice("cash-out"),
                diceChoice("continue")));
    Random luckySeven =
        new Random() {
          private int next;
          private final int[] rolls = {0, 5};

          @Override
          public int nextInt(int bound) {
            return rolls[next++];
          }
        };
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            -1,
            encounter,
            new DiceEncounterBehaviour(luckySeven, TestCardService.withCards("bandage")),
            new ChanceOutcomeApplier(new MockPlayerStateGateway(100, 50)),
            (nodeId, success) -> {});
    ChanceEncounterDisplay display = new ChanceEncounterDisplay(session);
    Entity entity = new Entity().addComponent(display);
    entities.register(entity);

    try {
      assertEquals(List.of("low", "high"), choiceIds(display));
      assertEquals("TOTAL  ?", display.getDiceRollDisplay().getTotalText());
      assertEquals("?", display.getDiceRollDisplay().getDisplayedTotalValue());
      ((Table) display.getChoiceButtons().get(0).getParent()).layout();
      assertTrue(
          display.getChoiceButtons().get(0).getX() < display.getChoiceButtons().get(1).getX());
      display.getChoiceButtons().get(0).fire(new ChangeEvent());
      assertTrue(display.getDiceRollDisplay().isRolling());
      assertEquals("ROLLING...", display.getResultText());
      display.getChoiceButtons().get(1).fire(new ChangeEvent());
      for (int frame = 0; frame < 42; frame++) {
        stage.act(1f / 60f);
      }
      assertTrue(display.getDiceRollDisplay().isRolling());
      assertEquals(1, display.getDiceRollDisplay().getFirstValue());
      assertEquals(6, display.getDiceRollDisplay().getSecondValue());
      assertEquals("ROLLING...", display.getDiceRollDisplay().getTotalText());
      for (int frame = 0; frame < 18; frame++) {
        stage.act(1f / 60f);
      }
      assertFalse(display.getDiceRollDisplay().isRolling());
      assertEquals(1, display.getDiceRollDisplay().getFirstValue());
      assertEquals(6, display.getDiceRollDisplay().getSecondValue());
      assertEquals("TOTAL  7", display.getDiceRollDisplay().getTotalText());
      assertEquals("7", display.getDiceRollDisplay().getDisplayedTotalValue());
      assertEquals(List.of("take", "double-down"), choiceIds(display));
      assertTrue(display.getResultText().contains("LUCKY SEVEN!"));
      assertTrue(display.getResultText().contains("dice total is 7"));
      assertFalse(session.isResolved());

      display.getChoiceButtons().get(0).fire(new ChangeEvent());
      assertFalse(display.getDiceRollDisplay().isRolling());
      assertEquals("TOTAL  7", display.getDiceRollDisplay().getTotalText());
      assertEquals(List.of("cash-out", "continue"), choiceIds(display));
      assertTrue(display.getResultText().contains("10 gold"));
      assertFalse(session.isResolved());

      display.getChoiceButtons().get(0).fire(new ChangeEvent());
      assertTrue(session.isResolved());
    } finally {
      entity.dispose();
      stage.dispose();
    }
  }

  private static ChanceChoice diceChoice(String id) {
    return new ChanceChoice(id, id, new ChanceOutcome(0, 0));
  }

  private static List<String> choiceIds(ChanceEncounterDisplay display) {
    return display.getChoiceButtons().stream()
        .map(TextButton::getText)
        .map(text -> text.toString().substring(4))
        .toList();
  }

  @Test
  void shouldFormatEncounterIdAsTitle() {
    assertEquals("Mysterious Shrine", ChanceEncounterDisplay.formatTitle("mysterious-shrine"));
    assertEquals("Healing Spring", ChanceEncounterDisplay.formatTitle("healing_spring"));
  }

  @Test
  void shouldDescribeHealthAndGoldOutcome() {
    assertEquals(
        "You lose 10 health.\nYou gain 25 gold.",
        ChanceEncounterDisplay.formatOutcome(new ChanceOutcome(-10, 25)));
  }

  @Test
  void shouldDescribeNoEffectOutcome() {
    assertEquals(
        "Nothing happens. You continue on your way.",
        ChanceEncounterDisplay.formatOutcome(new ChanceOutcome(0, 0)));
  }

  @Test
  void shouldDescribeCardRewardOutcome() {
    assertEquals(
        "You receive the Bandage card.",
        ChanceEncounterDisplay.formatOutcome(new ChanceOutcome(0, 0, "bandage")));
  }

  @Test
  void shouldDescribeUnresolvedChoice() {
    assertEquals("This choice could not be resolved.", ChanceEncounterDisplay.formatOutcome(null));
  }

  @Test
  void shouldDescribeEveryCardInMultiCardRewardOutcome() {
    assertEquals(
        "You receive the Bandage card.\nYou receive the Strike card.",
        ChanceEncounterDisplay.formatOutcome(
            ChanceOutcome.withCardRewards(0, 0, List.of("bandage", "strike"))));
  }

  @Test
  void shouldDiscoverEveryDisplayedCardInMultiCardPreview() {
    CardDiscoveryService discovery = new CardDiscoveryService(CardConfigLoader.loadCards());
    ServiceLocator.registerCardDiscoveryService(discovery);
    ChanceEncounter encounter =
        new ChanceEncounter(
            "multi-card-preview",
            "Two cards await.",
            List.of(
                new ChanceChoice(
                    "take-cards",
                    "Take the cards",
                    ChanceOutcome.withCardRewards(0, 0, List.of("bandage", "strike")))));
    ChanceEncounterDisplay display = new ChanceEncounterDisplay(encounter);
    entity = new Entity().addComponent(display);
    entity.create();
    assertEquals(CardUnlockState.LOCKED, discovery.getProgressSnapshot().get("bandage"));
    assertEquals(CardUnlockState.LOCKED, discovery.getProgressSnapshot().get("strike"));

    display.getChoiceButtons().getFirst().fire(new ChangeEvent());

    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("bandage"));
    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("strike"));
  }

  @Test
  void shouldAllowPreviewWithoutMapNode() {
    ChanceEncounter preview =
        new ChanceEncounter(
            "preview",
            "Preview encounter",
            List.of(new ChanceChoice("continue", "Continue", new ChanceOutcome(0, 0))));

    assertDoesNotThrow(() -> new ChanceEncounterDisplay(preview));
  }

  @Test
  void shouldRecordEventCardSeenOnlyWhenSuccessfulOutcomeIsDisplayed() {
    CardDiscoveryService discovery = new CardDiscoveryService(CardConfigLoader.loadCards());
    ServiceLocator.registerCardDiscoveryService(discovery);
    ChanceEncounter encounter =
        new ChanceEncounter(
            "event-card",
            "A card waits in the archive.",
            List.of(
                new ChanceChoice(
                    "take-card", "Take the card", new ChanceOutcome(0, 0, "bandage"))));
    ChanceEncounterDisplay display = new ChanceEncounterDisplay(encounter, null, 1);
    entity = new Entity().addComponent(display);
    entity.create();
    assertEquals(CardUnlockState.LOCKED, discovery.getProgressSnapshot().get("bandage"));

    display.getChoiceButtons().getFirst().fire(new ChangeEvent());

    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("bandage"));
  }
}
