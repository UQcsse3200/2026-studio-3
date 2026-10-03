package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.spritedisplay.clickable.CardImageSkins;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
 * <p>Layout (left to right): a scrollable grid of cards, then a preview panel showing the
 * hovered/last-clicked card enlarged with its details underneath. "Set Deck" sits bottom-left.
 * Cards currently in the hand are listed first. Every selected card shows a numbered badge in its
 * top-right corner; the number is its position in the hand row (selection order).
 *
 * <p>Every card the player owns is tracked as a distinct {@link CardInstance} (see {@link
 * com.csse3200.game.cards.deck.PlayerDeck}), not just a card ID, so two copies of the same card
 * (e.g. two "strike" cards) are selected, highlighted and submitted independently.
 *
 * <p>The card toggles and the preview card are built with a dedicated {@link ClickableFactory}.
 * Static widgets (the scroll up/down buttons) come from JSON and are only repositioned, never
 * rebuilt. Because all of these are stage-level actors outside the popup window's actor hierarchy,
 * they are torn down on close and rebuilt on open (see {@link #onOpened()} / {@link #onClosed()}).
 *
 * <p>Scrolling is row-by-row (mouse wheel or the up/down buttons) rather than a pixel-smooth {@code
 * ScrollPane}: stage-level actors aren't clipped by a pane, so only whole rows are ever shown.
 */
public class DeckEditorComponent extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(DeckEditorComponent.class);

  private static final String SELECT_TRIGGER = "toggleDeckCard";
  private static final String PREVIEW_TRIGGER =
      "deckPreview"; // nothing listens: click does nothing
  private static final String SCROLL_UP_TRIGGER = "deckScrollUp"; // defined in DeckEditorUi.json
  private static final String SCROLL_DOWN_TRIGGER =
      "deckScrollDown"; // defined in DeckEditorUi.json

  /** Smallest hand "Set Deck" will accept. Selecting more than this is allowed. */
  private static final int MIN_HAND_SIZE = 5;

  private static final float CARD_WIDTH = 100f;
  private static final float CARD_HEIGHT = 145f;
  private static final float GRID_GAP = 14f;
  private static final int COLUMNS = 4;
  private static final int ROWS_VISIBLE = 2;
  private static final int PAGE_SIZE = COLUMNS * ROWS_VISIBLE;

  private static final float SCROLL_BUTTON_SIZE = 40f;
  private static final float SCROLL_GAP = 10f;

  // Fixed insets from the window's own top-left corner to the grid's top-left corner, chosen to
  // clear the title bar/close button and leave room for the scroll buttons just left of the grid,
  // without needing to introspect Window's internal padding.
  private static final float GRID_LEFT_INSET = SCROLL_BUTTON_SIZE + SCROLL_GAP + 40f;
  private static final float GRID_TOP_INSET = 70f;

  // Preview panel, to the right of the grid and top-aligned with it.
  private static final float PREVIEW_GAP = 30f;
  private static final float PREVIEW_PANEL_WIDTH = 190f;
  private static final float PREVIEW_CARD_WIDTH = 140f;
  private static final float PREVIEW_CARD_HEIGHT = 203f;
  private static final float PREVIEW_INFO_GAP = 10f;

  private static final float BADGE_SIZE = 24f;
  private static final Color BADGE_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.9f);

  private static final Color DISCARDED_TINT = new Color(0.35f, 0.35f, 0.35f, 1f);
  private static final Color SELECTED_TINT = new Color(0.55f, 0.85f, 1f, 1f);
  // Both apply (a card that's on cooldown AND selected as a reserved slot): overlay both colours
  // rather than picking one, so it reads as neither "just discarded" nor "just selected".
  private static final Color SELECTED_DISCARDED_TINT =
      new Color(DISCARDED_TINT).lerp(SELECTED_TINT, 0.5f);

  private final CardPlayService cardPlayService;
  private final CardLibrary library;
  private final PopupDisplay popup;
  private final ClickableFactory poolFactory;
  private final Consumer<List<CardInstance>> onDeckChanged;

  // Insertion-ordered: the iteration order IS the hand-row order and the badge numbers.
  private final Set<CardInstance> selected = new LinkedHashSet<>();
  private final Map<String, CardInstance> poolByKey = new HashMap<>();
  private Set<CardInstance> discardedInstances = new HashSet<>();
  private List<CardInstance> pool = new ArrayList<>();
  private int scrollRow;
  private boolean open;
  private CardInstance previewed;

  private Label summaryLabel;
  private Label errorLabel;
  private Label previewInfo;
  private Label.LabelStyle badgeStyle;
  private final List<Label> badges = new ArrayList<>();
  private Cell<Actor> gridSpacerCell;

  // Window-anchored layout, recomputed from the popup's live bounds whenever widgets are built.
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

  /**
   * @param cardPlayService source of the player's card pool/hand and the rearrange operation
   * @param library card definitions, for names/art
   * @param popup the popup window this editor renders into
   * @param poolFactory a {@link ClickableFactory} dedicated to this editor's widgets (must not be
   *     shared with any other widget group, e.g. the main hand row). Built from {@code
   *     sprites/DeckEditorUi.json} so it already contains the scroll up/down buttons.
   * @param onDeckChanged callback run after a successful "Set Deck", passed the full confirmed
   *     selection (including any still-on-cooldown picks) — so the caller can refresh whatever else
   *     displays the hand (e.g. the on-screen hand row), which needs the whole picture even though
   *     {@link CardPlayService#rearrangeHand} itself only moves the currently-playable subset
   */
  public DeckEditorComponent(
      CardPlayService cardPlayService,
      CardLibrary library,
      PopupDisplay popup,
      ClickableFactory poolFactory,
      Consumer<List<CardInstance>> onDeckChanged) {
    this.cardPlayService = cardPlayService;
    this.library = library;
    this.popup = popup;
    this.poolFactory = poolFactory;
    this.onDeckChanged = onDeckChanged;
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(SELECT_TRIGGER, this::toggleSelection);
    entity.getEvents().addListener(SCROLL_UP_TRIGGER, () -> scrollBy(-1));
    entity.getEvents().addListener(SCROLL_DOWN_TRIGGER, () -> scrollBy(1));
    popup.setOnShow(this::onOpened);
    popup.setOnHide(this::onClosed);
    popup.setOnWindowClicked(this::bringEditorWidgetsToFront);
    buildFooter();
    buildStageActors();
    stage.addListener(wheelListener);
    // The factory's create() has already run (it's added to the entity first), so the JSON-defined
    // scroll buttons exist; keep them hidden until the popup opens.
    setScrollButtonsVisible(false);
  }

  /** Opens the popup with the pool/selection refreshed from the current battle deck state. */
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

    summaryLabel = new Label("", skin);
    summaryLabel.setWrap(true);

    TextButton setDeckButton = new TextButton("Set Deck", skin);
    setDeckButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            onSetDeckClicked();
          }
        });

    // "Set Deck" bottom-left, with the selection summary to its right.
    content.add(setDeckButton).size(140f, 48f).padTop(10f).padRight(20f);
    content.add(summaryLabel).width(520f).padTop(10f);
    content.row();

    errorLabel = new Label("", skin);
    errorLabel.setWrap(true);
    errorLabel.setColor(Color.SALMON);
    content.add(errorLabel).colspan(2).width(660f).padTop(6f);
  }

  /** Creates the stage-level actors that aren't ClickableRecords: preview text + badge style. */
  private void buildStageActors() {
    previewInfo = new Label("", skin);
    previewInfo.setWrap(true);
    previewInfo.setAlignment(Align.topLeft);
    previewInfo.setSize(PREVIEW_PANEL_WIDTH, previewInfoHeight());
    previewInfo.setTouchable(Touchable.disabled);
    previewInfo.setVisible(false);
    stage.addActor(previewInfo);

    badgeStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    badgeStyle.fontColor = Color.WHITE;
    if (skin.has("white", Drawable.class)) {
      badgeStyle.background = skin.newDrawable("white", BADGE_COLOR);
    }
  }

  private void refreshPoolAndSelection() {
    List<CardInstance> hand = cardPlayService.currentHand();

    // Cards currently in the hand come first (in hand order), then everything else in the
    // service's own order. The set de-duplicates the hand cards that also appear in allInstances().
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

  // ---- scrolling -------------------------------------------------------------------------

  private int totalRows() {
    return pool.isEmpty() ? 1 : ((pool.size() - 1) / COLUMNS) + 1;
  }

  private int maxScrollRow() {
    return Math.max(0, totalRows() - ROWS_VISIBLE);
  }

  private void scrollBy(int rows) {
    int target = Math.max(0, Math.min(scrollRow + rows, maxScrollRow()));
    if (target == scrollRow) {
      return;
    }
    scrollRow = target;
    buildGridWidgets();
  }

  // ---- layout ----------------------------------------------------------------------------

  private float gridWidth() {
    return COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GRID_GAP;
  }

  private float gridHeight() {
    return ROWS_VISIBLE * CARD_HEIGHT + (ROWS_VISIBLE - 1) * GRID_GAP;
  }

  private float cardX(int indexOnPage) {
    return gridOriginX + (indexOnPage % COLUMNS) * (CARD_WIDTH + GRID_GAP);
  }

  /** Bottom edge of a grid slot, in stage coordinates (y up). */
  private float cardBottomY(int indexOnPage) {
    int row = indexOnPage / COLUMNS;
    return gridTopY - row * (CARD_HEIGHT + GRID_GAP) - CARD_HEIGHT;
  }

  private float previewPanelX() {
    return gridOriginX + gridWidth() + PREVIEW_GAP;
  }

  private float previewCardX() {
    return previewPanelX() + (PREVIEW_PANEL_WIDTH - PREVIEW_CARD_WIDTH) / 2f;
  }

  private float previewCardBottomY() {
    return gridTopY - PREVIEW_CARD_HEIGHT;
  }

  private float previewInfoHeight() {
    return gridHeight() - PREVIEW_CARD_HEIGHT - PREVIEW_INFO_GAP;
  }

  private void anchorToWindow() {
    float windowX = popup.getWindowX();
    float windowTopY = popup.getWindowY() + popup.getWindowHeight();
    stageHeight = stage.getViewport().getWorldHeight();
    gridOriginX = windowX + GRID_LEFT_INSET;
    gridTopY = windowTopY - GRID_TOP_INSET;
  }

  /** Reserves room in the window's content table for the grid + preview panel (stage actors). */
  private void updateGridSpacer() {
    gridSpacerCell
        .width(GRID_LEFT_INSET + gridWidth() + PREVIEW_GAP + PREVIEW_PANEL_WIDTH)
        .height(gridHeight());
  }

  // ---- open / close ----------------------------------------------------------------------

  private void onOpened() {
    open = true;
    buildGridWidgets();
    showPreview(pool.isEmpty() ? null : pool.get(0));
    refreshSummary();
    errorLabel.setText("");
    bringEditorWidgetsToFront();
  }

  private void onClosed() {
    open = false;
    poolFactory.rebuildByTrigger(SELECT_TRIGGER, List.of());
    poolFactory.rebuildByTrigger(PREVIEW_TRIGGER, List.of());
    setScrollButtonsVisible(false);
    clearBadges();
    previewInfo.setVisible(false);
    previewed = null;
  }

  /**
   * A libGDX Window calls toFront() on itself on every touch inside it, which would bury all our
   * stage-level siblings — so re-assert their order (cards, preview, badges, text) on top.
   */
  private void bringEditorWidgetsToFront() {
    poolFactory.bringToFront();
    for (Label badge : badges) {
      badge.toFront();
    }
    if (previewInfo != null) {
      previewInfo.toFront();
    }
  }

  // ---- building widgets ------------------------------------------------------------------

  /**
   * Rebuilds the visible rows of cards for {@link #scrollRow}, repositions the (JSON-defined)
   * scroll buttons, and refreshes highlights/badges. Called on open and on every scroll.
   */
  private void buildGridWidgets() {
    scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow()));
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

      // Discarded (on-cooldown) cards stay selectable here — see rearrangeHand's javadoc — so this
      // deliberately never sets ClickableRecord.disabled(true); discarded/selected state is shown
      // entirely through applySelectionHighlights' tinting instead.
      ClickableRecord record =
          ClickableRecord.builder(SELECT_TRIGGER)
              .label(card.name)
              .position(cardX(indexOnPage), stageHeight - cardBottomY(indexOnPage))
              .size(CARD_WIDTH, CARD_HEIGHT)
              .skin(cardSkin)
              .args(instance.instanceId())
              .build();
      cardRecords.add(record);
    }

    poolFactory.rebuildByTrigger(SELECT_TRIGGER, cardRecords);
    attachHoverPreview();
    layoutScrollButtons();
    applySelectionHighlights();
  }

  /** Hovering a card shows it in the preview panel. Listeners are added once per (re)build. */
  private void attachHoverPreview() {
    for (Clickable widget : poolFactory.getByTrigger(SELECT_TRIGGER)) {
      Object[] args = widget.getArgs();
      if (args.length == 0) {
        continue;
      }
      CardInstance instance = poolByKey.get(String.valueOf(args[0]));
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
                  if (pointer == -1) { // mouse hover only
                    showPreview(instance);
                  }
                }
              });
    }
  }

  private void layoutScrollButtons() {
    float x = gridOriginX - SCROLL_BUTTON_SIZE - SCROLL_GAP;
    placeScrollButton(SCROLL_UP_TRIGGER, x, gridTopY - SCROLL_BUTTON_SIZE, scrollRow <= 0);
    placeScrollButton(SCROLL_DOWN_TRIGGER, x, gridTopY - gridHeight(), scrollRow >= maxScrollRow());
  }

  private void placeScrollButton(String trigger, float x, float bottomY, boolean disabled) {
    for (Clickable widget : poolFactory.getByTrigger(trigger)) {
      Button btn = widget.getBtn();
      btn.setPosition(x, bottomY); // stage coords, so no stageHeight flip like ClickableRecord
      btn.setDisabled(disabled);
      btn.setVisible(maxScrollRow() > 0); // nothing to scroll -> no buttons
    }
  }

  private void setScrollButtonsVisible(boolean visible) {
    for (String trigger : List.of(SCROLL_UP_TRIGGER, SCROLL_DOWN_TRIGGER)) {
      for (Clickable widget : poolFactory.getByTrigger(trigger)) {
        widget.getBtn().setVisible(visible);
      }
    }
  }

  // ---- preview panel ---------------------------------------------------------------------

  private void showPreview(CardInstance instance) {
    if (instance != null && instance.equals(previewed)) {
      return;
    }
    Optional<CardConfig> maybeCard =
        instance == null ? Optional.empty() : library.getCard(instance.cardId());
    if (maybeCard.isEmpty()) {
      previewed = null;
      poolFactory.rebuildByTrigger(PREVIEW_TRIGGER, List.of());
      previewInfo.setVisible(false);
      return;
    }
    previewed = instance;
    CardConfig card = maybeCard.get();

    ClickableRecord record =
        ClickableRecord.builder(PREVIEW_TRIGGER)
            .label(card.name)
            .position(previewCardX(), stageHeight - previewCardBottomY())
            .size(PREVIEW_CARD_WIDTH, PREVIEW_CARD_HEIGHT)
            .skin(CardImageSkins.forTexturePath(card.texturePath))
            .build();
    poolFactory.rebuildByTrigger(PREVIEW_TRIGGER, List.of(record));

    previewInfo.setText(previewText(card));
    previewInfo.setPosition(
        previewPanelX(), previewCardBottomY() - PREVIEW_INFO_GAP - previewInfoHeight());
    previewInfo.setVisible(true);
    bringEditorWidgetsToFront();
  }

  private String previewText(CardConfig card) {
    StringBuilder text = new StringBuilder(card.name);
    text.append("\nTarget: ").append(card.target);
    // TODO: show the card's stats here (energy cost, damage/block/effect values, description).
    //  I only know CardConfig's name/texturePath/target fields; add the rest once they're known
    //  (or pull the numbers from the card's effect definitions).
    return text.toString();
  }

  // ---- selection / highlights / badges ---------------------------------------------------

  private void applySelectionHighlights() {
    for (Clickable widget : poolFactory.getByTrigger(SELECT_TRIGGER)) {
      Object[] args = widget.getArgs();
      if (args.length == 0) {
        continue;
      }
      CardInstance instance = poolByKey.get(String.valueOf(args[0]));
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

  /**
   * Draws a numbered badge on the top-right corner of every visible selected card. The number is
   * the card's position in {@link #selected}'s iteration order, i.e. its slot in the hand row — so
   * deselecting a card renumbers the ones after it.
   */
  private void rebuildBadges() {
    clearBadges();
    Map<CardInstance, Integer> orderOf = new HashMap<>();
    int next = 1;
    for (CardInstance instance : selected) {
      orderOf.put(instance, next++);
    }

    int start = scrollRow * COLUMNS;
    int end = Math.min(pool.size(), start + PAGE_SIZE);
    for (int i = start; i < end; i++) {
      Integer order = orderOf.get(pool.get(i));
      if (order == null) {
        continue;
      }
      int indexOnPage = i - start;
      Label badge = new Label(String.valueOf(order), badgeStyle);
      badge.setAlignment(Align.center);
      badge.setSize(BADGE_SIZE, BADGE_SIZE);
      // Straddles the card's top-right corner, like the sketch.
      badge.setPosition(
          cardX(indexOnPage) + CARD_WIDTH - BADGE_SIZE * 0.75f,
          cardBottomY(indexOnPage) + CARD_HEIGHT - BADGE_SIZE * 0.75f);
      badge.setTouchable(Touchable.disabled); // clicks fall through to the card underneath
      stage.addActor(badge);
      badges.add(badge);
    }
  }

  private void clearBadges() {
    for (Label badge : badges) {
      badge.remove();
    }
    badges.clear();
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
    // No upper limit: any number of cards can be picked.
    if (!selected.remove(instance)) {
      selected.add(instance);
    }
    showPreview(instance);
    applySelectionHighlights();
    refreshSummary();
    errorLabel.setText("");
  }

  private void onSetDeckClicked() {
    if (selected.size() < MIN_HAND_SIZE) {
      errorLabel.setText("Pick at least " + MIN_HAND_SIZE + " cards.");
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
      errorLabel.setText("Not enough energy to rearrange the deck (needs 1).");
    }
  }

  private void refreshSummary() {
    summaryLabel.setText(summaryText());
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
    clearBadges();
    if (previewInfo != null) {
      previewInfo.remove();
    }
    super.dispose();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage; nothing to do per-frame here.
  }
}
