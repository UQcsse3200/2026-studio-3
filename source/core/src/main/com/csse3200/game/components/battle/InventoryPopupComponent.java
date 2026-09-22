package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A popup UI (hosted in a {@link PopupDisplay}) that shows the items the player currently owns,
 * sourced from {@link com.csse3200.game.maps.PlayerRunState#getOwnedItems()} so it always reflects
 * items that persist across battles. Mirrors {@link DeckEditorComponent}'s popup-hosted pattern,
 * but — unlike the deck editor's draggable card grid — item rows have no drag/drop interaction, so
 * they're built directly as simple widgets inside the popup's own content table rather than as
 * stage-level actors positioned against the window's bounds.
 */
public class InventoryPopupComponent extends UIComponent {

  private static final Map<ItemType, String> ITEM_DESCRIPTIONS = new EnumMap<>(ItemType.class);

  static {
    ITEM_DESCRIPTIONS.put(ItemType.ENERGY_CRYSTAL, "+1 max energy");
    ITEM_DESCRIPTIONS.put(ItemType.MERCHANTS_FAVOR, "+5% shop discount (caps at 50%)");
    ITEM_DESCRIPTIONS.put(ItemType.LUCKY_COIN, "+10% bonus gold from rewards");
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

    List<ItemType> ownedItems = runState.getOrCreatePlayerState().getOwnedItems();

    if (ownedItems.isEmpty()) {
      content.add(new Label("No items yet", skin)).pad(10f);
      return;
    }

    for (ItemType itemId : ownedItems) {
      content.add(new Label(formatItemName(itemId), skin)).left().padRight(20f).padTop(6f);
      content.add(new Label(ITEM_DESCRIPTIONS.getOrDefault(itemId, ""), skin)).left().padTop(6f);
      content.row();
    }
  }

  private String formatItemName(ItemType itemId) {
    String[] words = itemId.name().split("_");
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (!result.isEmpty()) {
        result.append(' ');
      }
      result.append(word.charAt(0)).append(word.substring(1).toLowerCase());
    }
    return result.toString();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage; nothing to do per-frame here.
  }
}
