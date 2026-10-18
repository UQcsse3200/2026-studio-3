package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.chance.ChanceEncounterBehaviourFactory;
import com.csse3200.game.chance.ChanceEncounterConfigLoader;
import com.csse3200.game.chance.TestMineRandom;
import com.csse3200.game.encounters.integration.CardCatalogGateway;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceOutcomeApplier;
import com.csse3200.game.encounters.integration.DeckGateway;
import com.csse3200.game.encounters.integration.ItemRewardGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks confirmed Curse rewards pass through the scenic UI exactly once. */
@ExtendWith(GameExtension.class)
class ChanceCurseChoiceDispatchTest {
  @Test
  void shouldDispatchBrandChoice() {
    shouldDispatchNewScenicChoiceWithoutChangingUi(
        "mysterious-shrine", "accept-brand", "BRAND_OF_THE_DAMNED", 50);
  }

  @Test
  void shouldDispatchBloodOathChoice() {
    shouldDispatchNewScenicChoiceWithoutChangingUi(
        "mysterious-shrine", "swear-blood-oath", "BLOOD_OATH_SIGIL", 70);
  }

  @Test
  void shouldDispatchShatteredAegisChoice() {
    shouldDispatchNewScenicChoiceWithoutChangingUi(
        "abandoned-mine", "search-tunnels", "SHATTERED_AEGIS", 60);
  }

  @Test
  void shouldDispatchAshenEmberChoice() {
    shouldDispatchNewScenicChoiceWithoutChangingUi(
        "abandoned-mine", "search-tunnels", "ASHEN_EMBER", 60);
  }

  @Test
  void mineWithoutCurseDisplaysOnlyTheCommittedGold() {
    Stage stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    var encounter =
        ChanceEncounterConfigLoader.loadEncounters().stream()
            .filter(event -> event.getId().equals("abandoned-mine"))
            .findFirst()
            .orElseThrow();
    ItemRewardGateway items = mock(ItemRewardGateway.class);
    MockPlayerStateGateway player = new MockPlayerStateGateway(40, 100, 35);
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            ChanceEncounterBehaviourFactory.create(
                encounter, new TestMineRandom(0.9, 0), TestCardService.withCards("bandage")),
            new ChanceOutcomeApplier(
                player, mock(CardCatalogGateway.class), mock(DeckGateway.class), items),
            null);
    ChanceEncounterDisplay display = new ChanceEncounterDisplay(session);
    Entity entity = new Entity().addComponent(display);
    try {
      entity.create();
      display.getChoiceButtons().get(0).fire(new ChangeEvent());
      assertEquals(95, player.getCurrency());
      var overlay = stage.getRoot().findActor("acquisition-result-overlay");
      assertNotNull(overlay);
      assertEquals(
          "Gold +60",
          ((com.badlogic.gdx.scenes.scene2d.ui.Label) overlay.findActor("acquisition-entry-gold"))
              .getText()
              .toString());
      assertNull(overlay.findActor("acquisition-entry-BRAND_OF_THE_DAMNED"));
      assertEquals(1, session.getAcquisitionResult(null).orElseThrow().entries().size());
      overlay.findActor("acquisition-continue").fire(new ChangeEvent());
      assertTrue(session.isCompleted());
      assertEquals(95, player.getCurrency());
      verifyNoInteractions(items);
    } finally {
      entity.dispose();
      stage.dispose();
    }
  }

  private void shouldDispatchNewScenicChoiceWithoutChangingUi(
      String eventId, String choiceId, String itemId, int gold) {
    Stage stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    var encounter =
        ChanceEncounterConfigLoader.loadEncounters().stream()
            .filter(event -> event.getId().equals(eventId))
            .findFirst()
            .orElseThrow();
    ItemRewardGateway items = mock(ItemRewardGateway.class);
    when(items.containsItem(itemId)).thenReturn(true);
    when(items.addItem(itemId)).thenReturn(true);
    MockPlayerStateGateway player = new MockPlayerStateGateway(40, 100, 35);
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            encounter,
            ChanceEncounterBehaviourFactory.create(
                encounter,
                new TestMineRandom(
                    0,
                    List.of(
                            "BRAND_OF_THE_DAMNED",
                            "BLOOD_OATH_SIGIL",
                            "SHATTERED_AEGIS",
                            "ASHEN_EMBER")
                        .indexOf(itemId)),
                TestCardService.withCards("bandage")),
            new ChanceOutcomeApplier(
                player, mock(CardCatalogGateway.class), mock(DeckGateway.class), items),
            null);
    ChanceEncounterDisplay display = new ChanceEncounterDisplay(session);
    Entity entity = new Entity().addComponent(display);
    try {
      entity.create();
      assertEquals(encounter.getChoices().size(), display.getChoiceButtons().size());
      int choiceIndex =
          encounter.getChoices().stream().map(choice -> choice.getId()).toList().indexOf(choiceId);
      display.getChoiceButtons().get(choiceIndex).fire(new ChangeEvent());
      assertTrue(session.isResolved());
      assertEquals(35 + gold, player.getCurrency());
      assertEquals(40, player.getHealth());
      var overlay = stage.getRoot().findActor("acquisition-result-overlay");
      assertNotNull(overlay);
      assertFalse(display.getContinueButton().isVisible());
      assertEquals(
          "Gold +" + gold,
          ((com.badlogic.gdx.scenes.scene2d.ui.Label)
                  overlay.findActor("acquisition-entry-gold"))
              .getText()
              .toString());
      assertNotNull(overlay.findActor("acquisition-entry-" + itemId));
      display.getChoiceButtons().get(choiceIndex).fire(new ChangeEvent());
      verify(items, times(1)).addItem(itemId);
      overlay.findActor("acquisition-close").fire(new ChangeEvent());
      assertNull(stage.getRoot().findActor("acquisition-result-overlay"));
      assertTrue(display.getContinueButton().isVisible());
    } finally {
      entity.dispose();
      stage.dispose();
    }
  }
}
