package com.csse3200.game.components.bestiary;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.bestiary.BestiaryEntryView;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.List;
import java.util.Objects;

/** Scene2D component that displays the enemy bestiary. */
public class BestiaryDisplay extends UIComponent {
  private static final float Z_INDEX = 3f;
  private static final float PANEL_WIDTH = 1160f;
  private static final String FALLBACK_ATLAS = "images/enemies/default.atlas";
  public static final String BUTTON_TEXTURE = "images/bestiary_button_frame.png";
  public static final String ELITE_FRAME_TEXTURE = "images/bestiary_elite_frame.png";
  public static final String BOSS_FRAME_TEXTURE = "images/bestiary_boss_frame.png";
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
  static final String EMPTY_STATE_MESSAGE = "No enemies in this category.";
  private static final Color NORMAL_COLOUR = new Color(0.76f, 0.64f, 0.46f, 1f);
  private static final Color ELITE_COLOUR = new Color(0.95f, 0.47f, 0.19f, 1f);
  private static final Color BOSS_COLOUR = new Color(0.82f, 0.19f, 0.22f, 1f);
  private final BestiaryService bestiary;
  private final Runnable returnAction;
  private final EventListener1<BestiaryEntryView> entryUpdatedListener = this::onEntryUpdated;
  private EnemyTier activeTier = EnemyTier.NORMAL;
  private BestiaryEntryView displayedEntry;

  private Table rootTable;
  private Table enemyListTable;
  private Label emptyStateLabel;
  private Image detailImage;
  private Label lockedArtLabel;
  private Label detailStateLabel;
  private Label detailNameLabel;
  private Label detailTierLabel;
  private Image detailTierFrame;
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
            "All enemies. Every tier. Know what awaits.", createLabelStyle(SMALL, MUTED_COLOUR));
    eyebrow.setFontScale(1.2f);
    title.setFontScale(1.45f);
    subtitle.setFontScale(1.15f);
    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);

    Texture backFrame =
        ServiceLocator.getResourceService()
            .getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class);
    backFrame.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextButton backButton = new TextButton("Back", MenuTheme.createButtonStyle(skin, backFrame));
    backButton.getLabel().setFontScale(0.85f);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            returnAction.run();
          }
        });

    panel.add(titleBlock).left().expandX();
    panel.add(backButton).right().width(170f).height(58f);
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, GOLD_COLOUR));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(14f);
  }

  private TextButton addFilters(Table panel) {
    Table filters = new Table();
    filters.defaults().padRight(12f);
    ButtonGroup<TextButton> filterGroup = new ButtonGroup<>();
    filterGroup.setMinCheckCount(1);
    filterGroup.setMaxCheckCount(1);
    filterGroup.setUncheckLast(true);

    TextButton normalButton = createFilterButton("Normal", EnemyTier.NORMAL, filterGroup);
    filters.add(normalButton).width(185f).height(62f);
    filters.add(createFilterButton("Elite", EnemyTier.ELITE, filterGroup)).width(191f).height(64f);
    filters.add(createFilterButton("Boss", EnemyTier.BOSS, filterGroup)).width(198f).height(66f);

    panel.add(filters).left().colspan(2).padBottom(16f);
    return normalButton;
  }

  private TextButton createFilterButton(
      String text, EnemyTier tier, ButtonGroup<TextButton> group) {
    TextButtonStyle style = createButtonStyle();
    style.up = tierButtonDrawable(tier, null);
    style.over = tierButtonDrawable(tier, new Color(1f, 0.9f, 0.8f, 1f));
    style.down = tierButtonDrawable(tier, new Color(0.8f, 0.7f, 0.7f, 1f));
    style.checked = tierButtonDrawable(tier, null);
    style.checkedFontColor = Color.WHITE;
    TextButton button = new TextButton(text, style);
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
    enemyListTable.top();
    ScrollPane scrollPane = new ScrollPane(enemyListTable, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);
    scrollPane.setForceScroll(false, true);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    Label listTitle = new Label("ENEMIES", createLabelStyle(SMALL, GOLD_COLOUR));
    listTitle.setFontScale(1.15f);
    listPanel.add(listTitle).left().expandX();
    listPanel.row();
    listPanel.add(scrollPane).expand().fill().padTop(12f);

    Table detailPanel = createDetailPanel();
    panel.add(listPanel).width(350f).expandY().fillY().padRight(22f);
    panel.add(detailPanel).expand().fill();
  }

  private Table createDetailPanel() {
    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(24f);
    detailPanel.top();

    detailStateLabel = new Label("", createLabelStyle(SMALL, GOLD_COLOUR));
    detailNameLabel = new Label("", createLabelStyle(LARGE, BODY_COLOUR));
    detailTierLabel = new Label("", createLabelStyle(SMALL, Color.WHITE));
    detailTierFrame = new Image();
    detailTierFrame.setScaling(Scaling.stretch);
    detailDescriptionLabel = new Label("", createLabelStyle(DEFAULT, BODY_COLOUR));
    detailStatsLabel = new Label("", createLabelStyle(DEFAULT, GOLD_COLOUR));
    detailStateLabel.setFontScale(1.1f);
    detailNameLabel.setFontScale(1.25f);
    detailTierLabel.setFontScale(0.85f);
    detailDescriptionLabel.setFontScale(1.15f);
    detailDescriptionLabel.setWrap(true);
    detailStatsLabel.setFontScale(1.15f);

    Table artBackground = new Table();
    artBackground.setBackground(skin.newDrawable(WHITE, new Color(0.035f, 0.03f, 0.04f, 1f)));
    detailImage = new Image();
    detailImage.setScaling(Scaling.fit);
    lockedArtLabel = new Label("?", createLabelStyle(LARGE, MUTED_COLOUR));
    lockedArtLabel.setFontScale(4f);

    Stack artwork = new Stack();
    artwork.add(artBackground);
    artwork.add(detailImage);
    Table lockedLayer = new Table();
    lockedLayer.add(lockedArtLabel).center();
    artwork.add(lockedLayer);

    detailPanel.add(detailStateLabel).left().expandX();
    detailPanel.row();
    detailPanel.add(detailNameLabel).left().padTop(4f);
    detailPanel.row();
    Stack tierBanner = new Stack();
    tierBanner.add(detailTierFrame);
    Table tierText = new Table();
    tierText.add(detailTierLabel).center();
    tierBanner.add(tierText);
    detailPanel.add(tierBanner).width(180f).height(60f).left().padTop(2f);
    detailPanel.row();
    ScrollPane descriptionPane = new ScrollPane(detailDescriptionLabel, skin);
    descriptionPane.setFadeScrollBars(false);
    descriptionPane.setScrollingDisabled(true, false);
    Table detailBody = new Table();
    detailBody.add(artwork).width(220f).height(220f).padRight(18f);
    detailBody.add(descriptionPane).width(370f).height(220f);
    detailPanel.add(detailBody).left().padTop(8f);
    detailPanel.row();
    detailPanel.add(detailStatsLabel).left().padTop(12f);
    return detailPanel;
  }

  void applyFilter(EnemyTier tier) {
    activeTier = tier;
    rebuildEnemyList();
  }

  private void rebuildEnemyList() {
    String selectedEnemyId = displayedEntry == null ? null : displayedEntry.enemyId();
    enemyListTable.clearChildren();
    emptyStateLabel = null;
    List<BestiaryEntryView> filtered = bestiary.getCatalogueEntriesByTier(activeTier);
    if (filtered.isEmpty()) {
      emptyStateLabel = new Label(EMPTY_STATE_MESSAGE, createLabelStyle(SMALL, MUTED_COLOUR));
      emptyStateLabel.setWrap(true);
      enemyListTable.add(emptyStateLabel).width(285f).padTop(24f);
      clearDetails();
      return;
    }

    for (BestiaryEntryView entry : filtered) {
      Button entryButton = createEnemyButton(entry);
      entryButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              showDetails(entry);
            }
          });
      enemyListTable.add(entryButton).width(285f).height(76f).padBottom(8f);
      enemyListTable.row();
    }
    BestiaryEntryView selection =
        filtered.stream()
            .filter(entry -> entry.enemyId().equals(selectedEnemyId))
            .findFirst()
            .orElse(filtered.get(0));
    showDetails(selection);
  }

  private void clearDetails() {
    displayedEntry = null;
    detailStateLabel.setText("NO RECORDS");
    detailNameLabel.setText("No enemy selected");
    detailTierLabel.setText("");
    detailDescriptionLabel.setText("New records will appear here when they become available.");
    detailStatsLabel.setText("");
    detailImage.setDrawable(null);
    detailImage.setVisible(false);
    lockedArtLabel.setVisible(true);
  }

  private void showDetails(BestiaryEntryView entry) {
    displayedEntry = entry;
    detailNameLabel.setText(visibleName(entry));
    detailTierLabel.setText(tierCaption(entry.tier()));
    detailTierLabel.setColor(tierColour(entry.tier()));
    detailTierFrame.setDrawable(tierButtonDrawable(entry.tier(), null));
    detailDescriptionLabel.setText(descriptionFor(entry));
    detailStatsLabel.setText(statsFor(entry));

    if (entry.unlockState() == BestiaryUnlockState.LOCKED) {
      detailStateLabel.setText("UNDISCOVERED");
      detailImage.setDrawable(null);
      detailImage.setVisible(false);
      lockedArtLabel.setVisible(true);
      return;
    }

    detailStateLabel.setText("ARCHIVE RECORD");
    lockedArtLabel.setVisible(false);
    setEnemyImage(entry.sprite().orElse(""));
  }

  private void onEntryUpdated(BestiaryEntryView entry) {
    if (entry.tier() == activeTier && rootTable != null) {
      rebuildEnemyList();
    }
  }

  BestiaryEntryView getDisplayedEntry() {
    return displayedEntry;
  }

  String getEmptyStateText() {
    return emptyStateLabel == null ? null : emptyStateLabel.getText().toString();
  }

  static String visibleName(BestiaryEntryView entry) {
    return entry.displayName();
  }

  private Button createEnemyButton(BestiaryEntryView entry) {
    ButtonStyle style = new ButtonStyle();
    style.up = tierButtonDrawable(entry.tier(), null);
    style.over = tierButtonDrawable(entry.tier(), new Color(1f, 0.9f, 0.8f, 1f));
    style.down = tierButtonDrawable(entry.tier(), new Color(0.8f, 0.7f, 0.7f, 1f));
    Button button = new Button(style);

    Image portrait = new Image();
    AtlasRegion region = enemyRegion(entry.sprite().orElse(""));
    if (region != null) {
      portrait.setDrawable(new TextureRegionDrawable(region));
      portrait.setScaling(Scaling.fit);
    }
    button.add(portrait).size(48f).padLeft(10f).padRight(6f);
    Label name = new Label(visibleName(entry), createLabelStyle(SMALL, BODY_COLOUR));
    name.setFontScale(1.05f);
    button.add(name).expandX().left();
    return button;
  }

  private static Color tierColour(EnemyTier tier) {
    return switch (tier) {
      case NORMAL -> NORMAL_COLOUR;
      case ELITE -> ELITE_COLOUR;
      case BOSS -> BOSS_COLOUR;
    };
  }

  private static String tierCaption(EnemyTier tier) {
    return switch (tier) {
      case NORMAL -> "NORMAL  /  THREAT I";
      case ELITE -> "ELITE  /  THREAT II";
      case BOSS -> "BOSS  /  THREAT III";
    };
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
    AtlasRegion region = enemyRegion(atlasPath);
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

  private AtlasRegion enemyRegion(String atlasPath) {
    ResourceService resources = ServiceLocator.getResourceService();
    String resolvedPath = atlasPath;
    if (resolvedPath.isBlank() || !resources.containsAsset(resolvedPath, TextureAtlas.class)) {
      resolvedPath = FALLBACK_ATLAS;
    }
    if (!resources.containsAsset(resolvedPath, TextureAtlas.class)) {
      return null;
    }

    TextureAtlas atlas = resources.getAsset(resolvedPath, TextureAtlas.class);
    AtlasRegion region = atlas.findRegion(DEFAULT);
    if (region == null) {
      region = atlas.findRegion("idle", 0);
    }
    if (region == null) {
      return null;
    }
    return region;
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
    return frameDrawable(BUTTON_TEXTURE, tint);
  }

  private Drawable tierButtonDrawable(EnemyTier tier, Color tint) {
    String path =
        switch (tier) {
          case NORMAL -> BUTTON_TEXTURE;
          case ELITE -> ELITE_FRAME_TEXTURE;
          case BOSS -> BOSS_FRAME_TEXTURE;
        };
    return frameDrawable(path, tint);
  }

  private Drawable frameDrawable(String path, Color tint) {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
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
    if (rootTable != null) {
      rootTable.remove();
    }
    super.dispose();
  }
}
