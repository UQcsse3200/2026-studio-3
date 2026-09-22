package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
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
class CardUpgradeDisplayTest {
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
  void refreshesOptionsAfterEachCommittedUpgrade() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    CardInstance first = new CardInstance("strike-first", "strike", CardInstance.BASE_LEVEL);
    CardInstance second = new CardInstance("strike-second", "strike", CardInstance.BASE_LEVEL);
    PlayerDeck playerDeck = PlayerDeck.fromInstances(cardService, List.of(first, second));
    CardUpgradeSelection selection = CardUpgradeSelection.forPlayerDeck(playerDeck, cardService, 1);
    CardUpgradeDisplay display =
        new CardUpgradeDisplay(selection, new PlayerDeckCardUpgradeCommitter(playerDeck));
    entity = new Entity().addComponent(display);
    entity.create();

    selection.toggle(first.instanceId());
    display.getConfirmButton().fire(new ChangeEvent());

    assertEquals(
        CardInstance.UPGRADED_LEVEL,
        playerDeck.getCard(first.instanceId()).orElseThrow().upgradeLevel());
    assertEquals(List.of(second.instanceId()), optionIds(selection));
    assertEquals(1, display.getDisplayedOptionCount());
    assertFalse(selection.canConfirm());

    selection.toggle(second.instanceId());
    display.getConfirmButton().fire(new ChangeEvent());

    assertTrue(selection.getCardUpgradeOption().isEmpty());
    assertEquals(0, display.getDisplayedOptionCount());
    assertEquals(
        CardInstance.UPGRADED_LEVEL,
        playerDeck.getCard(second.instanceId()).orElseThrow().upgradeLevel());
  }

  private static List<String> optionIds(CardUpgradeSelection selection) {
    return selection.getCardUpgradeOption().stream()
        .map(option -> option.instance().instanceId())
        .toList();
  }
}
