package com.csse3200.game.components.bestiary;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.bestiary.BestiaryDataSource;
import com.csse3200.game.bestiary.BestiaryEntry;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Scene2D component that displays the enemy bestiary. */
public class BestiaryDisplay extends UIComponent {
  private static final float Z_INDEX = 3f;
  private static final float PANEL_WIDTH = 1160f;
  private static final Color BACKDROP_COLOUR = new Color(0.018f, 0.012f, 0.02f, 0.92f);
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.98f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.95f, 0.73f, 0.28f, 1f);
  private static final Color BODY_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color MUTED_COLOUR = new Color(0.65f, 0.58f, 0.52f, 1f);

  private final List<BestiaryEntry> entries;
  private final Runnable returnAction;
  private EnemyTier activeTier = EnemyTier.NORMAL;

  private Table rootTable;
  private Table enemyListTable;
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
   * @param dataSource source of presentation-ready enemy entries
   * @param returnAction action invoked by the Back button
   */
  public BestiaryDisplay(BestiaryDataSource dataSource, Runnable returnAction) {
    Objects.requireNonNull(dataSource, "dataSource cannot be null");
    this.entries = List.copyOf(dataSource.getEntries());
    this.returnAction = Objects.requireNonNull(returnAction, "returnAction cannot be null");
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setTouchable(Touchable.enabled);
    rootTable.setBackground(skin.newDrawable("white", BACKDROP_COLOUR));
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
    Label eyebrow = new Label("ENEMY ARCHIVE", createLabelStyle("small", GOLD_COLOUR));
    Label title = new Label("Bestiary", createLabelStyle("large", BODY_COLOUR));
    Label subtitle =
        new Label(
            "Discover enemies to reveal their records.", createLabelStyle("small", MUTED_COLOUR));
    eyebrow.setFontScale(1.2f);
    title.setFontScale(1.45f);
    subtitle.setFontScale(1.15f);
    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);

    TextButton backButton = new TextButton("Back", createButtonStyle());
    backButton.getLabel().setFontScale(1.2f);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            returnAction.run();
          }
        });

    panel.add(titleBlock).left().expandX();
    panel.add(backButton).right().width(180f).height(58f);
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable("white", GOLD_COLOUR));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(14f);
  }

  private TextButton addFilters(Table panel) {
    Table filters = new Table();
    filters.defaults().width(185f).height(52f).padRight(12f);
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
    style.checked = skin.newDrawable("button-pressed", new Color(0.57f, 0.35f, 0.12f, 1f));
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
    listPanel.setBackground(skin.newDrawable("white", LIST_COLOUR));
    listPanel.pad(18f);
    Label listTitle = new Label("ENEMIES", createLabelStyle("small", GOLD_COLOUR));
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
    detailPanel.setBackground(skin.newDrawable("white", DETAIL_COLOUR));
    detailPanel.pad(24f);
    detailPanel.top();

    detailStateLabel = new Label("", createLabelStyle("small", GOLD_COLOUR));
    detailNameLabel = new Label("", createLabelStyle("large", BODY_COLOUR));
    detailTierLabel = new Label("", createLabelStyle("small", MUTED_COLOUR));
    detailDescriptionLabel = new Label("", createLabelStyle("default", BODY_COLOUR));
    detailStatsLabel = new Label("", createLabelStyle("default", GOLD_COLOUR));
    detailStateLabel.setFontScale(1.1f);
    detailNameLabel.setFontScale(1.25f);
    detailTierLabel.setFontScale(1.1f);
    detailDescriptionLabel.setFontScale(1.15f);
    detailDescriptionLabel.setWrap(true);
    detailStatsLabel.setFontScale(1.15f);

    Table artBackground = new Table();
    artBackground.setBackground(skin.newDrawable("white", new Color(0.035f, 0.03f, 0.04f, 1f)));
    detailImage = new Image();
    detailImage.setScaling(Scaling.fit);
    lockedArtLabel = new Label("?", createLabelStyle("large", MUTED_COLOUR));
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
    detailPanel.add(detailTierLabel).left().padTop(4f);
    detailPanel.row();
    detailPanel.add(artwork).width(650f).height(260f).padTop(14f);
    detailPanel.row();
    detailPanel.add(detailDescriptionLabel).left().top().expandX().fillX().padTop(16f);
    detailPanel.row();
    detailPanel.add(detailStatsLabel).left().padTop(16f);
    return detailPanel;
  }

  private void applyFilter(EnemyTier tier) {
    activeTier = tier;
    rebuildEnemyList();
  }

  private void rebuildEnemyList() {
    enemyListTable.clearChildren();
    List<BestiaryEntry> filtered = filterEntries(entries, activeTier);
    if (filtered.isEmpty()) {
      Label empty =
          new Label("No enemies in this category yet.", createLabelStyle("small", MUTED_COLOUR));
      empty.setWrap(true);
      enemyListTable.add(empty).width(285f).padTop(24f);
      clearDetails();
      return;
    }

    for (BestiaryEntry entry : filtered) {
      TextButton entryButton = new TextButton(visibleName(entry), createButtonStyle());
      entryButton.getLabel().setFontScale(1.1f);
      entryButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              showDetails(entry);
            }
          });
      enemyListTable.add(entryButton).width(285f).height(62f).padBottom(10f);
      enemyListTable.row();
    }
    showDetails(filtered.get(0));
  }

  private void clearDetails() {
    detailStateLabel.setText("NO RECORDS");
    detailNameLabel.setText("No enemy selected");
    detailTierLabel.setText("");
    detailDescriptionLabel.setText("New records will appear here when they become available.");
    detailStatsLabel.setText("");
    detailImage.setDrawable(null);
    detailImage.setVisible(false);
    lockedArtLabel.setVisible(true);
  }

  private void showDetails(BestiaryEntry entry) {
    if (!entry.isUnlocked()) {
      detailStateLabel.setText("UNDISCOVERED");
      detailNameLabel.setText("???");
      detailTierLabel.setText(entry.getTier().name());
      detailDescriptionLabel.setText("Encounter this enemy to reveal its record.");
      detailStatsLabel.setText("HP  ???     ATTACK  ???     ARMOUR  ???");
      detailImage.setDrawable(null);
      detailImage.setVisible(false);
      lockedArtLabel.setVisible(true);
      return;
    }

    detailStateLabel.setText(entry.getUnlockState().name());
    detailNameLabel.setText(entry.getDisplayName());
    detailTierLabel.setText(entry.getTier().name());
    detailDescriptionLabel.setText(entry.getDescription());
    detailStatsLabel.setText(
        String.format(
            "HP  %d     ATTACK  %d     ARMOUR  %d",
            entry.getMaxHealth(), entry.getBaseAttack(), entry.getArmour()));
    lockedArtLabel.setVisible(false);
    setEnemyImage(entry.getImagePath());
  }

  private void setEnemyImage(String imagePath) {
    ResourceService resources = ServiceLocator.getResourceService();
    String resolvedPath = imagePath;
    if (!resources.containsAsset(resolvedPath, Texture.class)) {
      resolvedPath = "images/enemies/default.png";
    }
    if (!resources.containsAsset(resolvedPath, Texture.class)) {
      detailImage.setDrawable(null);
      detailImage.setVisible(false);
      lockedArtLabel.setVisible(true);
      return;
    }

    Texture texture = resources.getAsset(resolvedPath, Texture.class);
    detailImage.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));
    detailImage.setVisible(true);
  }

  private LabelStyle createLabelStyle(String baseStyle, Color colour) {
    LabelStyle style = new LabelStyle(skin.get(baseStyle, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private TextButtonStyle createButtonStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable("button", new Color(0.22f, 0.15f, 0.13f, 1f));
    style.over = skin.newDrawable("button", new Color(0.44f, 0.27f, 0.14f, 1f));
    style.down = skin.newDrawable("button-pressed", new Color(0.58f, 0.38f, 0.17f, 1f));
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  static List<BestiaryEntry> filterEntries(List<BestiaryEntry> source, EnemyTier tier) {
    List<BestiaryEntry> matches = new ArrayList<>();
    for (BestiaryEntry entry : source) {
      if (entry.getTier() == tier) {
        matches.add(entry);
      }
    }
    return matches;
  }

  static String visibleName(BestiaryEntry entry) {
    return entry.isUnlocked() ? entry.getDisplayName() : "???";
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
    if (rootTable != null) {
      rootTable.remove();
    }
    super.dispose();
  }
}
