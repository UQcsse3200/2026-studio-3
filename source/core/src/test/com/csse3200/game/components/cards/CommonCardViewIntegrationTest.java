package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.components.battle.DeckEditorComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.clickable.DragNDrop;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class CommonCardViewIntegrationTest {
  private Stage stage;
  private ResourceService resources;
  private CardLibrary library;
  private Entity entity;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 960, true);
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    library = new CardLibrary(CardConfigLoader.loadCards());
    ServiceLocator.registerCardLibrary(library);
    resources = new ResourceService();
    resources.loadTextures(CardWidgetAssets.collectTexturePaths(library.getAllCards()));
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
  }

  @AfterEach
  void tearDown() {
    if (entity != null) {
      entity.dispose();
    }
    stage.dispose();
    resources.dispose();
  }

  @Test
  void shopOffersUseSharedFacesForAllRarities() {
    ShopService shop =
        new ShopService(
            new ShopItem[] {
              new ShopItem("common", "strike", "Strike", 20, 1),
              new ShopItem("uncommon", "inner_focus", "Inner Focus", 30, 1),
              new ShopItem("rare", "poison_mark", "Poison Mark", 40, 1)
            });
    entity =
        new Entity()
            .addComponent(
                new ShopDisplay(new ShopEncounter(new InventoryComponent(100), shop), library));
    entity.create();
    assertEquals(3, widgets().size());
    for (CardWidget widget : widgets()) {
      assertRarityFrame(widget);
    }
  }

  @Test
  void shopCommonOfferUsesSharedFaceInsteadOfPrecomposedShopImage() {
    ShopService shop =
        new ShopService(new ShopItem[] {new ShopItem("offer", "strike", "Strike", 20, 1)});
    ShopDisplay display =
        new ShopDisplay(new ShopEncounter(new InventoryComponent(100), shop), library);
    entity = new Entity().addComponent(display);
    entity.create();

    List<CardWidget> widgets = widgets();
    assertEquals(1, widgets.size());
    assertCommonFrame(widgets.getFirst());
    assertEquals("Strike", widgets.getFirst().displayedName());
    assertEquals("Deal 6 damage.", widgets.getFirst().displayedDescription());
  }

  @Test
  void upgradeChooserUsesRarityFaceAndKeepsExactInstanceSelection() {
    for (String cardId : List.of("strike", "iron_oath", "sealed_pact")) {
      assertUpgradeChooser(cardId);
      entity.dispose();
      entity = null;
    }
  }

  private void assertUpgradeChooser(String cardId) {
    CardInstance instance = new CardInstance("owned-" + cardId, cardId, 0);
    PlayerDeck deck = PlayerDeck.fromInstances(library, List.of(instance));
    CardUpgradeSelection selection = CardUpgradeSelection.forPlayerDeck(deck, library, 1);
    entity =
        new Entity()
            .addComponent(
                new CardUpgradeDisplay(selection, new PlayerDeckCardUpgradeCommitter(deck)));
    entity.create();

    CardWidget face = widgets().getFirst();
    assertRarityFrame(face);
    assertEquals(instance.instanceId(), face.getCard().instanceId());
    selection.toggle(instance.instanceId());
    assertEquals(List.of(instance.instanceId()), selection.getSelectedInstanceIds());
  }

  @Test
  void deckEditorKeepsDuplicateIdentityAndNestedSelectionTintsWithCommonFrame() {
    CardInstance base = new CardInstance("strike-base", "strike", 0);
    CardInstance upgraded = new CardInstance("strike-plus", "strike", 1);
    CardPlayService play = mock(CardPlayService.class);
    when(play.allInstances()).thenReturn(List.of(base, upgraded));
    when(play.currentHand()).thenReturn(List.of(base));
    when(play.discardedInstances()).thenReturn(List.of(upgraded));
    PopupDisplay popup = new PopupDisplay("Deck");
    ClickableFactory factory = new ClickableFactory(List.of());
    DeckEditorComponent editor = new DeckEditorComponent(play, library, popup, factory, null);
    entity = new Entity().addComponent(popup).addComponent(factory).addComponent(editor);
    entity.create();
    editor.open();

    List<Clickable> cards = factory.getByTrigger("toggleDeckCard");
    assertEquals(2, cards.size());
    CardWidget baseFace =
        assertInstanceOf(CardWidget.class, cards.getFirst().getBtn().getChildren().first());
    CardWidget upgradeFace =
        assertInstanceOf(CardWidget.class, cards.getLast().getBtn().getChildren().first());
    assertCommonFrame(baseFace);
    assertCommonFrame(upgradeFace);
    assertEquals(base.instanceId(), cards.getFirst().getArgs()[0]);
    assertEquals(upgraded.instanceId(), cards.getLast().getArgs()[0]);
    assertEquals("Strike+", upgradeFace.displayedName());
    assertEquals("Deal 12 damage.", upgradeFace.displayedDescription());
    assertEquals(new Color(0.35f, 0.35f, 0.35f, 1f), frameImage(upgradeFace).getColor());
    assertNotEquals(Color.WHITE, frameImage(baseFace).getColor());

    cards.getFirst().getBtn().fire(new ChangeEvent());
    assertEquals(Color.WHITE, frameImage(baseFace).getColor());
    assertEquals(new Color(0.35f, 0.35f, 0.35f, 1f), frameImage(upgradeFace).getColor());
    cards.getFirst().getBtn().validate();
    assertTrue(baseFace.getWidth() <= cards.getFirst().getBtn().getWidth());
    assertTrue(baseFace.getHeight() <= cards.getFirst().getBtn().getHeight());
    popup.hide();
    assertTrue(factory.getByTrigger("toggleDeckCard").isEmpty());
  }

  @Test
  void battleDragPreviewUsesSameRarityFrameWithoutReparentingLiveCard() {
    for (String cardId : List.of("strike", "iron_oath", "sealed_pact")) {
      assertBattleDragPreview(cardId);
    }
  }

  private void assertBattleDragPreview(String cardId) {
    DragAndDrop dragAndDrop = mock(DragAndDrop.class);
    DragNDropService dragService = mock(DragNDropService.class);
    when(dragService.getDragAndDrop()).thenReturn(dragAndDrop);
    ServiceLocator.registerDragNDropService(dragService);
    var resolved =
        new CardResolver()
            .resolve(
                library.getCard(cardId).orElseThrow(),
                new CardInstance("battle-" + cardId, cardId, 1));
    DragNDrop clickable =
        new DragNDrop(
            ClickableRecord.builder("playCard")
                .text(resolved.name())
                .args(resolved.instanceId())
                .size(90f, 130f)
                .build());
    clickable.getBtn().setSize(90f, 130f);
    CardWidgetAssets assets =
        CardWidgetAssets.fromManagedResources(UIComponent.getSharedSkin(), resources);
    clickable.setVisualContent(() -> new CardWidget(resolved, assets));
    CardWidget live = (CardWidget) clickable.getBtn().getChildren().first();
    ArgumentCaptor<DragAndDrop.Source> source = ArgumentCaptor.forClass(DragAndDrop.Source.class);
    verify(dragAndDrop).addSource(source.capture());

    DragAndDrop.Payload payload = source.getValue().dragStart(new InputEvent(), 0f, 0f, 0);
    Button ghost = assertInstanceOf(Button.class, payload.getDragActor());
    ghost.validate();
    CardWidget preview = assertInstanceOf(CardWidget.class, ghost.getChildren().first());
    assertRarityFrame(live);
    assertRarityFrame(preview);
    assertNotSame(live, preview);
    assertSame(clickable.getBtn(), live.getParent());
    assertEquals(resolved.instanceId(), preview.getCard().instanceId());
    assertEquals(resolved.name(), preview.displayedName());
    assertTrue(preview.getWidth() <= ghost.getWidth());
    assertTrue(preview.getHeight() <= ghost.getHeight());
    clickable.remove();
  }

  @Test
  void legacyCardsOverlayAlsoUsesSharedFacesForAllItsDefinitions() {
    entity = new Entity().addComponent(new CardHandDisplay());
    entity.create();
    List<CardWidget> widgets = widgets();
    assertEquals(6, widgets.size());
    widgets.forEach(this::assertRarityFrame);
  }

  private List<CardWidget> widgets() {
    List<CardWidget> result = new ArrayList<>();
    for (Actor actor : stage.getActors()) {
      collectWidgets(actor, result);
    }
    return result;
  }

  private static void collectWidgets(Actor actor, List<CardWidget> widgets) {
    if (actor instanceof CardWidget widget) {
      widgets.add(widget);
    } else if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        collectWidgets(child, widgets);
      }
    }
  }

  private void assertCommonFrame(CardWidget widget) {
    assertEquals(Rarity.COMMON, widget.getCard().rarity());
    assertRarityFrame(widget);
  }

  private void assertRarityFrame(CardWidget widget) {
    TextureRegionDrawable frame =
        assertInstanceOf(TextureRegionDrawable.class, widget.displayedFrame());
    assertSame(
        resources.getAsset(
            switch (widget.getCard().rarity()) {
              case COMMON -> CardWidgetAssets.COMMON_FRAME_TEXTURE;
              case UNCOMMON -> CardWidgetAssets.UNCOMMON_FRAME_TEXTURE;
              case RARE -> CardWidgetAssets.RARE_FRAME_TEXTURE;
            },
            Texture.class),
        frame.getRegion().getTexture());
  }

  private static Image frameImage(CardWidget widget) {
    widget.validate();
    return ((Group) widget.getChildren().first()).findActor("card-frame");
  }
}
