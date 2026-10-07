package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
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
  private static final Color AEGIS_EFFECT_COLOUR = new Color(0.32f, 0.73f, 0.91f, 1f);
  private static final Color CREST_EFFECT_COLOUR = new Color(0.9f, 0.32f, 0.18f, 1f);
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
    ITEM_DESCRIPTIONS.put(ItemType.LUCKY_COIN, count -> "+10% Total Gold on claim  |  Max +20");
    ITEM_DESCRIPTIONS.put(ItemType.IRON_AEGIS, count -> "+5 Armour when used");
    ITEM_DESCRIPTIONS.put(ItemType.WARRIORS_CREST, count -> "+1 Strength when used");
  }

  private final RunState runState;
  private final PopupDisplay popup;
  private final Entity player;
  private final BooleanSupplier canUseBattleItems;
  private boolean useActionInProgress;
  private Actor useFeedback;

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
    popup.setBackgroundTexture(PANEL_TEXTURE);
    popup.setPadding(14f, 22f, 14f, 22f);
    popup.setDefaultCloseButtonVisible(false);
    popup.setOnShow(this::refresh);
    popup.setOnHide(this::clearUseFeedback);
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
    Label name =
        new Label(
            ItemFormatting.formatItemName(itemId) + "  x" + count, createLabelStyle(NAME_COLOUR));
    Label description =
        new Label(
            ITEM_DESCRIPTIONS.containsKey(itemId) ? ITEM_DESCRIPTIONS.get(itemId).apply(count) : "",
            createSmallLabelStyle(DESCRIPTION_COLOUR));
    text.add(name).left();
    text.row();
    text.add(description).left().padTop(2f);
    row.add(text).left().growX();

    if (itemId.isBattleConsumable()) {
      row.add(createUseButton(itemId)).width(66f).height(32f).padLeft(12f);
    } else {
      row.add(createStatusBadge(itemId)).right().padLeft(12f);
    }

    frame.add(row).grow();
    return frame;
  }

  private TextButton createUseButton(ItemType itemId) {
    TextButtonStyle style = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
    style.font = skin.getFont("font_small");
    style.fontColor = GOLD_COLOUR;
    style.overFontColor = NAME_COLOUR;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = DESCRIPTION_COLOUR;

    TextButton useButton = new TextButton("USE", style);
    useButton.setDisabled(useActionInProgress || !canUseBattleItems.getAsBoolean());
    useButton.addListener(new TextTooltip("Use during your turn", skin));
    useButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            if (useActionInProgress || !canUseBattleItems.getAsBoolean()) {
              return;
            }

            if (runState.getOrCreatePlayerState().useBattleItem(itemId, player)) {
              useActionInProgress = true;
              useButton.setDisabled(true);
              showUseFeedback(itemId);
            }
          }
        });
    return useButton;
  }

  private Table createStatusBadge(ItemType itemId) {
    String status = itemId == ItemType.LUCKY_COIN ? "ON GOLD" : "PASSIVE";
    Table border = new Table();
    border.setBackground(skin.newDrawable("white", ROW_BORDER_COLOUR));
    border.pad(2f);

    Table badge = new Table();
    badge.setBackground(skin.newDrawable("white", ROW_BACKGROUND_COLOUR));
    badge.add(new Label(status, createSmallLabelStyle(DESCRIPTION_COLOUR))).pad(5f, 8f, 5f, 8f);
    border.add(badge);
    return border;
  }

  private void showUseFeedback(ItemType itemId) {
    if (useFeedback != null) {
      useFeedback.remove();
    }

    Color effectColour = itemId == ItemType.IRON_AEGIS ? AEGIS_EFFECT_COLOUR : CREST_EFFECT_COLOUR;
    String title = itemId == ItemType.IRON_AEGIS ? "IRON AEGIS USED" : "WARRIOR'S CREST USED";
    String detail = itemId == ItemType.IRON_AEGIS ? "+5 ARMOUR" : "+1 STRENGTH";

    Table border = new Table();
    border.setTransform(true);
    border.setOrigin(Align.center);
    border.setBackground(skin.newDrawable("white", effectColour));
    border.pad(3f);

    Table message = new Table();
    message.setBackground(skin.newDrawable("white", ROW_BACKGROUND_COLOUR));
    String iconPath = ITEM_ICONS.get(itemId);
    if (iconPath != null) {
      Texture texture = ServiceLocator.getResourceService().getAsset(iconPath, Texture.class);
      message.add(new Image(texture)).size(42f).pad(7f, 8f, 7f, 8f);
    }

    Table text = new Table();
    Label titleLabel = new Label(title, createLabelStyle(NAME_COLOUR));
    Label detailLabel = new Label(detail, createSmallLabelStyle(effectColour));
    text.add(titleLabel).left();
    text.row();
    text.add(detailLabel).left().padTop(2f);
    message.add(text).padRight(14f);
    border.add(message);
    border.pack();
    border.setPosition(
        (stage.getWidth() - border.getWidth()) / 2f,
        popup.getWindowY() + popup.getWindowHeight() - border.getHeight() - 18f);
    border.getColor().a = 0f;
    border.setScale(0.86f);
    stage.addActor(border);
    border.toFront();
    useFeedback = border;

    Action steppedPixelEffect =
        Actions.sequence(
            Actions.alpha(1f, 0f),
            Actions.scaleTo(0.92f, 0.92f, 0f),
            Actions.delay(0.05f),
            Actions.scaleTo(1.08f, 1.08f, 0f),
            Actions.delay(0.05f),
            Actions.scaleTo(1f, 1f, 0f),
            Actions.delay(0.12f),
            Actions.run(this::refresh),
            Actions.delay(0.23f),
            Actions.run(
                () -> {
                  useActionInProgress = false;
                  if (popup.isShowing()) {
                    refresh();
                  }
                }),
            Actions.delay(0.75f),
            Actions.alpha(0.45f, 0f),
            Actions.delay(0.07f),
            Actions.alpha(1f, 0f),
            Actions.delay(0.07f),
            Actions.alpha(0f, 0f),
            Actions.removeActor());
    border.addAction(steppedPixelEffect);
  }

  private void clearUseFeedback() {
    if (useFeedback != null) {
      useFeedback.remove();
      useFeedback = null;
    }
    useActionInProgress = false;
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
    String instruction =
        canUseBattleItems.getAsBoolean()
            ? "Combat items ready  |  ESC  Close"
            : "Wait for your turn  |  ESC  Close";
    footer
        .add(new Label(instruction, createSmallLabelStyle(DESCRIPTION_COLOUR)))
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
