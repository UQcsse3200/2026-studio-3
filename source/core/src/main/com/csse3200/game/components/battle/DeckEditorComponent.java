package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.spritedisplay.clickable.CardImageSkins;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.displaying.CardBadgesDisplay;
import com.csse3200.game.components.spritedisplay.displaying.CardFramesDisplay;
import com.csse3200.game.components.spritedisplay.displaying.CardPreviewDisplay;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A popup UI (hosted in a {@link PopupDisplay}) that lets the player rearrange their battle hand
 * from their whole card pool — draw pile, hand and discard pile combined — by toggling cards on and
 * off, then confirming with "Set Deck".
 *
 * <p>Layout (left to right): a scrollable grid of cards, a narrow gap containing the vertical
 * scroll up/down buttons, then a preview panel showing the hovered/last-clicked card enlarged with
 * its details underneath. "Set Deck" sits bottom-left. Cards currently in the hand are listed
 * first. Every selected card shows a numbered badge in its top-right corner; the number is its
 * position in the hand row (selection order). Each grid card sits in a brown-bordered, light-brown
 * frame (with its name along the bottom) that matches the preview panel.
 *
 * <p>The scroll buttons' definitions come from a dedicated {@code DeckEditorUi.json} — separate
 * from {@code BattleUi.json} — and are created by this component's own {@link ClickableFactory}.
 * Their visibility and interactivity are toggled in place (never rebuilt) so the JSON-provided
 * coordinates are honoured.
 *
 * <p>Everything display-only (preview panel, preview, summary, error, badges, card frames) is a
 * {@code Displaying} variant on this entity driven through {@link DeckEditorEvents}; this class
 * only pushes data to them.
 */
public class DeckEditorComponent extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(DeckEditorComponent.class);

  private static final String SELECT_TRIGGER = "toggleDeckCard";
  private static final String SCROLL_UP_TRIGGER = "deckScrollUp";
  private static final String SCROLL_DOWN_TRIGGER = "deckScrollDown";
  private static final String POPUP_STYLE = "popup";

  private static final float CARD_WIDTH = 100f;
  private static final float CARD_HEIGHT = 145f;
  private static final float GRID_GAP = 14f;
  private static final int COLUMNS = 4;
  private static final int ROWS_VISIBLE = 2;
  private static final int PAGE_SIZE = COLUMNS * ROWS_VISIBLE;

  private static final float GRID_LEFT_INSET = 40f;
  private static final float GRID_TOP_INSET = 70f;

  // Widened so the scroll buttons (40px) fit cleanly between the grid and the preview panel.
  private static final float PREVIEW_GAP = 60f;
  private static final float PREVIEW_PANEL_WIDTH = 230f;

  // Extra room reserved under the grid in the window's content table (so the footer clears the
  // card name strip along the bottom of the frames).
  private static final float CARD_LABEL_HEIGHT = 22f;

  // How far each card's frame extends past its slot on every side. Keep it under half of
  // GRID_GAP (7px) so neighbouring frames don't touch.
  private static final float FRAME_PAD = 4f;

  private static final Color DISCARDED_TINT = new Color(0.35f, 0.35f, 0.35f, 1f);
  private static final Color SELECTED_TINT = new Color(1f, 0.85f, 0.5f, 1f);
  private static final Color SELECTED_DISCARDED_TINT =
      new Color(DISCARDED_TINT).lerp(SELECTED_TINT, 0.5f);
  private static final Color SCROLL_DISABLED_TINT = new Color(0.5f, 0.5f, 0.5f, 1f);

  private static final int HAND_SIZE = 5;

  private final CardPlayService cardPlayService;
  private final CardLibrary library;
  private final PopupDisplay popup;
  private final ClickableFactory poolFactory;
  private final Consumer<List<CardInstance>> onDeckChanged;

  private final Set<CardInstance> selected = new LinkedHashSet<>();
  private final Map<String, CardInstance> poolByKey = new HashMap<>();
  private Set<CardInstance> discardedInstances = new HashSet<>();
  private List<CardInstance> pool = new ArrayList<>();
  private int scrollRow;
  private boolean open;
  private CardInstance previewed;

  private Cell<Actor> gridSpacerCell;

  private float gridOriginX;
  private float gridTopY;
  private float stageHeight;

  private final InputListener wheelListener =
      new InputListener() {
        @Override
        public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
          if (!open || amountY == 0f) {
            return false;
          }
          scrollBy((int) Math.signum(amountY));
          return true;
        }
      };

  public DeckEditorComponent(
      CardPlayService cardPlayService,
      CardLibrary library,
      PopupDisplay popup,
      ClickableFactory poolFactory,
      DisplayingFactory displayFactory,
      Consumer<List<CardInstance>> onDeckChanged) {
    this.cardPlayService = cardPlayService;
    this.library = library;
    this.popup = popup;
    this.poolFactory = poolFactory;
    this.onDeckChanged = onDeckChanged;
    Objects.requireNonNull(displayFactory, "displayFactory must be attached to the same entity");
  }

  @Override
  public void create() {
    super.create();
    popup.setHeaderColour(Color.valueOf("0f0a09"));
    entity.getEvents().addListener(SELECT_TRIGGER, this::toggleSelection);
    entity.getEvents().addListener(SCROLL_UP_TRIGGER, () -> scrollBy(-1));
    entity.getEvents().addListener(SCROLL_DOWN_TRIGGER, () -> scrollBy(1));
    popup.setOnShow(this::onOpened);
    popup.setOnHide(this::onClosed);
    popup.setOnWindowClicked(this::bringEditorWidgetsToFront);
    buildFooter();
    stage.addListener(wheelListener);

    // Buttons default to visible=true in libGDX; hide them now since the popup starts closed.
    rebuildScrollButtons();
  }

  public void open() {
    refreshPoolAndSelection();
    updateGridSpacer();
    popup.show();
  }

  private void buildFooter() {
    Table content = popup.getContentTable();

    Actor gridSpacer = new Actor();
    gridSpacerCell = content.add(gridSpacer).colspan(2);
    content.row();

    TextButton setDeckButton = new TextButton("Set Deck", skin, POPUP_STYLE);
    setDeckButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            onSetDeckClicked();
          }
        });

    content.add(setDeckButton).size(140f, 48f).padTop(152f).padRight(20f);
    Actor summarySpacer = new Actor();
    content.add(summarySpacer).width(520f).padTop(10f);
    content.row();

    Actor errorSpacer = new Actor();
    content.add(errorSpacer).colspan(2).width(660f).padTop(6f);
  }

  private void refreshPoolAndSelection() {
    List<CardInstance> hand = cardPlayService.currentHand();

    LinkedHashSet<CardInstance> ordered = new LinkedHashSet<>(hand);
    ordered.addAll(cardPlayService.allInstances());
    pool = new ArrayList<>(ordered);

    poolByKey.clear();
    for (CardInstance instance : pool) {
      poolByKey.put(instance.instanceId(), instance);
    }
    discardedInstances = new HashSet<>(cardPlayService.discardedInstances());
    selected.clear();
    selected.addAll(hand);
    scrollRow = 0;
    previewed = null;
  }

  private int totalRows() {
    return pool.isEmpty() ? 1 : ((pool.size() - 1) / COLUMNS) + 1;
  }

  private int maxScrollRow() {
    return Math.max(0, totalRows() - ROWS_VISIBLE);
  }

  private void scrollBy(int rows) {
    int target = (int) Math.clamp((long) scrollRow + rows, 0L, maxScrollRow());
    if (target == scrollRow) {
      return;
    }
    scrollRow = target;
    buildGridWidgets();
  }

  private float gridWidth() {
    return COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GRID_GAP;
  }

  private float gridHeight() {
    return ROWS_VISIBLE * CARD_HEIGHT + (ROWS_VISIBLE - 1) * GRID_GAP;
  }

  private float cardX(int indexOnPage) {
    return gridOriginX + (indexOnPage % COLUMNS) * (CARD_WIDTH + GRID_GAP);
  }

  private float cardBottomY(int indexOnPage) {
    int row = indexOnPage / COLUMNS;
    return gridTopY - row * (CARD_HEIGHT + GRID_GAP) - CARD_HEIGHT;
  }

  private void anchorToWindow() {
    float windowX = popup.getWindowX();
    float windowTopY = popup.getWindowY() + popup.getWindowHeight();
    stageHeight = stage.getViewport().getWorldHeight();
    gridOriginX = windowX + GRID_LEFT_INSET;
    gridTopY = windowTopY - GRID_TOP_INSET;
  }

  private void updateGridSpacer() {
    gridSpacerCell
        .width(GRID_LEFT_INSET + gridWidth() + PREVIEW_GAP + PREVIEW_PANEL_WIDTH)
        .height(gridHeight() + CARD_LABEL_HEIGHT);
  }

  private void onOpened() {
    open = true;
    entity.getEvents().trigger(DeckEditorEvents.OPENED);
    buildGridWidgets();
    refreshSummary();
    entity.getEvents().trigger(DeckEditorEvents.ERROR, "");
    bringEditorWidgetsToFront();
  }

  private void onClosed() {
    open = false;
    entity.getEvents().trigger(DeckEditorEvents.CLOSED); // clears frames, badges, preview, text
    poolFactory.rebuildByTrigger(SELECT_TRIGGER, List.of());
    rebuildScrollButtons();
    previewed = null;
  }

  /**
   * A libGDX Window calls toFront() on itself on every touch inside it, which would bury all our
   * stage-level siblings — so re-assert their order, back to front: the card frames, then the card
   * (and scroll) buttons, then everything that draws over the cards (preview, badges, text).
   */
  private void bringEditorWidgetsToFront() {
    entity.getEvents().trigger(DeckEditorEvents.TO_FRONT_BEHIND_CARDS);
    poolFactory.bringToFront();
    entity.getEvents().trigger(DeckEditorEvents.TO_FRONT);
  }

  private void buildGridWidgets() {
    scrollRow = (int) Math.clamp(scrollRow, 0L, maxScrollRow());
    anchorToWindow();

    int start = scrollRow * COLUMNS;
    int end = Math.min(pool.size(), start + PAGE_SIZE);

    List<ClickableRecord> cardRecords = new ArrayList<>();
    for (int i = start; i < end; i++) {
      CardInstance instance = pool.get(i);
      Optional<CardConfig> maybeCard = library.getCard(instance.cardId());
      if (maybeCard.isEmpty()) {
        logger.warn("Card ID {} not found in library, skipping", instance.cardId());
        continue;
      }
      CardConfig card = maybeCard.get();

      int indexOnPage = i - start;
      Skin cardSkin = CardImageSkins.forTexturePath(card.texturePath);

      cardRecords.add(
          ClickableRecord.builder(SELECT_TRIGGER)
              .label(card.name)
              .position(cardX(indexOnPage), stageHeight - cardBottomY(indexOnPage) - 10f)
              .size(CARD_WIDTH, CARD_HEIGHT)
              .skin(cardSkin)
              .args(instance.instanceId())
              .build());
    }

    // Frames first, so the card buttons built next are added on top of them.
    publishCardFrames(start, end);
    poolFactory.rebuildByTrigger(SELECT_TRIGGER, cardRecords);
    attachHoverPreview();
    rebuildScrollButtons();
    applySelectionHighlights();

    CardInstance target = previewed;
    if (target == null || !pageContains(target, start, end)) {
      target = start < end ? pool.get(start) : null;
    }
    showPreview(target);
  }

  /**
   * Pushes one frame per visible card into {@link CardFramesDisplay}: the card's slot grown by
   * {@link #FRAME_PAD} on every side, with the card's name drawn along its bottom edge.
   */
  private void publishCardFrames(int start, int end) {
    List<CardFramesDisplay.Frame> frames = new ArrayList<>();
    for (int i = start; i < end; i++) {
      Optional<CardConfig> maybeCard = library.getCard(pool.get(i).cardId());
      if (maybeCard.isEmpty()) {
        continue;
      }
      int indexOnPage = i - start;
      frames.add(
          new CardFramesDisplay.Frame(
              maybeCard.get().name,
              cardX(indexOnPage) - FRAME_PAD,
              cardBottomY(indexOnPage) - FRAME_PAD,
              CARD_WIDTH + 2 * FRAME_PAD,
              CARD_HEIGHT + 2 * FRAME_PAD));
    }
    entity.getEvents().trigger(DeckEditorEvents.FRAMES, frames);
  }

  private boolean pageContains(CardInstance instance, int start, int end) {
    for (int i = start; i < end; i++) {
      if (pool.get(i).equals(instance)) {
        return true;
      }
    }
    return false;
  }

  private void attachHoverPreview() {
    for (Clickable widget : poolFactory.getByTrigger(SELECT_TRIGGER)) {
      Object[] args = widget.getArgs();
      CardInstance instance = args.length == 0 ? null : poolByKey.get(String.valueOf(args[0]));
      if (instance == null) {
        continue;
      }
      widget
          .getBtn()
          .addListener(
              new InputListener() {
                @Override
                public void enter(
                    InputEvent event, float x, float y, int pointer, Actor fromActor) {
                  if (pointer == -1) {
                    showPreview(instance);
                  }
                }
              });
    }
  }

  private void rebuildScrollButtons() {
    boolean shouldShow = open && maxScrollRow() > 0;
    boolean canScrollUp = shouldShow && scrollRow > 0;
    boolean canScrollDown = shouldShow && scrollRow < maxScrollRow();

    setScrollButtonState(SCROLL_UP_TRIGGER, shouldShow, canScrollUp);
    setScrollButtonState(SCROLL_DOWN_TRIGGER, shouldShow, canScrollDown);
  }

  private void setScrollButtonState(String trigger, boolean visible, boolean enabled) {
    for (Clickable clickable : poolFactory.getByTrigger(trigger)) {
      Button b = clickable.getBtn();
      b.setVisible(visible);
      b.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
      Color tint = enabled ? Color.WHITE : SCROLL_DISABLED_TINT;
      b.setColor(tint);
      for (Actor child : b.getChildren()) {
        child.setColor(tint);
      }
    }
  }

  private void showPreview(CardInstance instance) {
    previewed = instance;
    if (instance == null) {
      entity.getEvents().trigger(DeckEditorEvents.PREVIEW, CardPreviewDisplay.Content.NONE);
      return;
    }
    Optional<CardConfig> maybeCard = library.getCard(instance.cardId());
    if (maybeCard.isEmpty()) {
      entity.getEvents().trigger(DeckEditorEvents.PREVIEW, CardPreviewDisplay.Content.NONE);
      return;
    }
    CardConfig card = maybeCard.get();
    entity
        .getEvents()
        .trigger(
            DeckEditorEvents.PREVIEW,
            new CardPreviewDisplay.Content(card.texturePath, previewText(card)));
  }

  private String previewText(CardConfig card) {
    StringBuilder text = new StringBuilder(card.name);
    text.append("\nTarget: ").append(card.target);
    return text.toString();
  }

  private void applySelectionHighlights() {
    for (Clickable widget : poolFactory.getByTrigger(SELECT_TRIGGER)) {
      Object[] args = widget.getArgs();
      CardInstance instance = args.length == 0 ? null : poolByKey.get(String.valueOf(args[0]));
      if (instance == null) {
        continue;
      }
      Color tint = tintFor(discardedInstances.contains(instance), selected.contains(instance));
      Button btn = widget.getBtn();
      btn.setColor(tint);
      for (Actor child : btn.getChildren()) {
        child.setColor(tint);
      }
    }
    rebuildBadges();
  }

  private void rebuildBadges() {
    Map<CardInstance, Integer> orderOf = new HashMap<>();
    int next = 1;
    for (CardInstance instance : selected) {
      orderOf.put(instance, next++);
    }

    int start = scrollRow * COLUMNS;
    int end = Math.min(pool.size(), start + PAGE_SIZE);
    List<CardBadgesDisplay.Badge> newBadges = new ArrayList<>();
    for (int i = start; i < end; i++) {
      Integer order = orderOf.get(pool.get(i));
      if (order == null) {
        continue;
      }
      int indexOnPage = i - start;
      newBadges.add(
          new CardBadgesDisplay.Badge(
              order, cardX(indexOnPage) + CARD_WIDTH, cardBottomY(indexOnPage) + CARD_HEIGHT));
    }
    entity.getEvents().trigger(DeckEditorEvents.BADGES, newBadges);
  }

  private static Color tintFor(boolean discarded, boolean selected) {
    if (discarded && selected) {
      return SELECTED_DISCARDED_TINT;
    }
    if (discarded) {
      return DISCARDED_TINT;
    }
    if (selected) {
      return SELECTED_TINT;
    }
    return Color.WHITE;
  }

  private void toggleSelection(String instanceKey) {
    CardInstance instance = poolByKey.get(instanceKey);
    if (instance == null) {
      return;
    }
    if (selected.remove(instance)) {
      // deselecting always allowed
    } else {
      if (selected.size() >= HAND_SIZE) {
        entity
            .getEvents()
            .trigger(
                DeckEditorEvents.ERROR, "Hand is full, deselect a card before adding another.");
        return;
      }
      selected.add(instance);
    }
    showPreview(instance);
    applySelectionHighlights();
    refreshSummary();
    entity.getEvents().trigger(DeckEditorEvents.ERROR, "");
  }

  private void onSetDeckClicked() {
    if (selected.size() != HAND_SIZE) {
      entity
          .getEvents()
          .trigger(
              DeckEditorEvents.ERROR,
              "Hand must contain exactly "
                  + HAND_SIZE
                  + " cards (currently "
                  + selected.size()
                  + ").");
      return;
    }
    List<CardInstance> confirmedSelection = new ArrayList<>(selected);
    try {
      cardPlayService.rearrangeHand(confirmedSelection);
      if (onDeckChanged != null) {
        onDeckChanged.accept(confirmedSelection);
      }
      popup.hide();
    } catch (IllegalStateException notEnoughEnergy) {
      entity
          .getEvents()
          .trigger(DeckEditorEvents.ERROR, "Not enough energy to rearrange the deck (needs 1).");
    }
  }

  private void refreshSummary() {
    entity.getEvents().trigger(DeckEditorEvents.SUMMARY, summaryText());
  }

  private String summaryText() {
    StringBuilder text = new StringBuilder("Selected (" + selected.size() + "): ");
    int number = 1;
    for (CardInstance instance : selected) {
      if (number > 1) {
        text.append(", ");
      }
      text.append(number++)
          .append(' ')
          .append(library.getCard(instance.cardId()).map(c -> c.name).orElse(instance.cardId()));
    }
    return text.toString();
  }

  @Override
  public void dispose() {
    stage.removeListener(wheelListener);
    super.dispose();
  }

  @Override
  protected void draw(SpriteBatch batch) {
      //drawn by BattleScreen
  }
}
