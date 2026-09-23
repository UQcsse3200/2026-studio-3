package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
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
    popup.setBackgroundColour(new Color(0.105f, 0.07f, 0.065f, 0.98f));
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

    var playerState = runState.getOrCreatePlayerState();
    List<ItemType> ownedItems = playerState.getOwnedItems();

    if (ownedItems.isEmpty()) {
      Label emptyLabel = new Label("No items yet", createLabelStyle(DESCRIPTION_COLOUR));
      content.add(emptyLabel).pad(10f);
      return;
    }

    for (ItemType itemId : distinctInOrder(ownedItems)) {
      int count = playerState.getOwnedItemCount(itemId);
      String name = ItemFormatting.formatItemName(itemId);
      String labelText = count > 1 ? name + " x" + count : name;
      String description =
          ITEM_DESCRIPTIONS.containsKey(itemId) ? ITEM_DESCRIPTIONS.get(itemId).apply(count) : "";

      Label nameLabel = new Label(labelText, createLabelStyle(NAME_COLOUR));
      Label descriptionLabel = new Label(description, createLabelStyle(DESCRIPTION_COLOUR));

      content.add(nameLabel).left().padRight(20f).padTop(6f);
      content.add(descriptionLabel).left().padTop(6f);
      content.row();
    }
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
