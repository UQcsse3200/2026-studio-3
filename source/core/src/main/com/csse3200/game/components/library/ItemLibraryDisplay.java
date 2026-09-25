package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
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
  private static final String ITEM_ART_DIRECTORY = "images/ui/";

  private static final Map<ItemType, String> ITEM_DESCRIPTIONS = new EnumMap<>(ItemType.class);

  private Image itemImage;

  static {
    ITEM_DESCRIPTIONS.put(
        ItemType.ENERGY_CRYSTAL, "+1 max energy per copy, capped at 5 total max energy.");
    ITEM_DESCRIPTIONS.put(
        ItemType.MERCHANTS_FAVOR, "+10% shop discount per copy, stacks up to a 50% cap.");
    ITEM_DESCRIPTIONS.put(
        ItemType.LUCKY_COIN,
        "Consumed when claiming a GOLD reward: adds 10% of your gold (current + reward), capped"
            + " at +20. Only one copy used per claim.");
    ITEM_DESCRIPTIONS.put(ItemType.IRON_AEGIS, "+5 Armour at the start of each battle, stacks.");
    ITEM_DESCRIPTIONS.put(
        ItemType.WARRIORS_CREST,
        "+1 Strength at the start of each battle. Each stack adds +1 damage to damaging cards.");
  }

  private final GdxGame game;

  private Stack rootStack;
  private Label nameLabel;
  private Label descriptionLabel;
  private TextButton.TextButtonStyle buttonStyle;

  private static String resolveArtworkPath(ItemType item) {
    String fileName = item.name().toLowerCase().replace('_', '-') + ".png";
    return ITEM_ART_DIRECTORY + fileName;
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

    Texture buttonFrameTexture = getTexture(MainMenuDisplay.BUTTON_FRAME_TEXTURE);
    buttonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    buttonStyle = MenuTheme.createButtonStyle(skin, buttonFrameTexture);

    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WHITE, PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    addDivider(panel);
    panel.row();
    addItemContent(panel);

    Table wrapper = new Table();
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

    panel.add(titleBlock).left().expandX();
    panel.add(backButton).right().width(170f).height(58f);
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, MenuTheme.softCoral()));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(16f);
  }

  private void addItemContent(Table panel) {
    ItemType[] items = ItemType.values();
    addItemList(items, panel);
    if (items.length > 0) {
      showItem(items[0]);
    }
  }

  private void addItemList(ItemType[] items, Table panel) {
    Table itemList = new Table();
    itemList.top();
    itemList.defaults().width(290f).height(58f).padBottom(8f).left();

    for (ItemType item : items) {
      TextButton itemButton = new TextButton(ItemFormatting.formatItemName(item), buttonStyle);
      itemButton.getLabel().setFontScale(0.75f);
      itemButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              showItem(item);
            }
          });
      itemList.add(itemButton).row();
    }

    ScrollPane scrollPane = new ScrollPane(itemList, skin);
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

  private Table createDetailPanel() {
    itemImage = new Image();
    itemImage.setScaling(Scaling.fit);

    Table artworkBackground = new Table();
    artworkBackground.setBackground(skin.newDrawable(WHITE, new Color(0.035f, 0.03f, 0.04f, 1f)));
    Stack artwork = new Stack();
    artwork.add(artworkBackground);
    artwork.add(itemImage);

    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(24f);
    detailPanel.top();

    nameLabel = new Label("", labelStyle(LARGE, MenuTheme.warmParchment()));
    descriptionLabel = new Label("", bodyLabelStyle());

    nameLabel.setFontScale(1.25f);
    descriptionLabel.setFontScale(1.1f);
    descriptionLabel.setWrap(true);

    detailPanel.add(nameLabel).left().expandX();
    detailPanel.row();
    detailPanel.add(artwork).width(200f).height(200f).padTop(8f).padBottom(16f);
    detailPanel.row();
    detailPanel.add(descriptionLabel).width(650f).left().padTop(12f);
    return detailPanel;
  }

  private void showItem(ItemType item) {
    nameLabel.setText(ItemFormatting.formatItemName(item));
    descriptionLabel.setText(ITEM_DESCRIPTIONS.getOrDefault(item, "No description available."));
    setItemImage(resolveArtworkPath(item));
  }

  private void setItemImage(String texturePath) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (!resources.containsAsset(texturePath, Texture.class)) {
      itemImage.setDrawable(null);
      itemImage.setVisible(false);
      return;
    }
    Texture texture = resources.getAsset(texturePath, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    itemImage.setDrawable(new TextureRegionDrawable(texture));
    itemImage.setVisible(true);
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
