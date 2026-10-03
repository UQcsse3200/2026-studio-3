package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
 * <p>Layout (left to right): a scrollable grid of cards, then a preview panel showing the
 * hovered/last-clicked card enlarged with its details underneath. "Set Deck" sits bottom-left.
 * Cards currently in the hand are listed first. Every selected card shows a numbered badge in its
 * top-right corner; the number is its position in the hand row (selection order).
 *
 * <p>Every card the player owns is tracked as a distinct {@link CardInstance} (see {@link
 * com.csse3200.game.cards.deck.PlayerDeck}), not just a card ID, so two copies of the same card
 * (e.g. two "strike" cards) are selected, highlighted and submitted independently.
 *
 * <p>The grid cards and the scroll up/down buttons are built with a dedicated {@link
 * ClickableFactory} (they need click/hover). Everything else that renders in stage space — the
 * preview card art, the preview details text, the selection summary, the error line, and the
 * numbered badges — is built through a {@link DisplayingFactory} whose variants are anchored to the
 * popup's window and driven by {@link DeckEditorEvents}. The two factories share this component's
 * entity, so they all communicate via its event handler.
 *
 * <p>Scrolling is row-by-row (mouse wheel or the up/down buttons) rather than a pixel-smooth {@code
 * ScrollPane}: stage-level actors aren't clipped by a pane, so only whole rows are ever shown.
 */
public class DeckEditorComponent extends UIComponent {
    private static final Logger logger = LoggerFactory.getLogger(DeckEditorComponent.class);

    private static final String SELECT_TRIGGER = "toggleDeckCard";
    private static final String SCROLL_UP_TRIGGER = "deckScrollUp"; // defined in DeckEditorUi.json
    private static final String SCROLL_DOWN_TRIGGER =
            "deckScrollDown"; // defined in DeckEditorUi.json

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

    private static final Color DISCARDED_TINT = new Color(0.35f, 0.35f, 0.35f, 1f);
    private static final Color SELECTED_TINT = new Color(0.55f, 0.85f, 1f, 1f);
    // Both apply (a card that's on cooldown AND selected as a reserved slot): overlay both colours
    // rather than picking one, so it reads as neither "just discarded" nor "just selected".
    private static final Color SELECTED_DISCARDED_TINT =
            new Color(DISCARDED_TINT).lerp(SELECTED_TINT, 0.5f);

    private static final int HAND_SIZE = 5;

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
     * @param poolFactory a {@link ClickableFactory} dedicated to this editor's clickable widgets (the
     *     grid cards and the scroll up/down buttons). Built from {@code sprites/DeckEditorUi.json}.
     * @param displayFactory a {@link DisplayingFactory} carrying the popup-anchored variants ({@code
     *     popupText}, {@code cardPreview}, {@code cardBadges}) for the summary, error, preview and
     *     badge displays. Must be attached to the same entity as this component. The component
     *     itself only talks to those displays via {@link DeckEditorEvents}; the reference is
     *     required here solely so callers wire them onto the correct entity.
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
            DisplayingFactory displayFactory,
            Consumer<List<CardInstance>> onDeckChanged) {
        this.cardPlayService = cardPlayService;
        this.library = library;
        this.popup = popup;
        this.poolFactory = poolFactory;
        this.onDeckChanged = onDeckChanged;
        // The display variants are driven entirely through entity events (see class javadoc); this
        // reference exists purely as a wiring contract so callers attach them to the same entity.
        Objects.requireNonNull(displayFactory, "displayFactory must be attached to the same entity");
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

    /**
     * Reserves room in the popup's content table for the stage-level grid, preview panel, summary
     * line, error line and (implicitly) the badges. The actual drawing is handled by the {@link
     * DisplayingFactory} variants and by {@link ClickableFactory}; the content table only needs empty
     * Actors to push the "Set Deck" button below everything else.
     */
    private void buildFooter() {
        Table content = popup.getContentTable();

        Actor gridSpacer = new Actor();
        gridSpacerCell = content.add(gridSpacer).colspan(2);
        content.row();

        TextButton setDeckButton = new TextButton("Set Deck", skin);
        setDeckButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        onSetDeckClicked();
                    }
                });

        // "Set Deck" bottom-left, with the summary text to its right (drawn by PopupTextDisplay).
        content.add(setDeckButton).size(140f, 48f).padTop(10f).padRight(20f);
        Actor summarySpacer = new Actor();
        content.add(summarySpacer).width(520f).padTop(10f);
        content.row();

        // Error line (drawn by PopupTextDisplay).
        Actor errorSpacer = new Actor();
        content.add(errorSpacer).colspan(2).width(660f).padTop(6f);
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
        int target = (int) Math.clamp((long) scrollRow + rows, 0L, maxScrollRow());
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
        entity.getEvents().trigger(DeckEditorEvents.OPENED);
        buildGridWidgets(); // also picks the initial preview
        refreshSummary();
        entity.getEvents().trigger(DeckEditorEvents.ERROR, "");
        bringEditorWidgetsToFront();
    }

    private void onClosed() {
        open = false;
        entity.getEvents().trigger(DeckEditorEvents.CLOSED); // clears badges + preview + summary
        poolFactory.rebuildByTrigger(SELECT_TRIGGER, List.of());
        setScrollButtonsVisible(false);
        previewed = null;
    }

    /**
     * A libGDX Window calls toFront() on itself on every touch inside it, which would bury all our
     * stage-level siblings — so re-assert their order (cards, preview, badges, text) on top.
     */
    private void bringEditorWidgetsToFront() {
        poolFactory.bringToFront();
        entity.getEvents().trigger(DeckEditorEvents.TO_FRONT);
    }

    // ---- building widgets ------------------------------------------------------------------

    /**
     * Rebuilds the visible rows of cards for {@link #scrollRow}, repositions the (JSON-defined)
     * scroll buttons, refreshes highlights/badges, and pushes the current preview into the {@link
     * CardPreviewDisplay}. Called on open and on every scroll.
     */
    private void buildGridWidgets() {
        scrollRow = (int) Math.clamp( scrollRow, 0L, maxScrollRow());
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
            cardRecords.add(
                    ClickableRecord.builder(SELECT_TRIGGER)
                            .label(card.name)
                            .position(cardX(indexOnPage), stageHeight - cardBottomY(indexOnPage))
                            .size(CARD_WIDTH, CARD_HEIGHT)
                            .skin(cardSkin)
                            .args(instance.instanceId())
                            .build());
        }

        poolFactory.rebuildByTrigger(SELECT_TRIGGER, cardRecords);
        attachHoverPreview();
        layoutScrollButtons();
        applySelectionHighlights();

        // Keep previewing the same card if it's still on this page, else fall back to the first one.
        CardInstance target = previewed;
        if (target == null || !pageContains(target, start, end)) {
            target = start < end ? pool.get(start) : null;
        }
        showPreview(target);
    }

    private boolean pageContains(CardInstance instance, int start, int end) {
        for (int i = start; i < end; i++) {
            if (pool.get(i).equals(instance)) {
                return true;
            }
        }
        return false;
    }

    /** Hovering a card shows it in the preview panel. Listeners are added once per (re)build. */
    private void attachHoverPreview() {
        for (Clickable widget : poolFactory.getByTrigger(SELECT_TRIGGER)) {
            Object[] args = widget.getArgs();
            CardInstance instance =
                    args.length == 0 ? null : poolByKey.get(String.valueOf(args[0]));
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

    /**
     * Pushes the current preview into {@link CardPreviewDisplay} via {@link
     * DeckEditorEvents#PREVIEW}.
     */
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
        // Note: stats (energy cost, damage/block/effect values, description) are not shown here yet,
        //  because CardConfig only exposes name/texturePath/target at the moment. Add them once the
        //  remaining fields (or the effect definitions that carry the numbers) are available.
        return text.toString();
    }

    // ---- selection / highlights / badges ---------------------------------------------------

    private void applySelectionHighlights() {
        for (Clickable widget : poolFactory.getByTrigger(SELECT_TRIGGER)) {
            Object[] args = widget.getArgs();
            CardInstance instance =
                    args.length == 0 ? null : poolByKey.get(String.valueOf(args[0]));
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
     * Pushes the numbered badges for every visible selected card into {@link CardBadgesDisplay}. The
     * number is the card's position in {@link #selected}'s iteration order, i.e. its slot in the hand
     * row — so deselecting a card renumbers the ones after it.
     */
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
            // Badge sits at the card's top-right corner, straddling it (CardBadgesDisplay handles the
            // overhang).
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
        // If it's already selected, deselect. Otherwise, only add while under the cap.
        if (selected.remove(instance)) {
            // deselecting always allowed
        } else {
            if (selected.size() >= HAND_SIZE) {
                entity
                        .getEvents()
                        .trigger(
                                DeckEditorEvents.ERROR, "Hand is full — deselect a card before adding another.");
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
        // The display variants live in DisplayingFactory, which owns their disposal.
        super.dispose();
    }

    @Override
    protected void draw(SpriteBatch batch) {
        // Actors are drawn by the stage; nothing to do per-frame here.
    }
}