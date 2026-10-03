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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardEntryView;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class CardLibraryDisplayTest {
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
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resources);
    return new CardLibraryDisplay(mock(GdxGame.class), discovery);
  }
}
