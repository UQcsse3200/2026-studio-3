package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
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
