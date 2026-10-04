package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopDisplay.ShopItemState;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopDisplayTest {
  private final ShopItem item = new ShopItem("offer", "card", "Test Card", 20, 1);
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
  void shouldShowAvailableWhenPurchaseCheckSucceeds() {
    assertEquals(
        ShopItemState.AVAILABLE, ShopDisplay.getItemState(PurchaseResult.available(item), false));
  }

  @Test
  void shouldShowUnaffordableWhenGoldIsInsufficient() {
    assertEquals(
        ShopItemState.UNAFFORDABLE,
        ShopDisplay.getItemState(
            PurchaseResult.failure(PurchaseResult.Status.INSUFFICIENT_GOLD, item), false));
  }

  @Test
  void shouldShowSoldAfterPurchase() {
    assertEquals(
        ShopItemState.SOLD, ShopDisplay.getItemState(PurchaseResult.available(item), true));
  }

  @Test
  void shouldShowSoldWhenStockIsEmpty() {
    assertEquals(
        ShopItemState.SOLD,
        ShopDisplay.getItemState(
            PurchaseResult.failure(PurchaseResult.Status.OUT_OF_STOCK, item), false));
  }

  @Test
  void shouldShowUnavailableForOtherFailures() {
    assertEquals(
        ShopItemState.UNAVAILABLE,
        ShopDisplay.getItemState(
            PurchaseResult.failure(PurchaseResult.Status.INVALID_ITEM, item), false));
  }

  @Test
  void shouldRenderSharedCardWidgetForOfferedCards() {
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    CardConfig card = offerCard();
    CardService cardService = mock(CardService.class);
    when(cardService.getCard("card")).thenReturn(Optional.of(card));

    ShopDisplay display = new ShopDisplay(openShop(), cardService);
    entity = new Entity().addComponent(display);
    entity.create();

    CardWidget widget = findCardWidget(stage.getRoot());
    assertNotNull(widget);
    assertEquals("Test Card", widget.getCard().name());
    assertEquals(1, widget.getCard().cost());
    assertEquals("Deal 6 damage.", widget.getCard().description());
    assertEquals(Rarity.UNCOMMON, widget.getCard().rarity());
    assertEquals(card.texturePath, widget.getCard().texturePath());
  }

  @Test
  void shouldKeepPlaceholderWhenOfferedCardCannotBeResolved() {
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    CardService cardService = mock(CardService.class);
    when(cardService.getCard("card")).thenReturn(Optional.empty());

    ShopDisplay display = new ShopDisplay(openShop(), cardService);
    entity = new Entity().addComponent(display);
    entity.create();

    assertNull(findCardWidget(stage.getRoot()));
  }

  @Test
  void shouldRecordEveryDisplayedShopCardSeen() {
    CardDiscoveryService discovery = new CardDiscoveryService(CardConfigLoader.loadCards());
    ServiceLocator.registerCardDiscoveryService(discovery);
    InventoryComponent inventory = new InventoryComponent(100);
    ShopService shopService =
        new ShopService(
            new ShopItem[] {
              new ShopItem("strike-offer", "strike", "Strike", 20, 1),
              new ShopItem("bandage-offer", "bandage", "Bandage", 20, 1)
            });
    ShopDisplay display = new ShopDisplay(inventory, shopService);
    entity = new Entity().addComponent(display);

    entity.create();

    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("strike"));
    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("bandage"));
  }

  private ShopEncounter openShop() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopService shopService = new ShopService(new ShopItem[] {item});
    return new ShopEncounter(inventory, shopService);
  }

  private static CardConfig offerCard() {
    CardConfig card = new CardConfig();
    card.id = "card";
    card.name = "Test Card";
    card.description = "Deal 6 damage.";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.rarity = Rarity.UNCOMMON;
    card.target = TargetType.SINGLE_ENEMY;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    card.texturePath = "images/cards/card.png";
    return card;
  }

  private static CardWidget findCardWidget(Actor actor) {
    if (actor instanceof CardWidget widget) {
      return widget;
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        CardWidget found = findCardWidget(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  @Test
  void shouldResolveArtworkFromTheSharedCardService() {
    CardService cardService = mock(CardService.class);
    CardConfig card = new CardConfig();
    card.name = "Battle Card";
    card.description = "Deal 8 damage.";
    card.cost = 2;
    card.texturePath = "images/cards/card.png";
    when(cardService.getCard("card")).thenReturn(Optional.of(card));

    ShopDisplay display = new ShopDisplay(openShop(), cardService);

    assertEquals("images/cards/card.png", display.resolveArtworkPath(item));
    assertEquals("Battle Card", display.resolveCardName(item));
    assertEquals("Deal 8 damage.", display.resolveCardDescription(item));
    assertEquals("2 ENERGY", display.resolveEnergyText(item));
    assertNull(display.resolveArtworkPath(null));
  }

  @Test
  void shouldFallBackToShopTextWhenCardDefinitionIsMissing() {
    CardService cardService = mock(CardService.class);
    when(cardService.getCard("card")).thenReturn(Optional.empty());
    ShopItem describedItem =
        new ShopItem("offer", "card", "Fallback Card", "Fallback description.", 20, 1);

    ShopDisplay display = new ShopDisplay(openShop(), cardService);

    assertEquals("Fallback Card", display.resolveCardName(describedItem));
    assertEquals("Fallback description.", display.resolveCardDescription(describedItem));
    assertEquals("-- ENERGY", display.resolveEnergyText(describedItem));
  }
}
