package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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
 * <p>Every card the player owns is tracked as a distinct {@link CardInstance} (see {@link
 * com.csse3200.game.cards.deck.PlayerDeck}), not just a card ID, so two copies of the same card
 * (e.g. two "strike" cards) are selected, highlighted and submitted independently — selecting one
 * never affects the other, even though they look identical.
 *
 * <p>The per-card toggle buttons are built with a dedicated {@link ClickableFactory} (per the
 * project's declarative-widget convention), positioned in a grid anchored to the popup window's
 * actual on-screen bounds. Because those buttons are stage-level actors outside the popup window's
 * own actor hierarchy, they are torn down on close and rebuilt on open (see {@link #onOpened()} /
 * {@link #onClosed()}) rather than living inside the window's content table like the summary label
 * and "Set Deck" button do.
 */
public class DeckEditorComponent extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(DeckEditorComponent.class);

  private static final String SELECT_TRIGGER = "toggleDeckCard";
  private static final String PREV_PAGE_TRIGGER = "deckPagePrev";
  private static final String NEXT_PAGE_TRIGGER = "deckPageNext";
  private static final int REQUIRED_HAND_SIZE = 5;

  private static final float CARD_WIDTH = 90f;
  private static final float CARD_HEIGHT = 130f;
  private static final float GRID_GAP = 14f;
  private static final int COLUMNS = 5;
  private static final int ROWS_PER_PAGE = 2;
  private static final int PAGE_SIZE = COLUMNS * ROWS_PER_PAGE;

  private static final float PAGER_BUTTON_SIZE = 40f;
  private static final float PAGER_GAP = 10f;

  // Fixed insets from the window's own top-left corner to the grid's top-left corner, chosen to
  // clear the title bar/close button and leave room for the "<" pager button just left of the
  // grid, without needing to introspect Window's internal padding.
  private static final float GRID_LEFT_INSET = PAGER_BUTTON_SIZE + PAGER_GAP + 40f;
  private static final float GRID_TOP_INSET = 70f;

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

  private final Set<CardInstance> selected = new LinkedHashSet<>();
  private final Map<String, CardInstance> poolByKey = new HashMap<>();
  private Set<CardInstance> discardedInstances = new HashSet<>();
  private List<CardInstance> pool = new ArrayList<>();
  private int currentPage;

  private Label summaryLabel;
  private Label errorLabel;
  private Cell<Actor> gridSpacerCell;

  /**
   * @param cardPlayService source of the player's card pool/hand and the rearrange operation
   * @param library card definitions, for names/art
   * @param popup the popup window this editor renders into
   * @param poolFactory a {@link ClickableFactory} dedicated to this editor's toggle buttons (must
   *     not be shared with any other widget group, e.g. the main hand row)
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
    entity.getEvents().addListener(PREV_PAGE_TRIGGER, this::previousPage);
    entity.getEvents().addListener(NEXT_PAGE_TRIGGER, this::nextPage);
    popup.setOnShow(this::onOpened);
    popup.setOnHide(this::onClosed);
    popup.setOnWindowClicked(poolFactory::bringToFront);
    buildFooter();
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
    gridSpacerCell = content.add(gridSpacer).colspan(2).height(0f);
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

    content.add(summaryLabel).width(520f).padTop(10f).padRight(20f);
    content.add(setDeckButton).size(140f, 48f).padTop(10f);
    content.row();

    errorLabel = new Label("", skin);
    errorLabel.setWrap(true);
    errorLabel.setColor(Color.SALMON);
    content.add(errorLabel).colspan(2).width(660f).padTop(6f);
  }

  private void refreshPoolAndSelection() {
    pool = cardPlayService.allInstances();
    poolByKey.clear();
    for (CardInstance instance : pool) {
      poolByKey.put(instance.instanceId(), instance);
    }
    discardedInstances = new HashSet<>(cardPlayService.discardedInstances());
    selected.clear();
    selected.addAll(cardPlayService.handInstances());
    currentPage = 0;
  }

  private int totalPages() {
    return pool.isEmpty() ? 1 : ((pool.size() - 1) / PAGE_SIZE) + 1;
  }

  private void previousPage() {
    if (currentPage > 0) {
      currentPage--;
      buildGridWidgets();
    }
  }

  private void nextPage() {
    if (currentPage < totalPages() - 1) {
      currentPage++;
      buildGridWidgets();
    }
  }

  private void updateGridSpacer() {
    float height = ROWS_PER_PAGE * CARD_HEIGHT + Math.max(0, ROWS_PER_PAGE - 1) * GRID_GAP;
    gridSpacerCell.height(height);
  }

  private void onOpened() {
    buildGridWidgets();
    refreshSummary();
    errorLabel.setText("");
    poolFactory.bringToFront();
  }

  private void onClosed() {
    poolFactory.rebuildByTrigger(SELECT_TRIGGER, List.of());
    poolFactory.rebuildByTrigger(PREV_PAGE_TRIGGER, List.of());
    poolFactory.rebuildByTrigger(NEXT_PAGE_TRIGGER, List.of());
  }

  /**
   * Rebuilds the card grid for {@link #currentPage} plus the "<"/">" pager buttons flanking it,
   * anchored to the popup window's actual on-screen bounds. Called on open and on every page
   * change — pages are cheap enough to rebuild wholesale rather than track incrementally.
   */
  private void buildGridWidgets() {
    int totalPages = totalPages();
    currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

    float windowX = popup.getWindowX();
    float windowTopY = popup.getWindowY() + popup.getWindowHeight();
    float stageHeight = stage.getViewport().getWorldHeight();

    float gridOriginX = windowX + GRID_LEFT_INSET;
    float gridTopY = windowTopY - GRID_TOP_INSET;
    float gridWidth = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GRID_GAP;
    float gridHeight = ROWS_PER_PAGE * CARD_HEIGHT + (ROWS_PER_PAGE - 1) * GRID_GAP;

    int start = currentPage * PAGE_SIZE;
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
      int row = indexOnPage / COLUMNS;
      int col = indexOnPage % COLUMNS;
      float screenX = gridOriginX + col * (CARD_WIDTH + GRID_GAP);
      float screenTopY = gridTopY - row * (CARD_HEIGHT + GRID_GAP);
      float screenBottomY = screenTopY - CARD_HEIGHT;
      float recordY = stageHeight - screenBottomY;

      Skin cardSkin = CardImageSkins.forTexturePath(card.texturePath);

      // Discarded (on-cooldown) cards stay selectable here — see rearrangeHand's javadoc — so this
      // deliberately never sets ClickableRecord.disabled(true); discarded/selected state is shown
      // entirely through applySelectionHighlights' tinting instead.
      ClickableRecord record =
          ClickableRecord.builder(SELECT_TRIGGER)
              .label(card.name)
              .position(screenX, recordY)
              .size(CARD_WIDTH, CARD_HEIGHT)
              .skin(cardSkin)
              .args(instance.instanceId())
              .build();
      cardRecords.add(record);
    }

    // Square "<"/">" buttons, vertically centred on the grid, just outside its left/right edges.
    float pagerBottomY = (gridTopY - gridHeight / 2f) - PAGER_BUTTON_SIZE / 2f;
    float pagerRecordY = stageHeight - pagerBottomY;
    float prevX = gridOriginX - PAGER_BUTTON_SIZE - PAGER_GAP;
    float nextX = gridOriginX + gridWidth + PAGER_GAP;

    ClickableRecord prevRecord =
        ClickableRecord.builder(PREV_PAGE_TRIGGER)
            .text("<")
            .position(prevX, pagerRecordY)
            .size(PAGER_BUTTON_SIZE, PAGER_BUTTON_SIZE)
            .disabled(currentPage <= 0)
            .build();
    ClickableRecord nextRecord =
        ClickableRecord.builder(NEXT_PAGE_TRIGGER)
            .text(">")
            .position(nextX, pagerRecordY)
            .size(PAGER_BUTTON_SIZE, PAGER_BUTTON_SIZE)
            .disabled(currentPage >= totalPages - 1)
            .build();

    poolFactory.rebuildByTrigger(SELECT_TRIGGER, cardRecords);
    poolFactory.rebuildByTrigger(PREV_PAGE_TRIGGER, List.of(prevRecord));
    poolFactory.rebuildByTrigger(NEXT_PAGE_TRIGGER, List.of(nextRecord));
    applySelectionHighlights();
  }

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
    if (!selected.remove(instance)) {
      selected.add(instance);
    }
    applySelectionHighlights();
    refreshSummary();
    errorLabel.setText("");
  }

  private void onSetDeckClicked() {
    if (selected.size() != REQUIRED_HAND_SIZE) {
      errorLabel.setText("Pick exactly " + REQUIRED_HAND_SIZE + " cards.");
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
    StringBuilder text =
        new StringBuilder("Selected (" + selected.size() + "/" + REQUIRED_HAND_SIZE + "): ");
    boolean first = true;
    for (CardInstance instance : selected) {
      if (!first) {
        text.append(", ");
      }
      text.append(library.getCard(instance.cardId()).map(c -> c.name).orElse(instance.cardId()));
      first = false;
    }
    return text.toString();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage; nothing to do per-frame here.
  }
}
