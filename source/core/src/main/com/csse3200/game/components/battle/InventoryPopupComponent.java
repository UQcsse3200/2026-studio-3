package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * A popup UI (hosted in a {@link PopupDisplay}) that shows the items the player currently owns,
 * sourced from {@link com.csse3200.game.maps.PlayerRunState#getOwnedItems()} so it always reflects
 * items that persist across battles. Mirrors {@link DeckEditorComponent}'s popup-hosted pattern,
 * but — unlike the deck editor's draggable card grid — item rows have no drag/drop interaction, so
 * they're built directly as simple widgets inside the popup's own content table rather than as
 * stage-level actors positioned against the window's bounds.
 */
public class InventoryPopupComponent extends UIComponent {

  private static final Map<ItemType, IntFunction<String>> ITEM_DESCRIPTIONS =
      new EnumMap<>(ItemType.class);

  private LabelStyle createLabelStyle(Color colour) {
    LabelStyle style = new LabelStyle(skin.get("default", LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private static final Color NAME_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color DESCRIPTION_COLOUR = new Color(0.65f, 0.58f, 0.52f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.83f, 0.61f, 0.27f, 1f);
  private static final String PANEL_TEXTURE = "images/ui/inventory-panel.png";
  private static final Color ROW_BORDER_COLOUR = new Color(0.33f, 0.25f, 0.25f, 1f);
  private static final Color ROW_BACKGROUND_COLOUR = new Color(0.055f, 0.05f, 0.065f, 0.96f);
  private static final Map<ItemType, String> ITEM_ICONS =
      Map.of(
          ItemType.LUCKY_COIN, "images/ui/lucky-coin.png",
          ItemType.ENERGY_CRYSTAL, "images/ui/energy-crystal.png",
          ItemType.MERCHANTS_FAVOR, "images/ui/merchants-favor.png",
          ItemType.IRON_AEGIS, "images/ui/iron-aegis.png",
          ItemType.WARRIORS_CREST, "images/ui/warriors-crest.png");

  static {
    ITEM_DESCRIPTIONS.put(ItemType.ENERGY_CRYSTAL, count -> "+" + count + " Max Energy");
    ITEM_DESCRIPTIONS.put(
        ItemType.MERCHANTS_FAVOR,
        count -> {
          float discount = Math.min(count * 0.10f, 0.5f);
          return String.format("+%.0f%% Shop Discount  |  Max 50%%", discount * 100);
        });
    ITEM_DESCRIPTIONS.put(ItemType.LUCKY_COIN, count -> "+" + (count * 10) + "% Gold Rewards");
    ITEM_DESCRIPTIONS.put(ItemType.IRON_AEGIS, count -> "+" + (count * 5) + " Starting Armour");
    ITEM_DESCRIPTIONS.put(ItemType.WARRIORS_CREST, count -> "+" + count + " Starting Strength");
  }

  private final RunState runState;
  private final PopupDisplay popup;

  public InventoryPopupComponent(RunState runState, PopupDisplay popup) {
    this.runState = runState;
    this.popup = popup;
  }

  @Override
  public void create() {
    super.create();
    popup.setBackgroundTexture(PANEL_TEXTURE);
    popup.setPadding(14f, 22f, 14f, 22f);
    popup.setDefaultCloseButtonVisible(false);
    popup.setOnShow(this::refresh);
  }

  /** Opens the popup with the item list refreshed from the current run state. */
  public void open() {
    refresh();
    popup.show();
  }

  private void refresh() {
    Table content = popup.getContentTable();
    content.clear();
    content.top().pad(2f, 5f, 5f, 5f);

    var playerState = runState.getOrCreatePlayerState();
    List<ItemType> ownedItems = playerState.getOwnedItems();

    List<ItemType> itemTypes = distinctInOrder(ownedItems);
    Table header = new Table();
    header.add().width(30f);
    header.add(new Label("INVENTORY", createTitleStyle())).expandX().center();
    header.add(createCloseButton()).size(30f);
    content.add(header).growX().padBottom(1f);
    content.row();
    Label summary =
        new Label(
            ownedItems.size() + " Items  |  " + itemTypes.size() + " Types",
            createSmallLabelStyle(DESCRIPTION_COLOUR));
    content.add(summary).center().padBottom(5f).growX();
    content.row();
    content.add(divider()).height(1f).growX().padBottom(5f);
    content.row();

    if (ownedItems.isEmpty()) {
      Label emptyLabel = new Label("No items yet", createLabelStyle(DESCRIPTION_COLOUR));
      content.add(emptyLabel).expand().center().pad(60f);
      return;
    }

    Table itemList = new Table();
    itemList.top();
    for (ItemType itemId : itemTypes) {
      int count = playerState.getOwnedItemCount(itemId);
      itemList.add(createItemRow(itemId, count)).growX().padBottom(4f);
      itemList.row();
    }

    ScrollPane scrollPane = new ScrollPane(itemList, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);
    content.add(scrollPane).grow().minHeight(190f);
    content.row();

    content.add(createFooter()).center().padTop(4f);
  }

  private Table createItemRow(ItemType itemId, int count) {
    Table frame = new Table();
    frame.setBackground(skin.newDrawable("white", ROW_BORDER_COLOUR));
    frame.pad(2f);

    Table row = new Table();
    row.setBackground(skin.newDrawable("white", ROW_BACKGROUND_COLOUR));
    row.pad(6f, 10f, 6f, 10f);

    String iconPath = ITEM_ICONS.get(itemId);
    if (iconPath != null) {
      Texture texture = ServiceLocator.getResourceService().getAsset(iconPath, Texture.class);
      row.add(new Image(texture)).size(46f).padRight(12f);
    }

    Table text = new Table();
    text.left();
    Label name = new Label(ItemFormatting.formatItemName(itemId), createLabelStyle(NAME_COLOUR));
    Label description =
        new Label(
            ITEM_DESCRIPTIONS.containsKey(itemId) ? ITEM_DESCRIPTIONS.get(itemId).apply(count) : "",
            createSmallLabelStyle(DESCRIPTION_COLOUR));
    text.add(name).left();
    text.row();
    text.add(description).left().padTop(2f);
    row.add(text).left().growX();

    Table badgeFrame = new Table();
    badgeFrame.setBackground(skin.newDrawable("white", ROW_BORDER_COLOUR));
    badgeFrame.pad(2f);
    Table badge = new Table();
    badge.setBackground(skin.newDrawable("white", ROW_BACKGROUND_COLOUR));
    badge.add(new Label("x" + count, createSmallLabelStyle(NAME_COLOUR))).pad(3f, 8f, 3f, 8f);
    badgeFrame.add(badge);
    row.add(badgeFrame).right().padLeft(18f);

    frame.add(row).grow();
    return frame;
  }

  private LabelStyle createTitleStyle() {
    LabelStyle style = createLabelStyle(NAME_COLOUR);
    style.font = skin.getFont("font");
    return style;
  }

  private LabelStyle createSmallLabelStyle(Color colour) {
    LabelStyle style = createLabelStyle(colour);
    style.font = skin.getFont("font_small");
    return style;
  }

  private TextButton createCloseButton() {
    TextButtonStyle style = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
    style.fontColor = NAME_COLOUR;
    style.up = skin.newDrawable("white", ROW_BACKGROUND_COLOUR);
    style.over = skin.newDrawable("white", ROW_BORDER_COLOUR);
    TextButton close = new TextButton("X", style);
    close.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            popup.hide();
          }
        });
    return close;
  }

  private Table createFooter() {
    Table footer = new Table();
    footer.add(new Image(skin.newDrawable("white", ROW_BORDER_COLOUR))).width(42f).height(1f);
    footer
        .add(new Label("ESC  Close", createSmallLabelStyle(DESCRIPTION_COLOUR)))
        .padLeft(8f)
        .padRight(8f);
    footer.add(new Image(skin.newDrawable("white", ROW_BORDER_COLOUR))).width(42f).height(1f);
    return footer;
  }

  private Image divider() {
    return new Image(skin.newDrawable("white", ROW_BORDER_COLOUR));
  }

  /** Returns each distinct item type once, in the order it was first acquired. */
  private static List<ItemType> distinctInOrder(List<ItemType> items) {
    return items.stream().distinct().toList();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage; nothing to do per-frame here.
  }
}
