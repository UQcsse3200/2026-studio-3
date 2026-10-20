package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class CardLibraryDisplayTest {
  @Test
  void checkboxPreviewsAndRestoresEveryProductionUpgradeWithoutChangingDiscovery() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    assertEquals(26, cards.size());
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    discovery.recordSeenAll(cards.stream().map(card -> card.id).toList());
    Map<String, CardUnlockState> progress = discovery.getProgressSnapshot();
    CardLibraryDisplay display = createDisplay(discovery);
    ResourceService resources = ServiceLocator.getResourceService();
    when(resources.containsAsset(anyString(), eq(Texture.class))).thenReturn(true);
    display.create();
    Group root = rootActor();
    CheckBox checkbox = root.findActor("card-library-upgrade-preview");
    for (CardConfig config : cards) {
      display.showCard(discovery.getEntry(config.id).orElseThrow());
      CardWidget widget = findWidget(root);
      assertTrue(checkbox.isVisible(), config.id);
      assertFalse(checkbox.isDisabled(), config.id);
      assertFalse(checkbox.isChecked(), config.id);
      assertEquals(config.name, widget.getCard().name(), config.id);
      togglePreview(checkbox, true);
      var resolved = widget.getCard();
      assertTrue(resolved.upgraded(), config.id);
      assertEquals(config.id, resolved.cardId(), config.id);
      assertEquals(config.upgrade.name, resolved.name(), config.id);
      assertEquals(config.upgrade.cost, resolved.cost(), config.id);
      assertEquals(config.upgrade.description, resolved.description(), config.id);
      assertEquals(config.upgrade.rarity, resolved.rarity(), config.id);
      assertEquals(config.texturePath, resolved.texturePath(), config.id);
      assertEquals(config.type, resolved.type(), config.id);
      assertEquals(config.target, resolved.target(), config.id);
      assertEquals(config.upgrade.description, display.getDescriptionText(), config.id);
      assertEquals("Cost: " + config.upgrade.cost, display.getCostText(), config.id);
      assertEquals(CardWidget.CARD_WIDTH, widget.getWidth(), config.id);
      assertEquals(CardWidget.CARD_HEIGHT, widget.getHeight(), config.id);
      assertNoArtworkPath(root);
      togglePreview(checkbox, false);
      assertFalse(widget.getCard().upgraded(), config.id);
      assertEquals(config.name, widget.getCard().name(), config.id);
      assertEquals(config.description, display.getDescriptionText(), config.id);
      assertEquals("Cost: " + config.cost, display.getCostText(), config.id);
    }
    assertEquals(progress, discovery.getProgressSnapshot());
    display.dispose();
  }

  @Test
  void previewChoiceIsPerCardSurvivesMatchingSearchAndResetsAfterRelocking() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("strike");
    discovery.recordSeen("purify");
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    Group root = rootActor();
    CheckBox checkbox = root.findActor("card-library-upgrade-preview");
    display.showCard(discovery.getEntry("purify").orElseThrow());
    togglePreview(checkbox, true);
    assertEquals("Cost: 0", display.getCostText());
    TextField search = root.findActor("card-library-search");
    search.setText("pur");
    assertTrue(checkbox.isChecked());
    assertEquals("Cost: 0", display.getCostText());
    assertEquals(List.of("Purify"), visibleNames(root));
    search.setText("");
    display.showCard(discovery.getEntry("strike").orElseThrow());
    assertFalse(checkbox.isChecked());
    display.showCard(discovery.getEntry("purify").orElseThrow());
    assertTrue(checkbox.isChecked());
    discovery.replaceProgress(Map.of());
    assertFalse(checkbox.isChecked());
    assertFalse(checkbox.isVisible());
    assertTrue(checkbox.isDisabled());
    assertTrue(display.isLockedArtworkVisible());
    assertEquals("Cost: ???", display.getCostText());
    discovery.recordSeen("purify");
    assertFalse(checkbox.isChecked());
    assertEquals("Cost: 1", display.getCostText());
    display.dispose();
  }

  @Test
  void lockedMissingAndFilteredEntriesCannotExposeOrToggleAnUpgrade() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    cards.stream().filter(card -> "strike".equals(card.id)).findFirst().orElseThrow().upgrade =
        null;
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    Group root = rootActor();
    CheckBox checkbox = root.findActor("card-library-upgrade-preview");
    assertFalse(checkbox.isVisible());
    assertTrue(checkbox.isDisabled());
    togglePreview(checkbox, true);
    assertTrue(display.isLockedArtworkVisible());
    discovery.recordSeen("strike");
    display.showCard(discovery.getEntry("strike").orElseThrow());
    assertTrue(checkbox.isVisible());
    assertTrue(checkbox.isDisabled());
    assertFalse(checkbox.isChecked());
    TextField search = root.findActor("card-library-search");
    search.setText("no matches");
    assertNull(display.getDisplayedEntry());
    assertFalse(checkbox.isVisible());
    assertTrue(checkbox.isDisabled());
    togglePreview(checkbox, true);
    assertEquals("", display.getDescriptionText());
    display.dispose();
  }

  @Test
  void disposedUpgradeControlCannotRefreshTheLibrary() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("strike");
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    display.showCard(discovery.getEntry("strike").orElseThrow());
    Group root = rootActor();
    CheckBox checkbox = root.findActor("card-library-upgrade-preview");
    display.dispose();
    togglePreview(checkbox, true);
    assertEquals("Deal 6 damage.", display.getDescriptionText());
  }

  private static void togglePreview(CheckBox checkbox, boolean checked) {
    checkbox.setChecked(checked);
    checkbox.fire(new ChangeEvent());
  }

  @Test
  void clickingTheCheckboxTogglesAndKeepsThePreviewAcrossViewportResizes() {
    Graphics previous = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(720);
    Stage realStage = new Stage(new ScreenViewport(), mock(Batch.class));
    realStage.getViewport().update(1280, 720, true);
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("purify");
    CardLibraryDisplay display = createDisplay(discovery);
    when(ServiceLocator.getRenderService().getStage()).thenReturn(realStage);
    try {
      display.create();
      display.showCard(discovery.getEntry("purify").orElseThrow());
      CheckBox checkbox = realStage.getRoot().findActor("card-library-upgrade-preview");
      checkbox.getClickListener().clicked(new com.badlogic.gdx.scenes.scene2d.InputEvent(), 0f, 0f);
      assertTrue(checkbox.isChecked());
      assertEquals("Cost: 0", display.getCostText());
      for (int[] size : new int[][] {{1920, 1080}, {1280, 720}}) {
        when(Gdx.graphics.getWidth()).thenReturn(size[0]);
        when(Gdx.graphics.getHeight()).thenReturn(size[1]);
        realStage.getViewport().update(size[0], size[1], true);
        realStage.getRoot().act(0f);
        assertTrue(checkbox.isChecked());
        assertEquals("Cost: 0", display.getCostText());
        assertEquals("purify", display.getDisplayedEntry().cardId());
      }
      checkbox.getClickListener().clicked(new com.badlogic.gdx.scenes.scene2d.InputEvent(), 0f, 0f);
      assertFalse(checkbox.isChecked());
      assertEquals("Cost: 1", display.getCostText());
    } finally {
      display.dispose();
      realStage.dispose();
      Gdx.graphics = previous;
    }
  }

  @Test
  void searchReturnsOnlyDiscoveredPartialNameMatchesWithoutChangingProgress() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("poison_dagger");
    discovery.recordSeen("poison_blade");
    discovery.recordSeen("strike");
    Map<String, CardUnlockState> before = discovery.getProgressSnapshot();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    Group root = rootActor();
    TextField search = root.findActor("card-library-search");
    search.setText("  pOiSoN  ");
    assertEquals(List.of("Poison Blade", "Poison Dagger"), visibleNames(root));
    assertEquals(
        "2 results", ((Label) root.findActor("card-library-result-count")).getText().toString());
    for (String query :
        List.of("Poison Flask", "???", "poison_dagger", "images/cards/", "Deal 4")) {
      search.setText(query);
      assertTrue(visibleNames(root).isEmpty(), query);
      assertEquals(
          "0 results", ((Label) root.findActor("card-library-result-count")).getText().toString());
    }
    search.setText("agGe");
    assertEquals(List.of("Poison Dagger"), visibleNames(root));
    assertEquals(before, discovery.getProgressSnapshot());
    display.dispose();
  }

  @Test
  void clearingSearchRestoresOriginalOrderingAndLockedPlaceholders() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("strike");
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    Group root = rootActor();
    List<String> original = visibleNames(root);
    TextField search = root.findActor("card-library-search");
    search.setText("str");
    assertEquals(List.of("Strike"), visibleNames(root));
    search.setText(" \t ");
    assertEquals(original, visibleNames(root));
    search.setText("str");
    TextButton clear = root.findActor("card-library-search-clear");
    clear.setChecked(true);
    assertEquals("", search.getText());
    assertEquals(original, visibleNames(root));
    verify(ServiceLocator.getRenderService().getStage()).setKeyboardFocus(search);
    display.dispose();
  }

  @Test
  void filteringClearsStaleDetailsAndKeepsASelectionThatStillMatches() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("strike");
    discovery.recordSeen("defend");
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    Group root = rootActor();
    Group list = root.findActor("card-library-list");
    TextButton strike =
        java.util.Arrays.stream(list.getChildren().toArray(Actor.class))
            .filter(
                actor ->
                    actor instanceof TextButton button && "Strike".contentEquals(button.getText()))
            .map(TextButton.class::cast)
            .findFirst()
            .orElseThrow();
    strike.setChecked(true);
    TextField search = root.findActor("card-library-search");
    search.setText("str");
    assertEquals("strike", display.getDisplayedEntry().cardId());
    search.setText("def");
    assertNull(display.getDisplayedEntry());
    assertEquals("", display.getDescriptionText());
    assertEquals("", display.getCostText());
    assertFalse(display.isStandardCardVisible());
    assertFalse(display.isUncommonCardVisible());
    assertFalse(display.isLockedArtworkVisible());
    // Detached buttons cannot restore filtered-out details via an old callback.
    strike.setChecked(true);
    assertNull(display.getDisplayedEntry());
    TextButton defend = (TextButton) list.getChildren().first();
    defend.setChecked(true);
    assertEquals("defend", display.getDisplayedEntry().cardId());
    search.setText("no matches");
    assertNull(display.getDisplayedEntry());
    Label message = root.findActor("card-library-selection-message");
    assertEquals("No discovered cards match your search.", message.getText().toString());
    display.dispose();
  }

  @Test
  void liveDiscoveryReappliesActiveSearchAndRelockingRemovesSelectedDetails() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    Group root = rootActor();
    TextField search = root.findActor("card-library-search");
    search.setText("poison");
    assertTrue(visibleNames(root).isEmpty());
    discovery.recordSeen("poison_dagger");
    assertEquals(List.of("Poison Dagger"), visibleNames(root));
    assertEquals("poison", search.getText());
    Group list = root.findActor("card-library-list");
    ((TextButton) list.getChildren().first()).setChecked(true);
    discovery.recordSeen("strike");
    assertEquals("poison_dagger", display.getDisplayedEntry().cardId());
    discovery.replaceProgress(Map.of());
    assertTrue(visibleNames(root).isEmpty());
    assertNull(display.getDisplayedEntry());
    display.dispose();
    discovery.recordSeen("poison_dagger");
    assertNull(display.getDisplayedEntry());
  }

  @Test
  void nameMatchingUsesLiteralPunctuationAndLocaleIndependentCase() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    CardConfig strike =
        cards.stream().filter(card -> "strike".equals(card.id)).findFirst().orElseThrow();
    strike.name = "IRON [Ward]+";
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    discovery.recordSeen("strike");
    CardEntryView entry = discovery.getEntry("strike").orElseThrow();
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      assertTrue(CardLibraryDisplay.matchesSearch(entry, "iron"));
      assertTrue(CardLibraryDisplay.matchesSearch(entry, "[Ward]+"));
      assertFalse(CardLibraryDisplay.matchesSearch(entry, ".*"));
      assertFalse(
          CardLibraryDisplay.matchesSearch(discovery.getEntry("defend").orElseThrow(), "???"));
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  void searchFieldReceivesFAsTextAndResizingPreservesTheQuery() {
    Graphics previous = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(800);
    Stage realStage = new Stage(new ScreenViewport(), mock(Batch.class));
    realStage.getViewport().update(1280, 800, true);
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    discovery.recordSeen("defend");
    CardLibraryDisplay display = createDisplay(discovery);
    when(ServiceLocator.getRenderService().getStage()).thenReturn(realStage);
    try {
      display.create();
      TextField search = realStage.getRoot().findActor("card-library-search");
      realStage.setKeyboardFocus(search);
      realStage.keyDown(Keys.F);
      realStage.keyTyped('f');
      realStage.keyUp(Keys.F);
      assertEquals("f", search.getText());
      when(Gdx.graphics.getWidth()).thenReturn(1920);
      when(Gdx.graphics.getHeight()).thenReturn(1080);
      realStage.getViewport().update(1920, 1080, true);
      assertEquals("f", search.getText());
      assertSame(search, realStage.getKeyboardFocus());
      display.dispose();
      assertNull(realStage.getKeyboardFocus());
      CardLibraryDisplay reopened = new CardLibraryDisplay(mock(GdxGame.class), discovery);
      reopened.create();
      TextField freshSearch = realStage.getRoot().findActor("card-library-search");
      assertEquals("", freshSearch.getText());
      assertEquals(26, visibleNames(realStage.getRoot()).size());
      reopened.dispose();
    } finally {
      realStage.dispose();
      Gdx.graphics = previous;
    }
  }

  private static Group rootActor() {
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());
    return (Group) root.getValue();
  }

  private static List<String> visibleNames(Group root) {
    Group list = root.findActor("card-library-list");
    List<String> names = new ArrayList<>();
    for (Actor actor : list.getChildren()) {
      if (actor instanceof TextButton button) names.add(button.getText().toString());
    }
    return names;
  }

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
      button.setChecked(true);
      assertEquals(expected.get(i).id, display.getDisplayedEntry().cardId());
      assertTrue(display.isLockedArtworkVisible());
      assertEquals(CardUnlockState.LOCKED, display.getDisplayedEntry().unlockState());
    }

    discovery.recordSeenAll(cards.stream().map(card -> card.id).toList());
    for (int i = 0; i < expected.size(); i++) {
      TextButton button = assertInstanceOf(TextButton.class, cardList.getChildren().get(i));
      CardConfig card = expected.get(i);
      assertEquals(card.name, button.getText().toString());
      button.setChecked(true);
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
    assertEquals("Find this card to reveal its lore.", display.getDescriptionText());
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
