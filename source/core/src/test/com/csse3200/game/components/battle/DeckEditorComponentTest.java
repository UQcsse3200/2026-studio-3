package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.components.spritedisplay.clickable.CardImageSkins;
import com.csse3200.game.components.spritedisplay.clickable.CardWidgetInstaller;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.displaying.CardBadgesDisplay;
import com.csse3200.game.components.spritedisplay.displaying.CardPreviewDisplay;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class DeckEditorComponentTest {
  private static final String SELECT_TRIGGER = "toggleDeckCard";
  private static final String SCROLL_UP_TRIGGER = "deckScrollUp";
  private static final String SCROLL_DOWN_TRIGGER = "deckScrollDown";

  // Layout constants mirrored from DeckEditorComponent.
  private static final float CARD_WIDTH = 100f;
  private static final float CARD_HEIGHT = 145f;
  private static final float CARD_STEP_X = CARD_WIDTH + 14f;
  private static final float CARD_STEP_Y = CARD_HEIGHT + 14f;

  private static final float STAGE_HEIGHT = 800f;
  private static final float WINDOW_X = 100f;
  private static final float WINDOW_Y = 50f;
  private static final float WINDOW_HEIGHT = 600f;
  private static final float GRID_ORIGIN_X = WINDOW_X + 40f;
  private static final float GRID_TOP_Y = WINDOW_Y + WINDOW_HEIGHT - 70f;

  private static final Color SELECTED_TINT = new Color(1f, 0.85f, 0.5f, 1f);
  private static final Color DISCARDED_TINT = new Color(0.35f, 0.35f, 0.35f, 1f);
  private static final Color SCROLL_DISABLED_TINT = new Color(0.5f, 0.5f, 0.5f, 1f);

  private MockedStatic<CardImageSkins> cardSkins;
  private MockedStatic<CardWidgetInstaller> widgetInstaller;
  private Stage stage;
  private CardPlayService cardPlayService;
  private CardLibrary library;
  private PopupDisplay popup;
  private ClickableFactory poolFactory;
  private Table content;
  private Button scrollUpBtn;
  private Button scrollDownBtn;
  private Runnable onShow;
  private Runnable onHide;
  private Runnable onWindowClicked;
  private Entity entity;
  private DeckEditorComponent editor;

  private final List<List<CardInstance>> deckChanges = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();
  private final List<String> summaries = new ArrayList<>();
  private final List<CardPreviewDisplay.Content> previews = new ArrayList<>();
  private final List<List<CardBadgesDisplay.Badge>> badges = new ArrayList<>();
  private final List<String> lifecycle = new ArrayList<>();

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, STAGE_HEIGHT), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);

    // Avoid loading real card textures in the headless test environment.
    cardSkins = mockStatic(CardImageSkins.class);
    cardSkins.when(() -> CardImageSkins.forTexturePath(anyString())).thenReturn(new Skin());
    widgetInstaller = mockStatic(CardWidgetInstaller.class);

    cardPlayService = mock(CardPlayService.class);
    library = new CardLibrary(CardConfigLoader.loadCards());

    content = new Table();
    popup = mock(PopupDisplay.class);
    when(popup.getContentTable()).thenReturn(content);
    when(popup.getWindowX()).thenReturn(WINDOW_X);
    when(popup.getWindowY()).thenReturn(WINDOW_Y);
    when(popup.getWindowHeight()).thenReturn(WINDOW_HEIGHT);
    doAnswer(inv -> onShow = inv.getArgument(0)).when(popup).setOnShow(any());
    doAnswer(inv -> onHide = inv.getArgument(0)).when(popup).setOnHide(any());
    doAnswer(inv -> onWindowClicked = inv.getArgument(0)).when(popup).setOnWindowClicked(any());
    doAnswer(inv -> runIfSet(onShow)).when(popup).show();
    doAnswer(inv -> runIfSet(onHide)).when(popup).hide();

    poolFactory = mock(ClickableFactory.class);
    scrollUpBtn = buttonWithChild();
    scrollDownBtn = buttonWithChild();
    // Built before stubbing: Mockito can't create a mock inside another mock's thenReturn().
    Clickable scrollUp = clickable(scrollUpBtn, new Object[0]);
    Clickable scrollDown = clickable(scrollDownBtn, new Object[0]);
    when(poolFactory.getByTrigger(SCROLL_UP_TRIGGER)).thenReturn(List.of(scrollUp));
    when(poolFactory.getByTrigger(SCROLL_DOWN_TRIGGER)).thenReturn(List.of(scrollDown));

    editor = newEditor(deckChanges::add);
    entity = new Entity().addComponent(editor);
    recordEvents(entity);
    entity.create();
  }

  @AfterEach
  void tearDown() {
    cardSkins.close();
    widgetInstaller.close();
  }

  // ---------------------------------------------------------------- construction

  @Test
  void constructorRejectsMissingDisplayFactory() {
    assertThrows(
        NullPointerException.class,
        () ->
            new DeckEditorComponent(
                cardPlayService,
                library,
                popup,
                poolFactory,
                null,
                mock(CardWidgetAssets.class),
                null));
  }

  @Test
  void constructorRejectsMissingCardWidgetAssets() {
    assertThrows(
        NullPointerException.class,
        () ->
            new DeckEditorComponent(
                cardPlayService,
                library,
                popup,
                poolFactory,
                mock(DisplayingFactory.class),
                null,
                null));
  }

  // ---------------------------------------------------------------- create

  @Test
  void createAppliesItemInventoryPanelLook() {
    verify(popup).setBackgroundTexture("images/ui/inventory-panel.png");
    verify(popup).setDefaultCloseButtonVisible(false);
  }

  @Test
  void createRegistersPopupCallbacks() {
    assertNotNull(onShow);
    assertNotNull(onHide);
    assertNotNull(onWindowClicked);
  }

  @Test
  void createBuildsHeaderFooterAndSetDeckButton() {
    assertNotNull(findLabel(content, "CARD INVENTORY"));
    assertNotNull(findLabel(content, "Pick 5 cards"));
    assertNotNull(findTextButton(content, "Set Deck"));
  }

  @Test
  void headerCloseButtonHidesPopup() {
    findTextButton(content, "X").fire(new ChangeListener.ChangeEvent());

    verify(popup).hide();
  }

  @Test
  void scrollButtonsStartHidden() {
    assertFalse(scrollUpBtn.isVisible());
    assertFalse(scrollDownBtn.isVisible());
  }

  @Test
  void windowClickReassertsDrawOrder() {
    onWindowClicked.run();

    assertEquals(
        List.of(DeckEditorEvents.TO_FRONT_BEHIND_CARDS, DeckEditorEvents.TO_FRONT), lifecycle);
    verify(poolFactory).bringToFront();
  }

  // ---------------------------------------------------------------- open

  @Test
  void openShowsPopupFiresOpenedAndClearsError() {
    givenDeck(instances("strike"), List.of(), List.of());

    editor.open();

    verify(popup).show();
    assertTrue(lifecycle.contains(DeckEditorEvents.OPENED));
    assertEquals("", last(errors));
  }

  @Test
  void openListsHandFirstThenRestWithoutDuplicates() {
    List<CardInstance> all = instances("strike", "defend", "strike", "defend", "strike");
    givenDeck(all, List.of(all.get(3), all.get(1)), List.of());

    editor.open();

    assertEquals(
        List.of("inst-3", "inst-1", "inst-0", "inst-2", "inst-4"), recordIds(lastCardRecords()));
  }

  @Test
  void openSummaryNumbersCurrentHandInOrder() {
    List<CardInstance> all = instances("strike", "defend");
    givenDeck(all, List.of(all.get(1), all.get(0)), List.of());

    editor.open();

    assertEquals("Selected (2): 1 Defend, 2 Strike", last(summaries));
  }

  @Test
  void openInstallsCardWidgetsForTheGrid() {
    givenDeck(instances("strike"), List.of(), List.of());

    editor.open();

    widgetInstaller.verify(
        () ->
            CardWidgetInstaller.install(
                eq(poolFactory), eq(SELECT_TRIGGER), eq(library), any(), any()),
        atLeastOnce());
  }

  @Test
  void cardRecordsArePositionedRelativeToPopupWindow() {
    givenDeck(instances(repeat("strike", 6)), List.of(), List.of());

    editor.open();

    List<ClickableRecord> records = lastCardRecords();
    // First card: top-left of the grid.
    assertEquals(GRID_ORIGIN_X, records.get(0).x(), 0.001f);
    assertEquals(STAGE_HEIGHT - (GRID_TOP_Y - CARD_HEIGHT) - 10f, records.get(0).y(), 0.001f);
    assertEquals(CARD_WIDTH, records.get(0).width(), 0.001f);
    assertEquals(CARD_HEIGHT, records.get(0).height(), 0.001f);
    // Fourth card: last column of the first row.
    assertEquals(GRID_ORIGIN_X + 3 * CARD_STEP_X, records.get(3).x(), 0.001f);
    // Sixth card: second column of the second row.
    assertEquals(GRID_ORIGIN_X + CARD_STEP_X, records.get(5).x(), 0.001f);
    assertEquals(
        STAGE_HEIGHT - (GRID_TOP_Y - CARD_STEP_Y - CARD_HEIGHT) - 10f, records.get(5).y(), 0.001f);
  }

  @Test
  void cardsMissingFromLibraryAreSkipped() {
    givenDeck(instances("strike", "ghost", "defend"), List.of(), List.of());

    editor.open();

    List<ClickableRecord> records = lastCardRecords();
    assertEquals(List.of("inst-0", "inst-2"), recordIds(records));
    assertEquals("Strike", records.get(0).label());
    assertEquals("Defend", records.get(1).label());
  }

  @Test
  void summaryFallsBackToCardIdWhenMissingFromLibrary() {
    List<CardInstance> all = instances("ghost");
    givenDeck(all, all, List.of());

    editor.open();

    assertEquals("Selected (1): 1 ghost", last(summaries));
  }

  @Test
  void openPreviewsFirstCardOnPage() {
    givenDeck(instances("defend", "strike"), List.of(), List.of());

    editor.open();

    CardPreviewDisplay.Content preview = last(previews);
    assertEquals(library.getCard("defend").orElseThrow().texturePath, preview.texturePath());
    assertTrue(preview.details().startsWith("Defend\nCost: "));
    assertTrue(preview.details().contains("Gain 5 block."));
  }

  @Test
  void emptyPoolBuildsNoCardsAndPreviewsNothing() {
    givenDeck(List.of(), List.of(), List.of());

    editor.open();

    assertTrue(lastCardRecords().isEmpty());
    assertEquals(CardPreviewDisplay.Content.NONE, last(previews));
    assertEquals("Selected (0): ", last(summaries));
  }

  // ---------------------------------------------------------------- selection

  @Test
  void toggleAddsThenRemovesCard() {
    List<CardInstance> all = instances("strike", "defend");
    givenDeck(all, List.of(), List.of());
    editor.open();

    toggle(all.get(1));
    assertEquals("Selected (1): 1 Defend", last(summaries));

    toggle(all.get(0));
    assertEquals("Selected (2): 1 Defend, 2 Strike", last(summaries));

    toggle(all.get(1));
    assertEquals("Selected (1): 1 Strike", last(summaries));
    assertEquals("", last(errors));
  }

  @Test
  void sixthCardIsRejectedWhenHandIsFull() {
    List<CardInstance> all = instances(repeat("strike", 6));
    givenDeck(all, all.subList(0, 5), List.of());
    editor.open();
    String summaryBefore = last(summaries);

    toggle(all.get(5));

    assertEquals("Hand is full, deselect a card before adding another.", last(errors));
    assertEquals(summaryBefore, last(summaries));
  }

  @Test
  void deselectingIsAllowedWhenHandIsFull() {
    List<CardInstance> all = instances(repeat("strike", 6));
    givenDeck(all, all.subList(0, 5), List.of());
    editor.open();

    toggle(all.get(0));
    toggle(all.get(5));

    assertTrue(last(summaries).startsWith("Selected (5): "));
    assertEquals("", last(errors));
  }

  @Test
  void toggleUnknownInstanceIsIgnored() {
    List<CardInstance> all = instances("strike");
    givenDeck(all, all, List.of());
    editor.open();
    int summaryCount = summaries.size();

    entity.getEvents().trigger(SELECT_TRIGGER, "no-such-instance");

    assertEquals(summaryCount, summaries.size());
  }

  @Test
  void togglePreviewsTheClickedCard() {
    List<CardInstance> all = instances("strike", "defend");
    givenDeck(all, List.of(), List.of());
    editor.open();

    toggle(all.get(1));

    assertTrue(last(previews).details().startsWith("Defend\n"));
  }

  @Test
  void badgesFollowSelectionOrderAndRenumber() {
    List<CardInstance> all = instances("strike", "defend", "strike");
    givenDeck(all, List.of(), List.of());
    editor.open();

    toggle(all.get(2));
    toggle(all.get(0));
    toggle(all.get(1));
    assertEquals(List.of(badge(1, 2), badge(2, 0), badge(3, 1)), sortedByNumber(last(badges)));

    toggle(all.get(2));
    assertEquals(List.of(badge(1, 0), badge(2, 1)), sortedByNumber(last(badges)));
  }

  @Test
  void badgesOnlyCoverCardsOnTheVisiblePage() {
    List<CardInstance> all = instances(repeat("strike", 12));
    givenDeck(all, List.of(all.get(0)), List.of());
    editor.open();
    assertEquals(List.of(badge(1, 0)), last(badges));

    // Scrolling one row down moves the selected first card off the visible page.
    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);

    assertTrue(last(badges).isEmpty());
  }

  @Test
  void tintsReflectSelectedAndDiscardedState() {
    List<CardInstance> all = instances("strike", "strike", "strike", "strike");
    givenDeck(all, List.of(all.get(1), all.get(3)), List.of(all.get(2), all.get(3)));
    Clickable plain = cardWidget(all.get(0));
    Clickable selected = cardWidget(all.get(1));
    Clickable discarded = cardWidget(all.get(2));
    Clickable both = cardWidget(all.get(3));
    givenCardWidgets(plain, selected, discarded, both);

    editor.open();

    verify(plain, atLeastOnce()).setContentTint(Color.WHITE);
    verify(selected, atLeastOnce()).setContentTint(SELECTED_TINT);
    verify(discarded, atLeastOnce()).setContentTint(DISCARDED_TINT);
    verify(both, atLeastOnce()).setContentTint(new Color(DISCARDED_TINT).lerp(SELECTED_TINT, 0.5f));
  }

  @Test
  void tintUpdatesWhenToggled() {
    List<CardInstance> all = instances("strike");
    givenDeck(all, List.of(), List.of());
    Clickable widget = cardWidget(all.get(0));
    givenCardWidgets(widget);
    editor.open();
    verify(widget, times(1)).setContentTint(Color.WHITE);

    toggle(all.get(0));

    verify(widget, times(1)).setContentTint(SELECTED_TINT);
  }

  @Test
  void widgetsWithoutKnownArgsAreSkipped() {
    givenDeck(instances("strike"), List.of(), List.of());
    Clickable noArgs = clickable(new Button(), new Object[0]);
    Clickable unknown = clickable(new Button(), new Object[] {"no-such-instance"});
    givenCardWidgets(noArgs, unknown);

    editor.open();

    verify(noArgs, never()).setContentTint(any());
    verify(unknown, never()).setContentTint(any());
  }

  @Test
  void mouseHoverPreviewsCard() {
    List<CardInstance> all = instances("strike", "defend");
    givenDeck(all, List.of(), List.of());
    Button defendBtn = new Button();
    givenCardWidgets(cardWidget(all.get(0)), clickable(defendBtn, new Object[] {"inst-1"}));
    editor.open();

    defendBtn.fire(enterEvent(-1));

    assertTrue(last(previews).details().startsWith("Defend\n"));
  }

  @Test
  void touchEnterDoesNotChangePreview() {
    List<CardInstance> all = instances("strike", "defend");
    givenDeck(all, List.of(), List.of());
    Button defendBtn = new Button();
    givenCardWidgets(cardWidget(all.get(0)), clickable(defendBtn, new Object[] {"inst-1"}));
    editor.open();
    int previewCount = previews.size();

    defendBtn.fire(enterEvent(0));

    assertEquals(previewCount, previews.size());
  }

  // ---------------------------------------------------------------- scrolling

  @Test
  void scrollButtonsStayHiddenWhenPoolFitsOnePage() {
    givenDeck(instances(repeat("strike", 8)), List.of(), List.of());

    editor.open();

    assertFalse(scrollUpBtn.isVisible());
    assertFalse(scrollDownBtn.isVisible());
  }

  @Test
  void scrollingDoesNothingWhenPoolFitsOnePage() {
    givenDeck(instances(repeat("strike", 8)), List.of(), List.of());
    editor.open();

    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);

    verify(poolFactory, times(1)).rebuildByTrigger(eq(SELECT_TRIGGER), anyList());
  }

  @Test
  void scrollDownMovesOneRowAndStopsAtBottom() {
    List<CardInstance> all = instances(repeat("strike", 12));
    givenDeck(all, List.of(), List.of());
    editor.open();

    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);
    assertEquals("inst-4", recordIds(lastCardRecords()).get(0));
    assertEquals(8, lastCardRecords().size());

    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);
    verify(poolFactory, times(2)).rebuildByTrigger(eq(SELECT_TRIGGER), anyList());
  }

  @Test
  void scrollUpAtTopDoesNothing() {
    givenDeck(instances(repeat("strike", 12)), List.of(), List.of());
    editor.open();

    entity.getEvents().trigger(SCROLL_UP_TRIGGER);

    verify(poolFactory, times(1)).rebuildByTrigger(eq(SELECT_TRIGGER), anyList());
  }

  @Test
  void scrollButtonsShowWhichDirectionsAreAvailable() {
    givenDeck(instances(repeat("strike", 12)), List.of(), List.of());
    editor.open();

    assertTrue(scrollUpBtn.isVisible());
    assertTrue(scrollDownBtn.isVisible());
    assertScrollButton(scrollUpBtn, false);
    assertScrollButton(scrollDownBtn, true);

    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);

    assertScrollButton(scrollUpBtn, true);
    assertScrollButton(scrollDownBtn, false);
  }

  @Test
  void previewStaysOnCardStillVisibleAfterScrolling() {
    List<CardInstance> all = instances(repeat("strike", 11));
    all.set(5, new CardInstance("inst-5", "defend", CardInstance.BASE_LEVEL));
    givenDeck(all, List.of(), List.of());
    editor.open();
    toggle(all.get(5));

    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);

    assertTrue(last(previews).details().startsWith("Defend\n"));
  }

  @Test
  void previewMovesToFirstVisibleCardWhenScrolledOffPage() {
    List<CardInstance> all = instances(repeat("strike", 12));
    all.set(4, new CardInstance("inst-4", "defend", CardInstance.BASE_LEVEL));
    givenDeck(all, List.of(), List.of());
    editor.open();
    assertTrue(last(previews).details().startsWith("Strike\n"));

    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);

    assertTrue(last(previews).details().startsWith("Defend\n"));
  }

  @Test
  void mouseWheelScrollsOnlyWhileOpen() {
    givenDeck(instances(repeat("strike", 12)), List.of(), List.of());

    stage.getRoot().fire(scrollEvent(1f));
    verify(poolFactory, never()).rebuildByTrigger(eq(SELECT_TRIGGER), anyList());

    editor.open();
    stage.getRoot().fire(scrollEvent(1f));

    assertEquals("inst-4", recordIds(lastCardRecords()).get(0));
  }

  // ---------------------------------------------------------------- set deck

  @Test
  void setDeckWithWrongCountShowsErrorAndDoesNotRearrange() {
    List<CardInstance> all = instances(repeat("strike", 6));
    givenDeck(all, all.subList(0, 4), List.of());
    editor.open();

    clickSetDeck();

    assertEquals("Hand must contain exactly 5 cards (currently 4).", last(errors));
    verify(cardPlayService, never()).rearrangeHand(any());
    verify(popup, never()).hide();
    assertTrue(deckChanges.isEmpty());
  }

  @Test
  void setDeckWithFiveCardsRearrangesNotifiesAndCloses() {
    List<CardInstance> all = instances(repeat("strike", 7));
    givenDeck(all, List.of(), List.of());
    editor.open();
    List<CardInstance> chosen = List.of(all.get(6), all.get(2), all.get(0), all.get(4), all.get(1));
    chosen.forEach(this::toggle);

    clickSetDeck();

    verify(cardPlayService).rearrangeHand(chosen);
    assertEquals(List.of(chosen), deckChanges);
    verify(popup).hide();
  }

  @Test
  void setDeckWithoutEnoughEnergyShowsErrorAndStaysOpen() {
    List<CardInstance> all = instances(repeat("strike", 5));
    givenDeck(all, all, List.of());
    when(cardPlayService.rearrangeHand(anyList()))
        .thenThrow(new IllegalStateException("Not enough energy"));
    editor.open();

    clickSetDeck();

    assertEquals("Not enough energy to rearrange the deck (needs 1).", last(errors));
    verify(popup, never()).hide();
    assertTrue(deckChanges.isEmpty());
  }

  @Test
  void setDeckWithNullCallbackStillCloses() {
    Table otherContent = new Table();
    when(popup.getContentTable()).thenReturn(otherContent);
    DeckEditorComponent noCallback = newEditor(null);
    new Entity().addComponent(noCallback).create();
    List<CardInstance> all = instances(repeat("strike", 5));
    givenDeck(all, all, List.of());
    noCallback.open();

    findTextButton(otherContent, "Set Deck").fire(new ChangeListener.ChangeEvent());

    verify(cardPlayService).rearrangeHand(all);
    verify(popup).hide();
  }

  // ---------------------------------------------------------------- close and dispose

  @Test
  void closingFiresClosedRemovesCardsAndHidesScrollButtons() {
    givenDeck(instances(repeat("strike", 12)), List.of(), List.of());
    editor.open();
    assertTrue(scrollDownBtn.isVisible());

    popup.hide();

    assertTrue(lifecycle.contains(DeckEditorEvents.CLOSED));
    verify(poolFactory).rebuildByTrigger(SELECT_TRIGGER, List.of());
    assertFalse(scrollUpBtn.isVisible());
    assertFalse(scrollDownBtn.isVisible());
  }

  @Test
  void reopeningResetsSelectionAndScroll() {
    List<CardInstance> all = instances(repeat("strike", 12));
    givenDeck(all, List.of(all.get(0)), List.of());
    editor.open();
    toggle(all.get(1));
    entity.getEvents().trigger(SCROLL_DOWN_TRIGGER);
    popup.hide();

    editor.open();

    assertEquals("inst-0", recordIds(lastCardRecords()).get(0));
    assertEquals("Selected (1): 1 Strike", last(summaries));
  }

  @Test
  void disposeClosesAndStopsListeningToMouseWheel() {
    givenDeck(instances(repeat("strike", 12)), List.of(), List.of());
    editor.open();

    editor.dispose();
    stage.getRoot().fire(scrollEvent(1f));

    assertTrue(lifecycle.contains(DeckEditorEvents.CLOSED));
    // Dispose clears the grid; a wheel scroll afterwards must not rebuild it.
    assertTrue(lastCardRecords().isEmpty());
  }

  // ---------------------------------------------------------------- helpers

  private DeckEditorComponent newEditor(java.util.function.Consumer<List<CardInstance>> callback) {
    return new DeckEditorComponent(
        cardPlayService,
        library,
        popup,
        poolFactory,
        mock(DisplayingFactory.class),
        mock(CardWidgetAssets.class),
        callback);
  }

  private void recordEvents(Entity target) {
    target.getEvents().addListener(DeckEditorEvents.ERROR, (String text) -> errors.add(text));
    target.getEvents().addListener(DeckEditorEvents.SUMMARY, (String text) -> summaries.add(text));
    target
        .getEvents()
        .addListener(
            DeckEditorEvents.PREVIEW,
            (CardPreviewDisplay.Content preview) -> previews.add(preview));
    target
        .getEvents()
        .addListener(
            DeckEditorEvents.BADGES, (List<CardBadgesDisplay.Badge> shown) -> badges.add(shown));
    for (String name :
        List.of(
            DeckEditorEvents.OPENED,
            DeckEditorEvents.CLOSED,
            DeckEditorEvents.TO_FRONT_BEHIND_CARDS,
            DeckEditorEvents.TO_FRONT)) {
      target.getEvents().addListener(name, () -> lifecycle.add(name));
    }
  }

  private static Object runIfSet(Runnable callback) {
    if (callback != null) {
      callback.run();
    }
    return null;
  }

  private static String[] repeat(String cardId, int count) {
    String[] ids = new String[count];
    Arrays.fill(ids, cardId);
    return ids;
  }

  private static List<CardInstance> instances(String... cardIds) {
    List<CardInstance> result = new ArrayList<>();
    for (int i = 0; i < cardIds.length; i++) {
      result.add(new CardInstance("inst-" + i, cardIds[i], CardInstance.BASE_LEVEL));
    }
    return result;
  }

  private void givenDeck(
      List<CardInstance> all, List<CardInstance> hand, List<CardInstance> discarded) {
    when(cardPlayService.allInstances()).thenReturn(all);
    when(cardPlayService.currentHand()).thenReturn(hand);
    when(cardPlayService.discardedInstances()).thenReturn(discarded);
  }

  private static Button buttonWithChild() {
    Button button = new Button();
    button.addActor(new Actor());
    return button;
  }

  private static Clickable clickable(Button btn, Object[] args) {
    Clickable clickable = mock(Clickable.class);
    when(clickable.getArgs()).thenReturn(args);
    when(clickable.getBtn()).thenReturn(btn);
    return clickable;
  }

  private static Clickable cardWidget(CardInstance instance) {
    return clickable(new Button(), new Object[] {instance.instanceId()});
  }

  private void givenCardWidgets(Clickable... widgets) {
    when(poolFactory.getByTrigger(SELECT_TRIGGER)).thenReturn(List.of(widgets));
  }

  private void toggle(CardInstance instance) {
    entity.getEvents().trigger(SELECT_TRIGGER, instance.instanceId());
  }

  private void clickSetDeck() {
    findTextButton(content, "Set Deck").fire(new ChangeListener.ChangeEvent());
  }

  @SuppressWarnings("unchecked")
  private List<ClickableRecord> lastCardRecords() {
    ArgumentCaptor<List<ClickableRecord>> captor = ArgumentCaptor.forClass(List.class);
    verify(poolFactory, atLeastOnce()).rebuildByTrigger(eq(SELECT_TRIGGER), captor.capture());
    return captor.getValue();
  }

  private static List<String> recordIds(List<ClickableRecord> records) {
    return records.stream().map(r -> String.valueOf(r.args()[0])).toList();
  }

  /** The badge expected for a selected card at the given index on the visible page. */
  private static CardBadgesDisplay.Badge badge(int number, int indexOnPage) {
    float cardX = GRID_ORIGIN_X + (indexOnPage % 4) * CARD_STEP_X;
    float cardTopY = GRID_TOP_Y - (indexOnPage / 4) * CARD_STEP_Y;
    return new CardBadgesDisplay.Badge(number, cardX + CARD_WIDTH, cardTopY);
  }

  private static List<CardBadgesDisplay.Badge> sortedByNumber(List<CardBadgesDisplay.Badge> shown) {
    return shown.stream().sorted((a, b) -> Integer.compare(a.number(), b.number())).toList();
  }

  private static void assertScrollButton(Button button, boolean enabled) {
    Color expectedTint = enabled ? Color.WHITE : SCROLL_DISABLED_TINT;
    assertEquals(enabled ? Touchable.enabled : Touchable.disabled, button.getTouchable());
    assertEquals(expectedTint, button.getColor());
    assertEquals(expectedTint, button.getChildren().first().getColor());
  }

  private static InputEvent enterEvent(int pointer) {
    InputEvent event = new InputEvent();
    event.setType(InputEvent.Type.enter);
    event.setPointer(pointer);
    return event;
  }

  private static InputEvent scrollEvent(float amountY) {
    InputEvent event = new InputEvent();
    event.setType(InputEvent.Type.scrolled);
    event.setScrollAmountY(amountY);
    return event;
  }

  private static <T> T last(List<T> items) {
    assertFalse(items.isEmpty(), "expected at least one event");
    return items.get(items.size() - 1);
  }

  private static Label findLabel(Table table, String text) {
    for (Actor actor : table.getChildren()) {
      if (actor instanceof Label label && text.contentEquals(label.getText())) {
        return label;
      }
      if (actor instanceof Table nested && !(actor instanceof Button)) {
        Label found = findLabel(nested, text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static TextButton findTextButton(Table table, String text) {
    for (Actor actor : table.getChildren()) {
      if (actor instanceof TextButton button && text.contentEquals(button.getText())) {
        return button;
      }
      if (actor instanceof Table nested && !(actor instanceof Button)) {
        TextButton found = findTextButton(nested, text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
