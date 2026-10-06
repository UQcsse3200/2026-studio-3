package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/**
 * Battle inventory popup for durable rewards. Passive items are displayed as owned, while battle
 * consumables can be used only during the player's turn.
 */
public class InventoryPopupComponent extends UIComponent {
  private static final Map<ItemType, String> ITEM_ICONS =
      Map.of(
          ItemType.LUCKY_COIN, "images/ui/lucky-coin.png",
          ItemType.ENERGY_CRYSTAL, "images/ui/energy-crystal.png",
          ItemType.MERCHANTS_FAVOR, "images/ui/merchants-favor.png",
          ItemType.IRON_AEGIS, "images/ui/iron-aegis.png",
          ItemType.WARRIORS_CREST, "images/ui/warriors-crest.png");

  private static final Map<ItemType, IntFunction<String>> ITEM_DESCRIPTIONS =
      new EnumMap<>(ItemType.class);

  static {
    ITEM_DESCRIPTIONS.put(ItemType.ENERGY_CRYSTAL, count -> "+" + count + " Max Energy");
    ITEM_DESCRIPTIONS.put(
        ItemType.MERCHANTS_FAVOR,
        count ->
            String.format("+%.0f%% Shop Discount | Max 50%%", Math.min(count * 0.10f, 0.5f) * 100));
    ITEM_DESCRIPTIONS.put(ItemType.LUCKY_COIN, count -> "+" + (count * 10) + "% Gold Reward bonus");
    ITEM_DESCRIPTIONS.put(ItemType.IRON_AEGIS, count -> "Use in battle: +5 Armour");
    ITEM_DESCRIPTIONS.put(ItemType.WARRIORS_CREST, count -> "Use in battle: +1 Strength");
  }

  private static final Color PANEL_COLOUR = new Color(0.09f, 0.07f, 0.09f, 0.96f);
  private static final Color ROW_COLOUR = new Color(0.15f, 0.11f, 0.13f, 1f);
  private static final Color BORDER_COLOUR = new Color(0.55f, 0.35f, 0.28f, 1f);
  private static final Color TEXT_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.72f, 0.65f, 0.58f, 1f);

  private final RunState runState;
  private final PopupDisplay popup;
  private final Entity player;
  private final BooleanSupplier canUseBattleItems;

  public InventoryPopupComponent(
      RunState runState, PopupDisplay popup, Entity player, BooleanSupplier canUseBattleItems) {
    this.runState = runState;
    this.popup = popup;
    this.player = player;
    this.canUseBattleItems = canUseBattleItems;
  }

  @Override
  public void create() {
    super.create();
    popup.setOnShow(this::refresh);
  }

  /** Opens the popup with the latest persistent item state. */
  public void open() {
    refresh();
    popup.show();
  }

  private void refresh() {
    Table content = popup.getContentTable();
    content.clear();
    content.top().pad(8f);
    content.setBackground(skin.newDrawable("white", PANEL_COLOUR));

    PlayerRunState playerState = runState.getOrCreatePlayerState();
    List<ItemType> ownedItems = playerState.getOwnedItems();

    content.add(new Label("ITEM INVENTORY", titleStyle())).center().padBottom(10f).row();
    if (ownedItems.isEmpty()) {
      content.add(new Label("No items collected yet.", detailStyle())).pad(40f).row();
      return;
    }

    Table itemList = new Table();
    itemList.top();
    for (ItemType item : ownedItems.stream().distinct().toList()) {
      itemList.add(createItemRow(playerState, item)).growX().padBottom(6f).row();
    }

    ScrollPane scrollPane = new ScrollPane(itemList, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);
    content.add(scrollPane).width(410f).height(230f).row();
    String turnHint =
        canUseBattleItems.getAsBoolean()
            ? "Consumable items can be used this turn."
            : "Consumable items can only be used on your turn.";
    content.add(new Label(turnHint, detailStyle())).padTop(8f).padBottom(4f);
  }

  private Table createItemRow(PlayerRunState playerState, ItemType item) {
    Table row = new Table();
    row.setBackground(skin.newDrawable("white", ROW_COLOUR));
    row.pad(7f, 9f, 7f, 9f);

    String iconPath = ITEM_ICONS.get(item);
    ResourceService resources = ServiceLocator.getResourceService();
    if (iconPath != null && resources.containsAsset(iconPath, Texture.class)) {
      row.add(new Image(resources.getAsset(iconPath, Texture.class))).size(38f).padRight(10f);
    }

    Table text = new Table();
    text.left();
    text.add(new Label(ItemFormatting.formatItemName(item), nameStyle())).left();
    text.row();
    String description =
        ITEM_DESCRIPTIONS
            .getOrDefault(item, ignored -> "")
            .apply(playerState.getOwnedItemCount(item));
    text.add(new Label(description, detailStyle())).left().padTop(2f);
    row.add(text).left().growX();

    if (item.isBattleConsumable()) {
      row.add(createUseButton(item)).width(64f).height(34f).padLeft(8f);
    } else {
      row.add(new Label("x" + playerState.getOwnedItemCount(item), detailStyle())).padLeft(10f);
    }

    Table border = new Table();
    border.setBackground(skin.newDrawable("white", BORDER_COLOUR));
    border.pad(1f);
    border.add(row).grow();
    return border;
  }

  private TextButton createUseButton(ItemType item) {
    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get("default", TextButton.TextButtonStyle.class));
    style.fontColor = TEXT_COLOUR;
    style.overFontColor = Color.WHITE;
    style.disabledFontColor = DETAIL_COLOUR;
    TextButton button = new TextButton("USE", style);
    button.setDisabled(!canUseBattleItems.getAsBoolean());
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (canUseBattleItems.getAsBoolean()
                && runState.getOrCreatePlayerState().useBattleItem(item, player)) {
              refresh();
            }
          }
        });
    return button;
  }

  private Label.LabelStyle titleStyle() {
    Label.LabelStyle style = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    style.fontColor = TEXT_COLOUR;
    return style;
  }

  private Label.LabelStyle nameStyle() {
    Label.LabelStyle style = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
    style.fontColor = TEXT_COLOUR;
    return style;
  }

  private Label.LabelStyle detailStyle() {
    Label.LabelStyle style = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    style.fontColor = DETAIL_COLOUR;
    return style;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage.
  }
}
