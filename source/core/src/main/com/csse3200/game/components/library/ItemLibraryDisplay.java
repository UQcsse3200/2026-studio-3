package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.ItemArtwork;
import com.csse3200.game.ui.ItemIconView;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Map;

/**
 * Read-only catalogue of every {@link ItemType}, mirroring {@link CardLibraryDisplay}'s
 * list-plus-detail layout. Unlike {@link
 * com.csse3200.game.components.battle.InventoryPopupComponent}, this shows all item types
 * regardless of what the player currently owns — it's a reference library, not a live inventory.
 */
public class ItemLibraryDisplay extends UIComponent {
  private static final float PANEL_WIDTH = 1120f;
  private static final float PANEL_HEIGHT = 680f;
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.96f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";

  private static final Map<ItemType, String> ITEM_DESCRIPTIONS = new EnumMap<>(ItemType.class);

  private ItemIconView itemIconView;

  static {
    ITEM_DESCRIPTIONS.put(
        ItemType.ENERGY_CRYSTAL, "+1 max energy per copy, capped at 5 total max energy.");
    ITEM_DESCRIPTIONS.put(
        ItemType.MERCHANTS_FAVOR, "+10% shop discount per copy, stacks up to a 50% cap.");
    ITEM_DESCRIPTIONS.put(
        ItemType.LUCKY_COIN,
        "+10% gold on a GOLD reward claim (current + reward), capped at +20. One copy is used"
            + " per claim.");
    ITEM_DESCRIPTIONS.put(ItemType.IRON_AEGIS, "+5 Armour when used in battle. Single use.");
    ITEM_DESCRIPTIONS.put(ItemType.WARRIORS_CREST, "+1 Strength when used in battle. Single use.");
    ITEM_DESCRIPTIONS.put(
        ItemType.AMBROSIA_FLASK,
        "Restore 20 Health when used in battle, up to max Health. Single use.");
    ITEM_DESCRIPTIONS.put(
        ItemType.BRAND_OF_THE_DAMNED,
        "Every Strength gain is reduced by 1 per copy, to a minimum of 0.");
    ITEM_DESCRIPTIONS.put(
        ItemType.SHATTERED_AEGIS, "Every Block gain is reduced by 1 per copy, to a minimum of 0.");
    ITEM_DESCRIPTIONS.put(
        ItemType.AMBROSIA_FLASK, "Consume during battle to restore 20 Health, up to max health.");
    ITEM_DESCRIPTIONS.put(
        ItemType.ASHEN_EMBER, "Reduces maximum energy by 1 per copy, to a minimum of 1.");
    ITEM_DESCRIPTIONS.put(
        ItemType.BLOOD_OATH_SIGIL,
        "Lose 2 Health at the start of each of your turns per copy. Bypasses Block and"
            + " Armour. Cannot reduce Health below 1.");
    ITEM_DESCRIPTIONS.put(
        ItemType.ETERNAL_VITALITY, "+20 max Health per copy, and fully heal when obtained.");
  }

  private final GdxGame game;

  private Stack rootStack;
  private Label nameLabel;
  private Label descriptionLabel;
  private Label sourceLabel;
  private Table itemListTable;
  private boolean showingCurses;
  private TextButton.TextButtonStyle buttonStyle;

  public static String resolveArtworkPath(ItemType item) {
    return ItemArtwork.framedPath(item);
  }

  public ItemLibraryDisplay(GdxGame game) {
    this.game = game;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);
    addBackground(rootStack);

    buttonStyle = createButtonStyle();

    Table panel = new Table();
    panel.setName("itemLibraryPanel");
    panel.setBackground(skin.newDrawable(WHITE, PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    addDivider(panel);
    panel.row();
    TextButton relicsButton = addFilters(panel);
    panel.row();
    addItemContent(panel);
    relicsButton.setChecked(true);
    applyFilter(false);

    Table wrapper = new Table();
    wrapper.setName("itemLibraryWrapper");
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);
    wrapper.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    rootStack.add(wrapper);
    stage.addActor(rootStack);
  }

  private void addBackground(Stack stack) {
    Texture backgroundTexture = getTexture(MainMenuDisplay.BACKGROUND_TEXTURE);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    stack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.62f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable(WHITE, overlayColour));
    stack.add(overlay);
  }

  private void addHeader(Table panel) {
    Table titleBlock = new Table();
    Label eyebrow = new Label("RELIC ARCHIVE", labelStyle(SMALL, MenuTheme.softCoral()));
    Label title = new Label("Item Library", labelStyle(LARGE, MenuTheme.warmParchment()));
    Label subtitle =
        new Label(
            "Browse every item that can be found during a run.",
            labelStyle(SMALL, MenuTheme.warmParchment()));
    eyebrow.setFontScale(1.15f);
    title.setFontScale(1.35f);
    subtitle.setFontScale(1.05f);

    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);

    TextButton backButton = new TextButton("Back", buttonStyle);
    backButton.getLabel().setFontScale(0.85f);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.LIBRARY);
          }
        });

    Table header = new Table();
    header.add(titleBlock).left().expandX();
    header.add(backButton).right().width(170f).height(58f);
    panel.add(header).colspan(2).expandX().fillX();
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, MenuTheme.softCoral()));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(16f);
  }

  private void addItemContent(Table panel) {
    addItemList(panel);
  }

  private void addItemList(Table panel) {
    itemListTable = new Table();
    itemListTable.top();
    itemListTable.defaults().width(290f).height(76f).padBottom(8f).left();

    ScrollPane scrollPane = new ScrollPane(itemListTable, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    listPanel.add(new Label("ITEMS", labelStyle(SMALL, MenuTheme.softCoral()))).left().expandX();
    listPanel.row();
    listPanel.add(scrollPane).expand().fill().padTop(12f);

    panel.add(listPanel).width(350f).expandY().fillY().padRight(22f);
    panel.add(createDetailPanel()).expand().fill();
  }

  private TextButton addFilters(Table panel) {
    Table filters = new Table();
    filters.defaults().width(185f).height(62f).padRight(12f);
    ButtonGroup<TextButton> filterGroup = new ButtonGroup<>();
    filterGroup.setMinCheckCount(1);
    filterGroup.setMaxCheckCount(1);
    filterGroup.setUncheckLast(true);

    TextButton relicsButton = createFilterButton("Relics", false, filterGroup);
    filters.add(relicsButton);
    filters.add(createFilterButton("Curses", true, filterGroup));

    panel.add(filters).left().colspan(2).padBottom(16f);
    return relicsButton;
  }

  private TextButton createFilterButton(
      String text, boolean curses, ButtonGroup<TextButton> group) {
    TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(buttonStyle);
    style.checked = style.over != null ? style.over : style.down;
    style.checkedFontColor = Color.WHITE;
    TextButton button = new TextButton(text, style);
    button.getLabel().setFontScale(0.85f);
    group.add(button);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (button.isChecked()) {
              applyFilter(curses);
            }
          }
        });
    return button;
  }

  private void applyFilter(boolean curses) {
    showingCurses = curses;
    rebuildItemList();
  }

  private void rebuildItemList() {
    itemListTable.clearChildren();
    ItemType firstItem = null;
    for (ItemType item : ItemType.values()) {
      if (item.isCurse() != showingCurses) {
        continue;
      }
      if (firstItem == null) {
        firstItem = item;
      }
      TextButton itemButton = new TextButton(ItemFormatting.formatItemName(item), buttonStyle);
      itemButton.getLabel().setFontScale(0.6f);
      itemButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              showItem(item);
            }
          });
      itemListTable.add(itemButton).row();
    }
    if (firstItem != null) {
      showItem(firstItem);
    }
  }

  private Table createDetailPanel() {
    itemIconView = new ItemIconView();

    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(24f);
    detailPanel.top();

    nameLabel = new Label("", labelStyle(LARGE, MenuTheme.warmParchment()));
    descriptionLabel = new Label("", bodyLabelStyle());

    nameLabel.setFontScale(1.25f);
    descriptionLabel.setFontScale(1.1f);
    descriptionLabel.setWrap(true);
    sourceLabel = new Label("", bodyLabelStyle());
    sourceLabel.setWrap(true);

    detailPanel.add(nameLabel).left().expandX();
    detailPanel.row();
    detailPanel.add(itemIconView).width(200f).height(200f).padTop(8f).padBottom(16f);
    detailPanel.row();
    detailPanel.add(descriptionLabel).expandX().fillX().left().padTop(12f);
    detailPanel.row();
    detailPanel.add(sourceLabel).expandX().fillX().left().padTop(12f);
    return detailPanel;
  }

  private void showItem(ItemType item) {
    nameLabel.setText(ItemFormatting.formatItemName(item));
    descriptionLabel.setText(ITEM_DESCRIPTIONS.getOrDefault(item, "No description available."));
    sourceLabel.setText("Obtained from: " + ItemFormatting.formatItemSource(item));
    setItemImage(resolveArtworkPath(item));
  }

  private void setItemImage(String texturePath) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (!resources.containsAsset(texturePath, Texture.class)) {
      itemIconView.setItem(null);
      return;
    }
    Texture texture = resources.getAsset(texturePath, Texture.class);
    itemIconView.setItem(texture);
  }

  private Label.LabelStyle bodyLabelStyle() {
    return labelStyle(DEFAULT, MenuTheme.warmParchment());
  }

  private Label.LabelStyle labelStyle(String baseStyle, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(baseStyle, Label.LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private Texture getTexture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  private TextButton.TextButtonStyle createButtonStyle() {
    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    style.font = skin.getFont("font_large");
    style.up = buttonDrawable(null);
    style.over = buttonDrawable(new Color(1f, 0.88f, 0.68f, 1f));
    style.down = buttonDrawable(new Color(0.7f, 0.48f, 0.58f, 1f));
    style.checked = buttonDrawable(new Color(1f, 0.72f, 0.34f, 1f));
    style.checkedOver = buttonDrawable(new Color(1f, 0.86f, 0.55f, 1f));
    style.fontColor = MenuTheme.warmParchment();
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.checkedFontColor = Color.WHITE;
    return style;
  }

  private Drawable buttonDrawable(Color tint) {
    Texture texture = getTexture(CardLibraryDisplay.BUTTON_TEXTURE);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
    drawable.setMinWidth(0f);
    drawable.setMinHeight(0f);
    return tint == null ? drawable : drawable.tint(tint);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Rendering handled by the stage.
  }

  @Override
  public void update() {
    stage.act(ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  public void dispose() {
    if (rootStack != null) {
      rootStack.remove();
      rootStack.clear();
    }
    super.dispose();
  }
}
