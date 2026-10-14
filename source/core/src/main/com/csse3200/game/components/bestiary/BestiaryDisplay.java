package com.csse3200.game.components.bestiary;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.bestiary.BestiaryEntryView;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.LibraryNameSearch;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Scene2D component that displays the enemy bestiary. */
public class BestiaryDisplay extends UIComponent {
  private static final float Z_INDEX = 3f;
  private static final float PANEL_WIDTH = 1160f;
  private static final String FALLBACK_ATLAS = "images/enemies/default.atlas";
  public static final String BUTTON_TEXTURE = "images/bestiary_button_frame.png";
  private static final Color BACKDROP_COLOUR = new Color(0.018f, 0.012f, 0.02f, 0.92f);
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.98f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.95f, 0.73f, 0.28f, 1f);
  private static final Color BODY_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color MUTED_COLOUR = new Color(0.65f, 0.58f, 0.52f, 1f);
  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";
  static final String EMPTY_STATE_MESSAGE = "No enemies discovered in this category yet.";
  static final String NO_RESULTS_MESSAGE =
      "No discovered enemies match this search in this category.";
  private final BestiaryService bestiary;
  private final Runnable returnAction;
  private final EventListener1<BestiaryEntryView> entryUpdatedListener = this::onEntryUpdated;
  private EnemyTier activeTier = EnemyTier.NORMAL;
  private BestiaryEntryView displayedEntry;

  private Table rootTable;
  private Table enemyListTable;
  private final Map<String, TextButton> enemyButtons = new HashMap<>();
  private ScrollPane enemyScroll;
  private ScrollPane detailScroll;
  private TextField searchField;
  private ChangeListener searchChangedListener;
  private Label resultCountLabel;
  private Label emptyStateLabel;
  private Image detailImage;
  private Label lockedArtLabel;
  private Label detailStateLabel;
  private Label detailNameLabel;
  private Label detailTierLabel;
  private Label detailDescriptionLabel;
  private Label detailStatsLabel;

  /**
   * Creates a bestiary display.
   *
   * @param bestiary source of progress-aware enemy entries
   * @param returnAction action invoked by the Back button
   */
  public BestiaryDisplay(BestiaryService bestiary, Runnable returnAction) {
    this.bestiary = Objects.requireNonNull(bestiary, "bestiary cannot be null");
    this.returnAction = Objects.requireNonNull(returnAction, "returnAction cannot be null");
  }

  @Override
  public void create() {
    super.create();
    addActors();
    bestiary.getEvents().addListener(BestiaryService.ENTRY_UPDATED_EVENT, entryUpdatedListener);
  }

  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setTouchable(Touchable.enabled);
    rootTable.setBackground(skin.newDrawable(WHITE, BACKDROP_COLOUR));
    rootTable.center();

    Table panel = new Table();
    panel.setBackground(skin.newDrawable("window-w", PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    addDivider(panel);
    panel.row();
    TextButton normalButton = addFilters(panel);
    panel.row();
    addContent(panel);

    rootTable.add(panel).width(PANEL_WIDTH).height(720f);
    stage.addActor(rootTable);

    normalButton.setChecked(true);
    applyFilter(EnemyTier.NORMAL);
  }

  private void addHeader(Table panel) {
    Table titleBlock = new Table();
    Label eyebrow = new Label("ENEMY ARCHIVE", createLabelStyle(SMALL, GOLD_COLOUR));
    Label title = new Label("Bestiary", createLabelStyle(LARGE, BODY_COLOUR));
    Label subtitle =
        new Label(
            "Discover enemies to reveal their records.", createLabelStyle(SMALL, MUTED_COLOUR));
    eyebrow.setFontScale(1.2f);
    title.setFontScale(1.45f);
    subtitle.setFontScale(1.15f);
    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);

    TextButton backButton = new TextButton("Back", createButtonStyle());
    backButton.setName("bestiary-back");
    backButton.getLabel().setFontScale(1.2f);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            returnAction.run();
          }
        });

    // Keep header sizing independent of the list/detail columns as search results change.
    Table header = new Table();
    header.add(titleBlock).left().expandX();
    header.add(backButton).right().width(180f).height(60f);
    panel.add(header).colspan(2).fillX();
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, GOLD_COLOUR));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(14f);
  }

  private TextButton addFilters(Table panel) {
    Table filters = new Table();
    filters.defaults().width(185f).height(62f).padRight(12f);
    ButtonGroup<TextButton> filterGroup = new ButtonGroup<>();
    filterGroup.setMinCheckCount(1);
    filterGroup.setMaxCheckCount(1);
    filterGroup.setUncheckLast(true);

    TextButton normalButton = createFilterButton("Normal", EnemyTier.NORMAL, filterGroup);
    filters.add(normalButton);
    filters.add(createFilterButton("Elite", EnemyTier.ELITE, filterGroup));
    filters.add(createFilterButton("Boss", EnemyTier.BOSS, filterGroup));

    panel.add(filters).left().colspan(2).padBottom(16f);
    return normalButton;
  }

  private TextButton createFilterButton(
      String text, EnemyTier tier, ButtonGroup<TextButton> group) {
    TextButtonStyle style = createButtonStyle();
    style.checked = buttonDrawable(new Color(1f, 0.72f, 0.3f, 1f));
    style.checkedFontColor = Color.WHITE;
    TextButton button = new TextButton(text, style);
    button.setName("bestiary-filter-" + tier.name());
    button.getLabel().setFontScale(1.15f);
    group.add(button);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (button.isChecked()) {
              applyFilter(tier);
            }
          }
        });
    return button;
  }

  private void addContent(Table panel) {
    enemyListTable = new Table();
    enemyListTable.setName("bestiary-enemy-list");
    enemyListTable.top();
    enemyScroll = new ScrollPane(enemyListTable, skin);
    enemyScroll.setName("bestiary-enemy-scroll");
    enemyScroll.setFadeScrollBars(false);
    enemyScroll.setScrollingDisabled(true, false);
    enemyScroll.setForceScroll(false, true);
    emptyStateLabel = new Label("", createLabelStyle(SMALL, MUTED_COLOUR));
    emptyStateLabel.setName("bestiary-no-results");
    emptyStateLabel.setWrap(true);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    Label listTitle = new Label("ENEMIES", createLabelStyle(SMALL, GOLD_COLOUR));
    listTitle.setFontScale(1.15f);
    listPanel.add(listTitle).left().expandX();
    listPanel.row();
    addSearchControls(listPanel);
    listPanel.row();
    listPanel.add(enemyScroll).expand().fill().padTop(12f);

    Table detailPanel = createDetailPanel();
    panel.add(listPanel).width(350f).expandY().fillY().padRight(22f);
    panel.add(detailPanel).expand().fill();
  }

  private void addSearchControls(Table listPanel) {
    listPanel
        .add(new Label("Search discovered enemies", createLabelStyle(SMALL, BODY_COLOUR)))
        .left()
        .padTop(10f);
    listPanel.row();
    searchField = new TextField("", skin);
    searchField.setName("bestiary-search");
    searchField.setMessageText("Enemy name...");
    searchField.setProgrammaticChangeEvents(true);
    searchChangedListener =
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            rebuildEnemyList(false);
            resetListScroll();
          }
        };
    searchField.addListener(searchChangedListener);
    TextButton clear = new TextButton("Clear", skin);
    clear.setName("bestiary-search-clear");
    clear.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (searchField == null) return;
            searchField.setText("");
            stage.setKeyboardFocus(searchField);
          }
        });
    Table searchRow = new Table();
    searchRow.add(searchField).growX().minWidth(0f).height(42f);
    searchRow.add(clear).width(76f).height(42f).padLeft(8f);
    listPanel.add(searchRow).growX().padTop(5f);
    listPanel.row();
    resultCountLabel = new Label("", createLabelStyle(SMALL, GOLD_COLOUR));
    resultCountLabel.setName("bestiary-result-count");
    listPanel.add(resultCountLabel).left().padTop(8f);
  }

  private Table createDetailPanel() {
    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(24f);
    Table detailContent = new Table();
    detailContent.top();

    detailStateLabel = new Label("", createLabelStyle(SMALL, GOLD_COLOUR));
    detailNameLabel = new Label("", createLabelStyle(LARGE, BODY_COLOUR));
    detailTierLabel = new Label("", createLabelStyle(SMALL, MUTED_COLOUR));
    detailDescriptionLabel = new Label("", createLabelStyle(DEFAULT, BODY_COLOUR));
    detailStatsLabel = new Label("", createLabelStyle(DEFAULT, GOLD_COLOUR));
    detailNameLabel.setName("bestiary-name");
    detailDescriptionLabel.setName("bestiary-description");
    detailStatsLabel.setName("bestiary-stats");
    detailStateLabel.setFontScale(1.1f);
    detailNameLabel.setFontScale(1.25f);
    detailNameLabel.setWrap(true);
    detailTierLabel.setFontScale(1.1f);
    detailDescriptionLabel.setFontScale(1.15f);
    detailDescriptionLabel.setWrap(true);
    detailStatsLabel.setFontScale(1.15f);
    detailStatsLabel.setWrap(true);

    Table artBackground = new Table();
    artBackground.setBackground(skin.newDrawable(WHITE, new Color(0.035f, 0.03f, 0.04f, 1f)));
    detailImage = new Image();
    detailImage.setName("bestiary-artwork");
    detailImage.setScaling(Scaling.fit);
    lockedArtLabel = new Label("?", createLabelStyle(LARGE, MUTED_COLOUR));
    lockedArtLabel.setFontScale(4f);

    Stack artwork = new Stack();
    artwork.add(artBackground);
    artwork.add(detailImage);
    Table lockedLayer = new Table();
    lockedLayer.add(lockedArtLabel).center();
    artwork.add(lockedLayer);

    detailContent.add(detailStateLabel).left().expandX();
    detailContent.row();
    detailContent.add(detailNameLabel).left().growX().minWidth(0f).padTop(4f);
    detailContent.row();
    detailContent.add(detailTierLabel).left().padTop(4f);
    detailContent.row();
    detailContent.add(artwork).growX().minWidth(0f).height(260f).padTop(14f);
    detailContent.row();
    detailContent.add(detailDescriptionLabel).left().top().growX().minWidth(0f).padTop(16f);
    detailContent.row();
    detailContent.add(detailStatsLabel).left().growX().minWidth(0f).padTop(16f);
    detailScroll = new ScrollPane(detailContent, skin);
    detailScroll.setName("bestiary-detail-scroll");
    detailScroll.setScrollingDisabled(true, false);
    detailScroll.setFadeScrollBars(false);
    detailPanel.add(detailScroll).grow().minSize(0f);
    return detailPanel;
  }

  void applyFilter(EnemyTier tier) {
    if (searchField == null) return;
    activeTier = tier;
    rebuildEnemyList(LibraryNameSearch.normalize(searchField.getText()).isEmpty());
    resetListScroll();
  }

  private void resetListScroll() {
    enemyScroll.setScrollY(0f);
    enemyScroll.updateVisualScroll();
  }

  private boolean eligible(BestiaryEntryView entry) {
    return searchField != null
        && entry.tier() == activeTier
        && entry.unlockState().isAtLeast(BestiaryUnlockState.ENCOUNTERED)
        && LibraryNameSearch.matches(entry.displayName(), searchField.getText());
  }

  private void rebuildEnemyList(boolean selectFirst) {
    String selectedEnemyId = displayedEntry == null ? null : displayedEntry.enemyId();
    Actor focus = stage.getKeyboardFocus();
    enemyListTable.clearChildren();
    List<BestiaryEntryView> filtered =
        bestiary.getDiscoveredEntriesByTier(activeTier).stream().filter(this::eligible).toList();
    boolean searching = !LibraryNameSearch.normalize(searchField.getText()).isEmpty();
    resultCountLabel.setText(
        filtered.size()
            + (searching
                ? (filtered.size() == 1 ? " result" : " results")
                : (filtered.size() == 1 ? " enemy" : " enemies")));
    if (filtered.isEmpty()) {
      emptyStateLabel.setText(searching ? NO_RESULTS_MESSAGE : EMPTY_STATE_MESSAGE);
      enemyListTable.add(emptyStateLabel).width(285f).padTop(24f);
      clearDetails(true);
      return;
    }

    for (BestiaryEntryView entry : filtered) {
      TextButton entryButton =
          enemyButtons.computeIfAbsent(entry.enemyId(), this::createEntryButton);
      entryButton.setText(visibleName(entry));
      enemyListTable.add(entryButton).width(285f).height(76f).padBottom(8f);
      enemyListTable.row();
    }
    if (focus != null && focus.isDescendantOf(enemyListTable)) stage.setKeyboardFocus(focus);
    BestiaryEntryView selection =
        filtered.stream()
            .filter(entry -> entry.enemyId().equals(selectedEnemyId))
            .findFirst()
            .orElse(selectFirst ? filtered.getFirst() : null);
    if (selection == null) clearDetails(false);
    else showDetails(selection);
  }

  private TextButton createEntryButton(String enemyId) {
    TextButton button = new TextButton("", createButtonStyle());
    button.getLabel().setFontScale(1.1f);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (button.getParent() == enemyListTable) {
              // Resolve latest progress; old callbacks must not expose a relocked/filtered record.
              bestiary
                  .getEntry(enemyId)
                  .filter(BestiaryDisplay.this::eligible)
                  .ifPresent(BestiaryDisplay.this::showDetails);
            }
          }
        });
    return button;
  }

  private void clearDetails(boolean empty) {
    displayedEntry = null;
    boolean searching = !LibraryNameSearch.normalize(searchField.getText()).isEmpty();
    detailStateLabel.setText(empty ? (searching ? "NO RESULTS" : "NO RECORDS") : "NO SELECTION");
    detailNameLabel.setText("No enemy selected");
    detailTierLabel.setText("");
    detailDescriptionLabel.setText(
        empty
            ? (searching
                ? NO_RESULTS_MESSAGE
                : "New records will appear here when they become available.")
            : "Select a discovered enemy to view its record.");
    detailStatsLabel.setText("");
    detailImage.setDrawable(null);
    detailImage.setVisible(false);
    lockedArtLabel.setVisible(true);
    detailScroll.setScrollY(0f);
    detailScroll.updateVisualScroll();
  }

  private void showDetails(BestiaryEntryView entry) {
    if (displayedEntry == null || !displayedEntry.enemyId().equals(entry.enemyId())) {
      detailScroll.setScrollY(0f);
      detailScroll.updateVisualScroll();
    }
    displayedEntry = entry;
    detailNameLabel.setText(visibleName(entry));
    detailTierLabel.setText(entry.tier().name());
    detailDescriptionLabel.setText(descriptionFor(entry));
    detailStatsLabel.setText(statsFor(entry));

    if (entry.unlockState() == BestiaryUnlockState.LOCKED) {
      detailStateLabel.setText("UNDISCOVERED");
      detailImage.setDrawable(null);
      detailImage.setVisible(false);
      lockedArtLabel.setVisible(true);
      return;
    }

    detailStateLabel.setText(entry.unlockState().name());
    lockedArtLabel.setVisible(false);
    setEnemyImage(entry.sprite().orElse(""));
  }

  private void onEntryUpdated(BestiaryEntryView entry) {
    if (entry.tier() == activeTier && rootTable != null) {
      rebuildEnemyList(LibraryNameSearch.normalize(searchField.getText()).isEmpty());
    }
  }

  BestiaryEntryView getDisplayedEntry() {
    return displayedEntry;
  }

  String getEmptyStateText() {
    return emptyStateLabel.getParent() == enemyListTable
        ? emptyStateLabel.getText().toString()
        : null;
  }

  static String visibleName(BestiaryEntryView entry) {
    return entry.displayName();
  }

  static String descriptionFor(BestiaryEntryView entry) {
    return switch (entry.unlockState()) {
      case LOCKED -> "Encounter this enemy to reveal its record.";
      case ENCOUNTERED -> "Defeat this enemy to reveal its complete record.";
      case DEFEATED -> entry.description().orElse("No description available.");
    };
  }

  static String statsFor(BestiaryEntryView entry) {
    if (!entry.hasFullDetails()) {
      return "HP  ???     ATTACK  ???     ARMOUR  ???";
    }

    String behaviour = entry.behaviour().orElse("Unknown").replace('_', ' ').toUpperCase();
    return String.format(
        "HP  %d     ATTACK  %d     ARMOUR  %d\nBEHAVIOUR  %s",
        entry.health().orElse(0),
        entry.baseAttack().orElse(0),
        entry.armour().orElse(0),
        behaviour);
  }

  private void setEnemyImage(String atlasPath) {
    ResourceService resources = ServiceLocator.getResourceService();
    String resolvedPath = atlasPath;
    if (resolvedPath.isBlank() || !resources.containsAsset(resolvedPath, TextureAtlas.class)) {
      resolvedPath = FALLBACK_ATLAS;
    }
    if (!resources.containsAsset(resolvedPath, TextureAtlas.class)) {
      detailImage.setDrawable(null);
      detailImage.setVisible(false);
      lockedArtLabel.setVisible(true);
      return;
    }

    TextureAtlas atlas = resources.getAsset(resolvedPath, TextureAtlas.class);
    AtlasRegion region = atlas.findRegion(DEFAULT);
    if (region == null) {
      region = atlas.findRegion("idle", 0);
    }
    if (region == null) {
      detailImage.setDrawable(null);
      detailImage.setVisible(false);
      lockedArtLabel.setVisible(true);
      return;
    }

    detailImage.setDrawable(new TextureRegionDrawable(region));
    detailImage.setVisible(true);
    lockedArtLabel.setVisible(false);
  }

  private LabelStyle createLabelStyle(String baseStyle, Color colour) {
    LabelStyle style = new LabelStyle(skin.get(baseStyle, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private TextButtonStyle createButtonStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = buttonDrawable(null);
    style.over = buttonDrawable(new Color(1f, 0.82f, 0.58f, 1f));
    style.down = buttonDrawable(new Color(0.68f, 0.48f, 0.44f, 1f));
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  private Drawable buttonDrawable(Color tint) {
    Texture texture = ServiceLocator.getResourceService().getAsset(BUTTON_TEXTURE, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
    drawable.setMinWidth(0f);
    drawable.setMinHeight(0f);
    return tint == null ? drawable : drawable.tint(tint);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    bestiary.getEvents().removeListener(BestiaryService.ENTRY_UPDATED_EVENT, entryUpdatedListener);
    if (searchField != null) searchField.removeListener(searchChangedListener);
    if (rootTable != null) {
      Actor focus = stage.getKeyboardFocus();
      if (focus != null && focus.isDescendantOf(rootTable)) stage.setKeyboardFocus(null);
      Actor scrollFocus = stage.getScrollFocus();
      if (scrollFocus != null && scrollFocus.isDescendantOf(rootTable)) stage.setScrollFocus(null);
      rootTable.remove();
      rootTable.clear();
    }
    enemyButtons.clear();
    searchField = null;
    searchChangedListener = null;
    super.dispose();
  }
}
