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
    display.openMerchant();

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
    display.openMerchant();

    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("strike"));
    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("bandage"));
  }

  @Test
  void successfulPurchaseShowsConfirmedResultAndDoesNotGrantTwiceOnContinue() {
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ShopEncounter encounter = openShop();
    CardService cards = mock(CardService.class);
    when(cards.getCard("card")).thenReturn(Optional.empty());
    ShopDisplay display = new ShopDisplay(encounter, cards);
    entity = new Entity().addComponent(display);
    entity.create();
    display.openMerchant();

    Actor purchase = stage.getRoot().findActor("purchase-offer");
    purchase.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertEquals(80, encounter.getCurrency());
    assertNotNull(stage.getRoot().findActor("acquisition-result-overlay"));

    stage
        .getRoot()
        .findActor("acquisition-continue")
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertNull(stage.getRoot().findActor("acquisition-result-overlay"));
    assertEquals(80, encounter.getCurrency());
  }

  private ShopEncounter openShop() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopService shopService = new ShopService(new ShopItem[] {item});
    return new ShopEncounter(inventory, shopService);
  }

  @Test
  void encounterStartsWithoutCardsAndApproachPreservesTheShopSession() {
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ShopEncounter encounter = openShop();
    var before = encounter.snapshot();
    ShopDisplay display = new ShopDisplay(encounter, mock(CardService.class));
    entity = new Entity().addComponent(display);
    entity.create();
    assertNull(findCardWidget(stage.getRoot()));
    Actor target = stage.getRoot().findActor("merchant-approach");
    org.junit.jupiter.api.Assertions.assertTrue(target.getWidth() < 80);
    org.junit.jupiter.api.Assertions.assertTrue(target.getHeight() < 80);
    Actor stall = stage.getRoot().findActor("merchant-stall-hover");
    for (var listener : stall.getListeners()) {
      org.junit.jupiter.api.Assertions.assertFalse(
          listener instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener);
    }
    Actor frame = stage.getRoot().findActor("merchant-title");
    display.openMerchant();
    org.junit.jupiter.api.Assertions.assertSame(frame, stage.getRoot().findActor("merchant-title"));
    assertEquals(before, encounter.snapshot());
  }

  @Test
  void refreshPreservesWoodenSlotsAndUsesBackendFees() {
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ShopEncounter encounter = openShop();
    encounter.enableRefresh(
        com.csse3200.game.shop.ShopRefreshPolicy.standard(),
        () ->
            new ShopService(
                new ShopItem[] {
                  new ShopItem("a", "a", "A", 20, 1),
                  new ShopItem("b", "b", "B", 20, 1),
                  new ShopItem("c", "c", "C", 20, 1)
                }));
    ShopDisplay display = new ShopDisplay(encounter, mock(CardService.class));
    entity = new Entity().addComponent(display);
    entity.create();
    display.openMerchant();
    Actor root = stage.getRoot().findActor("merchant-scene");
    CardDisplaySlot slot = stage.getRoot().findActor("shop-slot-0");
    Actor counter = stage.getRoot().findActor("merchant-products");
    assertEquals(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly, counter.getTouchable());
    Actor backing = slot.getChildren().first();
    Actor purchase = slot.purchaseButton();
    var refresh =
        (com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("shop-refresh");
    assertEquals("Refresh (Free)", refresh.getText().toString());
    refresh.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertEquals(100, encounter.getCurrency());
    assertEquals("Refresh (10 GOLD)", refresh.getText().toString());
    org.junit.jupiter.api.Assertions.assertSame(slot, stage.getRoot().findActor("shop-slot-0"));
    org.junit.jupiter.api.Assertions.assertSame(backing, slot.getChildren().first());
    org.junit.jupiter.api.Assertions.assertSame(purchase, slot.purchaseButton());
    org.junit.jupiter.api.Assertions.assertSame(
        counter, stage.getRoot().findActor("merchant-products"));
    CardDisplaySlot middle = stage.getRoot().findActor("shop-slot-1");
    CardDisplaySlot right = stage.getRoot().findActor("shop-slot-2");
    org.junit.jupiter.api.Assertions.assertSame(counter, slot.getParent());
    org.junit.jupiter.api.Assertions.assertSame(slot.getParent(), middle.getParent());
    org.junit.jupiter.api.Assertions.assertSame(slot.getParent(), right.getParent());
    assertEquals(3, slot.getParent().getChildren().size);
    assertNull(stage.getRoot().findActor("shop-continuous-card-channel"));
    assertEquals(3, slot.getChildren().size);
    assertNull(slot.findActor("offer-thin-base"));
    org.junit.jupiter.api.Assertions.assertNotNull(
        ((com.badlogic.gdx.scenes.scene2d.ui.Table) slot.getChildren().get(1)).getBackground());
    assertEquals(slot.getY(), middle.getY());
    assertEquals(slot.getY(), right.getY());
    assertEquals(middle.getX() - slot.getX(), right.getX() - middle.getX());
    assertEquals(slot.purchaseButton().getY(), middle.purchaseButton().getY());
    assertEquals(slot.purchaseButton().getWidth(), right.purchaseButton().getWidth());
    var pricePanel = slot.getChildren().get(1);
    var cardLayer = slot.getChildren().first();
    assertEquals(410f, slot.getHeight());
    org.junit.jupiter.api.Assertions.assertTrue(refresh.getY() > 4f);
    org.junit.jupiter.api.Assertions.assertTrue(
        pricePanel.getY() + pricePanel.getHeight() < cardLayer.getY());
    assertEquals(14f, cardLayer.getX());
    assertEquals(pricePanel.getWidth(), slot.purchaseButton().getWidth());
    assertEquals(pricePanel.getX(), slot.purchaseButton().getX());
    org.junit.jupiter.api.Assertions.assertTrue(
        slot.purchaseButton().getY() + slot.purchaseButton().getHeight() < pricePanel.getY());
    org.junit.jupiter.api.Assertions.assertTrue(
        refresh.getY() + refresh.getHeight() < counter.getY());
    org.junit.jupiter.api.Assertions.assertSame(root, stage.getRoot().findActor("merchant-scene"));
    org.junit.jupiter.api.Assertions.assertSame(refresh, stage.getRoot().findActor("shop-refresh"));
    refresh.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertEquals(90, encounter.getCurrency());
    assertEquals("Refresh (20 GOLD)", refresh.getText().toString());
    var buy =
        (com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("purchase-a");
    buy.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertEquals(70, encounter.getCurrency());
    buy.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertEquals(70, encounter.getCurrency());
    var second =
        (com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("purchase-b");
    second.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertEquals(50, encounter.getCurrency());
    org.junit.jupiter.api.Assertions.assertSame(slot, stage.getRoot().findActor("shop-slot-0"));
    org.junit.jupiter.api.Assertions.assertSame(purchase, slot.purchaseButton());
    org.junit.jupiter.api.Assertions.assertSame(root, stage.getRoot().findActor("merchant-scene"));
  }

  @Test
  void goodsRowContainsNoSharedFurnitureOrForegroundSceneCrop() {
    MerchantDisplayCounter row = new MerchantDisplayCounter();
    assertEquals(0, row.getChildren().size);
    assertEquals(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly, row.getTouchable());
    assertEquals(98f, row.getY());
  }

  @Test
  void sceneKeepsSharedCoordinatesAtDifferentWindowShapes() {
    MerchantSceneLayout layout = new MerchantSceneLayout();
    layout.setSize(1600, 1000);
    layout.validate();
    assertEquals(1.25f, layout.scene.getScaleX());
    assertEquals(0f, layout.scene.getX());
    layout.setSize(1600, 900);
    layout.validate();
    assertEquals(1.125f, layout.scene.getScaleX());
    assertEquals(80f, layout.scene.getX());
    assertEquals(0f, layout.scene.getY());
  }

  @Test
  void runeHoverDoesNotApproachUntilClicked() {
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var marker =
        new MerchantInteractionMarker(
            mock(com.badlogic.gdx.scenes.scene2d.utils.Drawable.class), calls::incrementAndGet);
    var click = (com.badlogic.gdx.scenes.scene2d.utils.ClickListener) marker.getListeners().first();
    var event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
    click.enter(event, 10, 10, -1, null);
    marker.act(0.4f);
    assertEquals(0, calls.get());
    click.exit(event, 10, 10, -1, null);
    assertEquals(0, calls.get());
    click.clicked(event, 10, 10);
    assertEquals(1, calls.get());
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
