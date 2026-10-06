package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardEntryView;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class CardLibraryDisplayTest {
  @Test
  void shouldSortAllCardsByRarityThenNameWithoutRevealingLockedEntries() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    List<CardConfig> expected =
        List.of(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE).stream()
            .flatMap(
                rarity ->
                    cards.stream()
                        .filter(card -> card.rarity == rarity)
                        .sorted(Comparator.comparing(card -> card.name)))
            .toList();
    assertEquals(26, expected.size());
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());
    Group cardList = ((Group) root.getValue()).findActor("card-library-list");
    assertEquals(expected.size(), cardList.getChildren().size);

    // Verify actual button order, including anonymous entries, rather than just a comparator.
    for (int i = 0; i < expected.size(); i++) {
      TextButton button = assertInstanceOf(TextButton.class, cardList.getChildren().get(i));
      assertEquals("???", button.getText().toString());
      button.fire(new ChangeEvent());
      assertEquals(expected.get(i).id, display.getDisplayedEntry().cardId());
      assertTrue(display.isLockedArtworkVisible());
      assertEquals(CardUnlockState.LOCKED, display.getDisplayedEntry().unlockState());
    }

    discovery.recordSeenAll(cards.stream().map(card -> card.id).toList());
    for (int i = 0; i < expected.size(); i++) {
      TextButton button = assertInstanceOf(TextButton.class, cardList.getChildren().get(i));
      CardConfig card = expected.get(i);
      assertEquals(card.cost + "  " + card.name, button.getText().toString());
      button.fire(new ChangeEvent());
      assertEquals(card.id, display.getDisplayedEntry().cardId());
      assertFalse(display.isLockedArtworkVisible());
    }
    display.dispose();
  }

  @Test
  void shouldUseManagedCommonFrameOnlyAfterCardIsDiscovered() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    ResourceService resources = ServiceLocator.getResourceService();
    Texture frame = mock(Texture.class);
    when(frame.getWidth()).thenReturn(450);
    when(frame.getHeight()).thenReturn(912);
    when(resources.containsAsset(CardWidgetAssets.COMMON_FRAME_TEXTURE, Texture.class))
        .thenReturn(true);
    when(resources.getAsset(CardWidgetAssets.COMMON_FRAME_TEXTURE, Texture.class))
        .thenReturn(frame);
    display.create();
    assertTrue(display.isLockedArtworkVisible());

    discovery.recordSeen("strike");
    display.showCard(discovery.getEntry("strike").orElseThrow());
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());
    CardWidget widget = findWidget(root.getValue());
    assertTrue(display.isStandardCardVisible());
    assertEquals("Strike", widget.getCard().name());
    Image frameImage = ((Group) widget.getChildren().first()).findActor("card-frame");
    TextureRegionDrawable drawable =
        assertInstanceOf(TextureRegionDrawable.class, frameImage.getDrawable());
    assertSame(frame, drawable.getRegion().getTexture());
    display.dispose();
  }

  private static CardWidget findWidget(Actor actor) {
    if (actor instanceof CardWidget widget) {
      return widget;
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        CardWidget found = findWidget(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  @Test
  void shouldKeepManagedRarityFramesHiddenUntilDiscovery() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    ResourceService resources = ServiceLocator.getResourceService();
    for (String path :
        List.of(
            CardWidgetAssets.COMMON_FRAME_TEXTURE,
            CardWidgetAssets.UNCOMMON_FRAME_TEXTURE,
            CardWidgetAssets.RARE_FRAME_TEXTURE)) {
      Texture frame = mock(Texture.class);
      when(frame.getWidth()).thenReturn(450);
      when(frame.getHeight()).thenReturn(912);
      when(resources.containsAsset(path, Texture.class)).thenReturn(true);
      when(resources.getAsset(path, Texture.class)).thenReturn(frame);
    }
    display.create();
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());
    for (String cardId : List.of("strike", "inner_focus", "poison_mark")) {
      display.showCard(discovery.getEntry(cardId).orElseThrow());
      assertTrue(display.isLockedArtworkVisible());
      assertFalse(display.isStandardCardVisible());
      discovery.recordSeen(cardId);
      assertTrue(display.isStandardCardVisible());
      assertFalse(display.isUncommonCardVisible());
      assertFalse(display.isLockedArtworkVisible());
      CardWidget widget = findWidget(root.getValue());
      assertEquals(cardId, widget.getCard().cardId());
      String path =
          switch (widget.getCard().rarity()) {
            case COMMON -> CardWidgetAssets.COMMON_FRAME_TEXTURE;
            case UNCOMMON -> CardWidgetAssets.UNCOMMON_FRAME_TEXTURE;
            case RARE -> CardWidgetAssets.RARE_FRAME_TEXTURE;
          };
      Image frameImage = ((Group) widget.getChildren().first()).findActor("card-frame");
      TextureRegionDrawable drawable =
          assertInstanceOf(TextureRegionDrawable.class, frameImage.getDrawable());
      assertSame(resources.getAsset(path, Texture.class), drawable.getRegion().getTexture());
    }
    display.dispose();
  }

  @Test
  void shouldRenderLockedPlaceholders() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);

    display.create();

    assertEquals(CardUnlockState.LOCKED, display.getDisplayedEntry().unlockState());
    assertEquals("UNDISCOVERED", display.getStateText());
    assertEquals("Find this card to reveal its record.", display.getDescriptionText());
    assertEquals("Cost: ???", display.getCostText());
    assertEquals("???", CardLibraryDisplay.formatCardButton(display.getDisplayedEntry()));
    assertTrue(display.isLockedArtworkVisible());
    assertFalse(display.isStandardCardVisible());
    assertFalse(display.isUncommonCardVisible());
    display.dispose();
  }

  @Test
  void shouldRefreshDisplayedEntryOnDiscoveryEventAndUnsubscribeOnDispose() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    String cardId = display.getDisplayedEntry().cardId();

    discovery.recordSeen(cardId);

    assertEquals(CardUnlockState.SEEN, display.getDisplayedEntry().unlockState());
    assertEquals("SEEN", display.getStateText());
    assertFalse(display.isLockedArtworkVisible());

    display.dispose();
    discovery.replaceProgress(Map.of());
    assertEquals(CardUnlockState.SEEN, display.getDisplayedEntry().unlockState());
  }

  @Test
  void shouldSelectFrameOnlyAfterDiscoveryUsingLatestEntryView() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();

    discovery.recordSeen("poison_dagger");
    display.showCard(discovery.getEntry("poison_dagger").orElseThrow());

    assertTrue(display.isUncommonCardVisible());
    assertFalse(display.isStandardCardVisible());
    assertFalse(display.isLockedArtworkVisible());

    discovery.recordSeen("strike");
    display.showCard(discovery.getEntry("strike").orElseThrow());

    assertTrue(display.isStandardCardVisible());
    assertFalse(display.isUncommonCardVisible());
    display.dispose();
  }

  @Test
  void shouldShowDiscoveredInnerFocusWithUncommonFrame() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    discovery.recordSeen("inner_focus");
    CardEntryView entry = discovery.getEntry("inner_focus").orElseThrow();

    display.showCard(entry);

    assertEquals(Rarity.UNCOMMON, CardLibraryDisplay.resolveForDisplay(entry).rarity());
    assertTrue(display.isUncommonCardVisible());
    assertFalse(display.isStandardCardVisible());
    assertFalse(display.isLockedArtworkVisible());
    display.dispose();
  }

  @Test
  void shouldResolveOnlySeenEntriesForPresentation() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardEntryView locked = discovery.getEntry("strike").orElseThrow();

    assertThrows(
        IllegalArgumentException.class, () -> CardLibraryDisplay.resolveForDisplay(locked));

    discovery.recordSeen("strike");
    CardEntryView seen = discovery.getEntry("strike").orElseThrow();

    assertEquals("strike", CardLibraryDisplay.resolveForDisplay(seen).cardId());
  }

  @Test
  void shouldShowLoreOnlyForDiscoveredCardsThatHaveIt() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    CardConfig strike =
        cards.stream().filter(card -> "strike".equals(card.id)).findFirst().orElseThrow();
    strike.lore = "A test-only fragment of card history.";
    CardConfig defend =
        cards.stream().filter(card -> "defend".equals(card.id)).findFirst().orElseThrow();
    defend.lore = null;
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    discovery.recordSeen("strike");
    discovery.recordSeen("defend");
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();

    display.showCard(discovery.getEntry("strike").orElseThrow());
    assertEquals("A test-only fragment of card history.", display.getLoreText());

    display.showCard(discovery.getEntry("defend").orElseThrow());
    assertEquals("", display.getLoreText());
    display.dispose();
  }

  @Test
  void shouldOmitArtworkPathsFromAllLibraryDetails() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());

    assertNoArtworkPath(root.getValue());
    for (CardConfig card : CardConfigLoader.loadCards()) {
      discovery.recordSeen(card.id);
      display.showCard(discovery.getEntry(card.id).orElseThrow());
      assertEquals(card.description, display.getDescriptionText(), card.id);
      assertNoArtworkPath(root.getValue());
    }
    display.dispose();
  }

  private static void assertNoArtworkPath(Actor root) {
    List<String> texts = new ArrayList<>();
    collectLabelTexts(root, texts);
    assertFalse(texts.stream().anyMatch(text -> text.contains("Artwork:")));
    assertFalse(texts.stream().anyMatch(text -> text.contains("images/cards/")));
  }

  private static void collectLabelTexts(Actor actor, List<String> texts) {
    if (actor instanceof Label label) {
      texts.add(label.getText().toString());
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        collectLabelTexts(child, texts);
      }
    }
  }

  private CardLibraryDisplay createDisplay(CardDiscoveryService discovery) {
    RenderService renderService = mock(RenderService.class);
    Stage libraryStage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(libraryStage);
    ServiceLocator.registerRenderService(renderService);

    ResourceService resources = mock(ResourceService.class);
    Texture artwork = mock(Texture.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(artwork);
    ServiceLocator.registerResourceService(resources);
    return new CardLibraryDisplay(mock(GdxGame.class), discovery);
  }
}
