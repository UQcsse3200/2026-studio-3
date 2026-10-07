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
 * Popup displaying the player's persistent inventory.
 *
 * <p>Items are sourced from PlayerRunState so inventory remains consistent across battles.
 * Battle consumables can be used during the player's turn.
 */
public class InventoryPopupComponent extends UIComponent {

    private static final String PANEL_TEXTURE = "images/ui/inventory-panel.png";

    private static final Color NAME_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
    private static final Color DESCRIPTION_COLOUR = new Color(0.65f, 0.58f, 0.52f, 1f);
    private static final Color GOLD_COLOUR = new Color(0.83f, 0.61f, 0.27f, 1f);
    private static final Color AEGIS_EFFECT_COLOUR = new Color(0.32f, 0.73f, 0.91f, 1f);
    private static final Color CREST_EFFECT_COLOUR = new Color(0.9f, 0.32f, 0.18f, 1f);
    private static final Color PANEL_COLOUR = new Color(0.09f, 0.07f, 0.09f, 0.96f);
    private static final Color ROW_BORDER_COLOUR = new Color(0.33f, 0.25f, 0.25f, 1f);
    private static final Color ROW_BACKGROUND_COLOUR = new Color(0.055f, 0.05f, 0.065f, 0.96f);
    private static final Color ROW_COLOUR = new Color(0.15f, 0.11f, 0.13f, 1f);
    private static final Color BORDER_COLOUR = new Color(0.55f, 0.35f, 0.28f, 1f);
    private static final Color TEXT_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
    private static final Color DETAIL_COLOUR = new Color(0.72f, 0.65f, 0.58f, 1f);

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
        ITEM_DESCRIPTIONS.put(
                ItemType.ENERGY_CRYSTAL,
                count -> "+" + count + " Max Energy");

        ITEM_DESCRIPTIONS.put(
                ItemType.MERCHANTS_FAVOR,
                count -> {
                    float discount = Math.min(count * 0.10f, 0.5f);
                    return String.format(
                            "+%.0f%% Shop Discount | Max 50%%", discount * 100);
                });

        // Lucky Coins provide a non-stacking bonus per reward claim.
        // Additional coins are retained for later gold rewards.
        ITEM_DESCRIPTIONS.put(
                ItemType.LUCKY_COIN,
                count -> "+10% Gold Reward bonus per claim (max +20 gold)");

        ITEM_DESCRIPTIONS.put(
                ItemType.IRON_AEGIS,
                count -> "Use in battle: +5 Armour");

        ITEM_DESCRIPTIONS.put(
                ItemType.WARRIORS_CREST,
                count -> "Use in battle: +1 Strength");
    }

    private final RunState runState;
    private final PopupDisplay popup;
    private final Entity player;
    private final BooleanSupplier canUseBattleItems;

    private boolean useActionInProgress;
    private Actor useFeedback;

    public InventoryPopupComponent(
            RunState runState,
            PopupDisplay popup,
            Entity player,
            BooleanSupplier canUseBattleItems) {
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

    /** Opens the popup with the inventory refreshed from the current run state. */
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
        List<ItemType> itemTypes = distinctInOrder(ownedItems);

        Table header = new Table();
        header.add().width(30f);
        header.add(new Label("INVENTORY", titleStyle())).expandX().center();
        header.add(createCloseButton()).size(30f);

        content.add(header).growX().padBottom(1f).row();

        Label summary =
                new Label(
                        ownedItems.size() + " Items | " + itemTypes.size() + " Types",
                        detailStyle());

        content.add(summary).center().padBottom(5f).growX().row();
        content.add(divider()).height(1f).growX().padBottom(5f).row();

        content.add(new Label("ITEM INVENTORY", titleStyle()))
                .center()
                .padBottom(10f)
                .row();

        if (ownedItems.isEmpty()) {
            content.add(new Label("No items collected yet.", detailStyle()))
                    .pad(40f)
                    .row();
            return;
        }

        Table itemList = new Table();
        itemList.top();

        for (ItemType item : itemTypes) {
            itemList.add(createItemRow(playerState, item))
                    .growX()
                    .padBottom(6f)
                    .row();
        }

        ScrollPane scrollPane = new ScrollPane(itemList, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        content.add(scrollPane).width(410f).height(230f).row();

        String turnHint =
                canUseBattleItems.getAsBoolean()
                        ? "Consumable items can be used this turn."
                        : "Consumable items can only be used on your turn.";

        content.add(new Label(turnHint, detailStyle()))
                .padTop(8f)
                .padBottom(4f);
    }

    private Table createItemRow(PlayerRunState playerState, ItemType item) {
        int count = playerState.getOwnedItemCount(item);

        Table frame = new Table();
        frame.setBackground(skin.newDrawable("white", BORDER_COLOUR));
        frame.pad(1f);

        Table row = new Table();
        row.setBackground(skin.newDrawable("white", ROW_COLOUR));
        row.pad(7f, 9f, 7f, 9f);

        String iconPath = ITEM_ICONS.get(item);
        ResourceService resources = ServiceLocator.getResourceService();

        if (iconPath != null
                && resources != null
                && resources.containsAsset(iconPath, Texture.class)) {
            row.add(new Image(resources.getAsset(iconPath, Texture.class)))
                    .size(38f)
                    .padRight(10f);
        }

        Table text = new Table();
        text.left();

        text.add(
                        new Label(
                                ItemFormatting.formatItemName(item),
                                nameStyle()))
                .left()
                .row();

        String description =
                ITEM_DESCRIPTIONS
                        .getOrDefault(item, ignored -> "")
                        .apply(count);

        text.add(new Label(description, detailStyle()))
                .left()
                .padTop(2f);

        row.add(text).left().growX();

        if (item.isBattleConsumable()) {
            row.add(createUseButton(item))
                    .width(66f)
                    .height(34f)
                    .padLeft(8f);
        } else {
            row.add(createStatusBadge(item))
                    .padLeft(10f);
        }

        frame.add(row).grow();
        return frame;
    }

    private Table createStatusBadge(ItemType item) {
        String status = item == ItemType.LUCKY_COIN ? "ON GOLD" : "PASSIVE";

        Table border = new Table();
        border.setBackground(skin.newDrawable("white", ROW_BORDER_COLOUR));
        border.pad(2f);

        Table badge = new Table();
        badge.setBackground(skin.newDrawable("white", ROW_BACKGROUND_COLOUR));
        badge.add(new Label(status, smallLabelStyle(DESCRIPTION_COLOUR)))
                .pad(5f, 8f, 5f, 8f);

        border.add(badge);
        return border;
    }

    private TextButton createUseButton(ItemType item) {
        TextButtonStyle style =
                new TextButtonStyle(skin.get("default", TextButtonStyle.class));

        style.font = skin.getFont("font_small");
        style.fontColor = GOLD_COLOUR;
        style.overFontColor = NAME_COLOUR;
        style.downFontColor = Color.WHITE;
        style.disabledFontColor = DESCRIPTION_COLOUR;

        TextButton button = new TextButton("USE", style);
        button.setDisabled(
                useActionInProgress || !canUseBattleItems.getAsBoolean());

        button.addListener(new TextTooltip("Use during your turn", skin));

        button.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (useActionInProgress || !canUseBattleItems.getAsBoolean()) {
                            return;
                        }

                        boolean used =
                                runState
                                        .getOrCreatePlayerState()
                                        .useBattleItem(item, player);

                        if (used) {
                            useActionInProgress = true;
                            button.setDisabled(true);
                            showUseFeedback(item);
                        }
                    }
                });

        return button;
    }

    private void showUseFeedback(ItemType itemId) {
        if (useFeedback != null) {
            useFeedback.remove();
            useFeedback = null;
        }

        Color effectColour =
                itemId == ItemType.IRON_AEGIS
                        ? AEGIS_EFFECT_COLOUR
                        : CREST_EFFECT_COLOUR;

        String title =
                itemId == ItemType.IRON_AEGIS
                        ? "IRON AEGIS USED"
                        : "WARRIOR'S CREST USED";

        String detail =
                itemId == ItemType.IRON_AEGIS
                        ? "+5 ARMOUR"
                        : "+1 STRENGTH";

        Table border = new Table();
        border.setTransform(true);
        border.setOrigin(Align.center);
        border.setBackground(skin.newDrawable("white", effectColour));
        border.pad(3f);

        Table message = new Table();
        message.setBackground(skin.newDrawable("white", ROW_BACKGROUND_COLOUR));

        String iconPath = ITEM_ICONS.get(itemId);
        ResourceService resources = ServiceLocator.getResourceService();

        if (iconPath != null
                && resources != null
                && resources.containsAsset(iconPath, Texture.class)) {
            Texture texture = resources.getAsset(iconPath, Texture.class);
            message.add(new Image(texture))
                    .size(42f)
                    .pad(7f, 8f, 7f, 8f);
        }

        Table text = new Table();

        Label titleLabel =
                new Label(title, labelStyle(NAME_COLOUR));

        Label detailLabel =
                new Label(detail, smallLabelStyle(effectColour));

        text.add(titleLabel).left().row();
        text.add(detailLabel).left().padTop(2f);

        message.add(text).padRight(14f);
        border.add(message);

        border.pack();
        border.setPosition(
                (stage.getWidth() - border.getWidth()) / 2f,
                popup.getWindowY()
                        + popup.getWindowHeight()
                        - border.getHeight()
                        - 18f);

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

    private LabelStyle labelStyle(Color colour) {
        LabelStyle style =
                new LabelStyle(skin.get("default", LabelStyle.class));
        style.fontColor = colour;
        return style;
    }

    private LabelStyle titleStyle() {
        LabelStyle style =
                new LabelStyle(skin.get("large", LabelStyle.class));
        style.fontColor = TEXT_COLOUR;
        return style;
    }

    private LabelStyle nameStyle() {
        LabelStyle style =
                new LabelStyle(skin.get("default", LabelStyle.class));
        style.fontColor = TEXT_COLOUR;
        return style;
    }

    private LabelStyle detailStyle() {
        return smallLabelStyle(DETAIL_COLOUR);
    }

    private LabelStyle smallLabelStyle(Color colour) {
        LabelStyle style =
                new LabelStyle(skin.get("small", LabelStyle.class));
        style.fontColor = colour;
        return style;
    }

    private TextButton createCloseButton() {
        TextButtonStyle style =
                new TextButtonStyle(skin.get("default", TextButtonStyle.class));

        style.fontColor = NAME_COLOUR;
        style.up = skin.newDrawable("white", ROW_BACKGROUND_COLOUR);
        style.over = skin.newDrawable("white", ROW_BORDER_COLOUR);

        TextButton close = new TextButton("X", style);

        close.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        popup.hide();
                    }
                });

        return close;
    }

    private Image divider() {
        return new Image(skin.newDrawable("white", ROW_BORDER_COLOUR));
    }

    /** Returns each distinct item type once, preserving acquisition order. */
    private static List<ItemType> distinctInOrder(List<ItemType> items) {
        return items.stream().distinct().toList();
    }

    @Override
    protected void draw(SpriteBatch batch) {
        // Actors are drawn by the stage.
    }
}
