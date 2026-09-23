package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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
  private static final Color PANEL_COLOUR = new Color(0.055f, 0.045f, 0.065f, 0.98f);
  private static final Color ROW_COLOUR = new Color(0.095f, 0.075f, 0.09f, 1f);
  private static final Map<ItemType, String> ITEM_ICONS =
      Map.of(
          ItemType.LUCKY_COIN, "images/money.png",
          ItemType.ENERGY_CRYSTAL, "images/energy.png",
          ItemType.MERCHANTS_FAVOR, "images/piety.png");

  static {
    ITEM_DESCRIPTIONS.put(ItemType.ENERGY_CRYSTAL, count -> "+" + count + " max energy");
    ITEM_DESCRIPTIONS.put(
        ItemType.MERCHANTS_FAVOR,
        count -> {
          float discount = Math.min(count * 0.10f, 0.5f);
          return String.format("+%.0f%% shop discount (caps at 50%%)", discount * 100);
        });
    ITEM_DESCRIPTIONS.put(
        ItemType.LUCKY_COIN, count -> "+" + (count * 10) + "% bonus gold from rewards");
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
    popup.setBackgroundColour(PANEL_COLOUR);
    popup.setTitleStyle(NAME_COLOUR, "font_large");
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
    content.top().pad(4f, 8f, 8f, 8f);

    var playerState = runState.getOrCreatePlayerState();
    List<ItemType> ownedItems = playerState.getOwnedItems();

    List<ItemType> itemTypes = distinctInOrder(ownedItems);
    Label summary =
        new Label(
            ownedItems.size() + " Items  |  " + itemTypes.size() + " Types",
            createLabelStyle(DESCRIPTION_COLOUR));
    content.add(summary).left().padBottom(8f).growX();
    content.row();
    content.add(divider()).height(2f).growX().padBottom(12f);
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
      itemList.add(createItemRow(itemId, count)).growX().padBottom(10f);
      itemList.row();
    }

    ScrollPane scrollPane = new ScrollPane(itemList, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);
    content.add(scrollPane).grow().minHeight(280f);
    content.row();

    Label footer = new Label("ESC  Close", createLabelStyle(DESCRIPTION_COLOUR));
    content.add(footer).right().padTop(8f);
  }

  private Table createItemRow(ItemType itemId, int count) {
    Table border = new Table();
    border.setBackground(skin.newDrawable("white", GOLD_COLOUR));
    border.pad(2f);

    Table row = new Table();
    row.setBackground(skin.newDrawable("white", ROW_COLOUR));
    row.pad(12f);

    String iconPath = ITEM_ICONS.get(itemId);
    if (iconPath != null) {
      Texture texture = ServiceLocator.getResourceService().getAsset(iconPath, Texture.class);
      row.add(new Image(texture)).size(52f).padRight(16f);
    }

    Table text = new Table();
    text.left();
    Label name = new Label(ItemFormatting.formatItemName(itemId), createLabelStyle(NAME_COLOUR));
    Label description =
        new Label(
            ITEM_DESCRIPTIONS.containsKey(itemId) ? ITEM_DESCRIPTIONS.get(itemId).apply(count) : "",
            createLabelStyle(DESCRIPTION_COLOUR));
    text.add(name).left();
    text.row();
    text.add(description).left().padTop(5f);
    row.add(text).left().growX();

    Label quantity = new Label("x" + count, createLabelStyle(GOLD_COLOUR));
    row.add(quantity).right().padLeft(18f);
    border.add(row).grow();
    return border;
  }

  private Image divider() {
    return new Image(skin.newDrawable("white", GOLD_COLOUR));
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
