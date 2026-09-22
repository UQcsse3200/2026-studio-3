package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the items the player currently owns, sourced from {@link
 * com.csse3200.game.maps.PlayerRunState#getOwnedItems()} so it always reflects items that persist
 * across battles.
 */
public class InventoryDisplay extends Displaying {

    private static final Map<ItemType, String> ITEM_DESCRIPTIONS = new EnumMap<>(ItemType.class);

    static {
        ITEM_DESCRIPTIONS.put(ItemType.ENERGY_CRYSTAL, "+1 max energy");
        ITEM_DESCRIPTIONS.put(ItemType.MERCHANTS_FAVOR, "+5% shop discount (caps at 50%)");
        ITEM_DESCRIPTIONS.put(ItemType.LUCKY_COIN, "+10% bonus gold from rewards");
    }

    private final RunState runState;

    public InventoryDisplay(DisplayingRecord rec, RunState runState) {
        super(rec);
        this.runState = runState;
    }

    @Override
    public void create() {
        super.create();
        buildInventoryTable();
    }

    private void buildInventoryTable() {
        List<ItemType> ownedItems = runState.getOrCreatePlayerState().getOwnedItems();

        Table listTable = new Table();
        listTable.top();

        if (ownedItems.isEmpty()) {
            listTable.add(new Label("No items yet", skin)).pad(10f);
        } else {
            for (ItemType itemId : ownedItems) {
                listTable.add(buildItemRow(itemId)).pad(8f).fillX();
                listTable.row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();
        rootTable.padTop(120f);
        rootTable.add(new Label("Inventory", skin)).padBottom(20f);
        rootTable.row();
        rootTable.add(scrollPane).width(400f).height(500f);

        stage.addActor(rootTable);
    }

    private Table buildItemRow(ItemType itemId) {
        Table row = new Table();
        row.add(new Label(formatItemName(itemId), skin)).left().padRight(10f);
        row.add(new Label(ITEM_DESCRIPTIONS.getOrDefault(itemId, ""), skin)).left();
        return row;
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
        // positioning handled by the table layout
    }
}