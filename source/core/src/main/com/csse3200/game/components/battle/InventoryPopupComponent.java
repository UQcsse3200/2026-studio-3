package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.CursePenalties;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.ui.ItemArtwork;
import com.csse3200.game.ui.ItemIconView;
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
 * <p>Items are sourced from PlayerRunState so inventory remains consistent across battles. Battle
 * consumables can be used during the player's turn.
 */
public class InventoryPopupComponent extends UIComponent {

  private record UseFeedbackDetails(Color colour, String title, String detail) {}

  private static final String PANEL_TEXTURE = "images/ui/inventory-panel.png";
  private static final float WINDOW_WIDTH = 590f;
  private static final float CONTENT_WIDTH = 530f;
  private static final float LIST_WIDTH = 518f;
  private static final float ROW_HEIGHT = 94f;
  private static final float ROW_SPACING = 6f;
  private static final int MAX_VISIBLE_ROWS = 3;
  private static final float TEXT_WIDTH = 308f;
  private static final float ACTION_WIDTH = 112f;

  private static final Color NAME_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.83f, 0.61f, 0.27f, 1f);
  private static final Color AEGIS_EFFECT_COLOUR = new Color(0.32f, 0.73f, 0.91f, 1f);
  private static final Color CREST_EFFECT_COLOUR = new Color(0.9f, 0.32f, 0.18f, 1f);
  private static final Color AMBROSIA_EFFECT_COLOUR = new Color(0.8f, 0.25f, 0.24f, 1f);
  private static final Color PANEL_COLOUR = new Color(0.09f, 0.07f, 0.09f, 0.96f);
  private static final Color ROW_BORDER_COLOUR = new Color(0.33f, 0.25f, 0.25f, 1f);
  private static final Color ROW_BACKGROUND_COLOUR = new Color(0.055f, 0.05f, 0.065f, 0.96f);
  private static final Color ROW_COLOUR = new Color(0.15f, 0.11f, 0.13f, 1f);
  private static final Color BORDER_COLOUR = new Color(0.55f, 0.35f, 0.28f, 1f);
  private static final Color TEXT_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.72f, 0.65f, 0.58f, 1f);
  private static final Color CURSE_COLOUR = new Color(0.91f, 0.52f, 0.45f, 1f);
  private static final Color CURSE_CRACK_COLOUR = new Color(0.52f, 0.25f, 0.72f, 1f);
  private static final Color ACTION_COLOUR = new Color(0.97f, 0.80f, 0.42f, 1f);
  private static final Color DISABLED_ACTION_COLOUR = new Color(0.48f, 0.46f, 0.45f, 1f);

  record ItemText(String effect) {}

  private static final Map<ItemType, IntFunction<ItemText>> ITEM_DESCRIPTIONS =
      new EnumMap<>(ItemType.class);

  static {
    ITEM_DESCRIPTIONS.put(
        ItemType.ENERGY_CRYSTAL, count -> new ItemText("+" + count + " MAX ENERGY (CAP 5)"));

    ITEM_DESCRIPTIONS.put(
        ItemType.MERCHANTS_FAVOR,
        count -> {
          int discount = Math.clamp(count, 0, 5) * 10;
          return new ItemText("+" + discount + "% SHOP DISCOUNT");
        });

    // Lucky Coins provide a non-stacking bonus per reward claim.
    // Additional coins are retained for later gold rewards.
    ITEM_DESCRIPTIONS.put(ItemType.LUCKY_COIN, count -> new ItemText("+10% GOLD (MAX +20)"));

    ITEM_DESCRIPTIONS.put(ItemType.IRON_AEGIS, count -> new ItemText("+5 ARMOUR"));

    ITEM_DESCRIPTIONS.put(ItemType.WARRIORS_CREST, count -> new ItemText("+1 STRENGTH"));

    ITEM_DESCRIPTIONS.put(
        ItemType.ASHEN_EMBER, count -> new ItemText("-" + count + " MAX ENERGY (MIN 1)"));

    ITEM_DESCRIPTIONS.put(
        ItemType.BRAND_OF_THE_DAMNED, count -> new ItemText("STRENGTH GAINS -" + count));

    ITEM_DESCRIPTIONS.put(ItemType.SHATTERED_AEGIS, count -> new ItemText("BLOCK GAINS -" + count));

    ITEM_DESCRIPTIONS.put(ItemType.AMBROSIA_FLASK, count -> new ItemText("RESTORE 20 HEALTH"));
    ITEM_DESCRIPTIONS.put(
        ItemType.BLOOD_OATH_SIGIL, count -> new ItemText("-" + (count * 2) + " HEALTH / TURN"));
    ITEM_DESCRIPTIONS.put(
        ItemType.ETERNAL_VITALITY, count -> new ItemText("+" + (count * 20) + " MAX HEALTH"));
  }

  static ItemText describeItem(ItemType item, int count) {
    return ITEM_DESCRIPTIONS.getOrDefault(item, ignored -> new ItemText("")).apply(count);
  }

  private final RunState runState;
  private final PopupDisplay popup;
  private final Entity player;
  private final BooleanSupplier canUseBattleItems;

  private boolean useActionInProgress;
  private Actor useFeedback;
  private Actor curseGainFeedback;
  private final EventListener1<CursePenalties.GainReduction> curseGainListener =
      this::showCurseGainFeedback;
  private static final String STYLE_NAME_WHITE = "white";
  private static final String STYLE_NAME_DEFAULT = "default";
  private static final String STYLE_NAME_SMALL = "small";
  private static final String STYLE_NAME_LARGE = "large";
  private static final String FONT_SMALL = "font_small";

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
    popup.setMinSize(WINDOW_WIDTH, 0f);
    popup.setDefaultCloseButtonVisible(false);
    popup.setOnShow(this::refresh);
    popup.setOnHide(this::clearUseFeedback);
    if (player != null) {
      player.getEvents().addListener(CursePenalties.GAIN_REDUCED_EVENT, curseGainListener);
    }
  }

  @Override
  public void dispose() {
    if (player != null) {
      player.getEvents().removeListener(CursePenalties.GAIN_REDUCED_EVENT, curseGainListener);
    }
    clearUseFeedback();
    if (curseGainFeedback != null) {
      curseGainFeedback.remove();
      curseGainFeedback = null;
    }
    super.dispose();
  }

  /** Opens the popup with the inventory refreshed from the current run state. */
  public void open() {
    refresh();
    AudioService.playSound(SoundId.ITEM_PICKUP, 0.8f);
    popup.show();
  }

  private void refresh() {
    Table content = popup.getContentTable();
    content.clear();
    content.top().pad(8f);
    content.setBackground(skin.newDrawable(STYLE_NAME_WHITE, PANEL_COLOUR));

    PlayerRunState playerState = runState.getOrCreatePlayerState();
    List<ItemType> ownedItems = playerState.getOwnedItems();
    List<ItemType> itemTypes = distinctInOrder(ownedItems);
    float listHeight =
        Math.clamp(itemTypes.size(), 1, MAX_VISIBLE_ROWS) * (ROW_HEIGHT + ROW_SPACING);

    Table header = new Table();
    header.add().width(30f);
    header.add(new Label("INVENTORY", titleStyle())).expandX().center();
    header.add(createCloseButton()).size(30f);

    content.add(header).width(CONTENT_WIDTH).height(54f).padBottom(1f).row();

    int itemCount = ownedItems.size();
    int typeCount = itemTypes.size();
    Label summary =
        new Label(
            itemCount
                + (itemCount == 1 ? " Item | " : " Items | ")
                + typeCount
                + (typeCount == 1 ? " Type" : " Types"),
            detailStyle());

    content.add(summary).width(CONTENT_WIDTH).height(26f).left().padBottom(5f).row();
    content.add(divider()).width(CONTENT_WIDTH).height(1f).padBottom(5f).row();

    if (ownedItems.isEmpty()) {
      content
          .add(new Label("No items collected yet.", detailStyle()))
          .width(CONTENT_WIDTH)
          .height(listHeight)
          .center()
          .row();
    } else {
      Table itemList = new Table();
      itemList.top();

      for (ItemType item : itemTypes) {
        itemList
            .add(createItemRow(playerState, item))
            .width(LIST_WIDTH)
            .height(ROW_HEIGHT)
            .padBottom(ROW_SPACING)
            .row();
      }

      ScrollPane scrollPane = new ScrollPane(itemList, skin);
      scrollPane.setFadeScrollBars(false);
      scrollPane.setScrollbarsOnTop(true);
      scrollPane.setOverscroll(false, false);
      scrollPane.setScrollingDisabled(true, false);

      content.add(scrollPane).width(CONTENT_WIDTH).height(listHeight).row();
    }

    String turnHint =
        canUseBattleItems.getAsBoolean()
            ? "Consumable items can be used this turn."
            : "Consumable items can be used on your turn in battle.";

    content
        .add(new Label(turnHint, detailStyle()))
        .width(CONTENT_WIDTH)
        .height(28f)
        .center()
        .padTop(8f)
        .padBottom(4f);
  }

  private Table createItemRow(PlayerRunState playerState, ItemType item) {
    int count = playerState.getOwnedItemCount(item);

    Table frame = new Table();
    frame.setBackground(skin.newDrawable(STYLE_NAME_WHITE, BORDER_COLOUR));
    frame.pad(1f);

    Table row = new Table();
    row.setBackground(skin.newDrawable(STYLE_NAME_WHITE, ROW_COLOUR));
    row.pad(8f, 12f, 8f, 12f);

    String iconPath = ItemArtwork.cutoutPath(item);
    ResourceService resources = ServiceLocator.getResourceService();
    ItemIconView icon = new ItemIconView();
    if (iconPath != null && resources != null && resources.containsAsset(iconPath, Texture.class)) {
      icon.setItem(resources.getAsset(iconPath, Texture.class));
    } else {
      icon.setItem(null);
    }
    row.add(icon).size(56f).padRight(8f);

    Table text = new Table();
    text.left();

    text.add(new Label(ItemFormatting.formatItemName(item), nameStyle()))
        .width(TEXT_WIDTH)
        .height(30f)
        .left()
        .row();

    ItemText description = describeItem(item, count);
    Color effectColour = item.isCurse() ? CURSE_COLOUR : GOLD_COLOUR;
    Label effect = new Label(description.effect(), smallLabelStyle(effectColour));
    effect.setWrap(true);
    text.add(effect).width(TEXT_WIDTH).height(28f).left().row();

    row.add(text).width(TEXT_WIDTH).left();

    Table actionColumn = new Table();
    actionColumn.add(createCountBadge(count)).width(54f).height(26f).right().row();
    if (item.isBattleConsumable()) {
      actionColumn.add(createUseButton(item)).width(ACTION_WIDTH).height(36f).padTop(4f);
    } else {
      actionColumn.add(createStatusBadge(item)).width(ACTION_WIDTH).height(36f).padTop(4f);
    }
    row.add(actionColumn).width(ACTION_WIDTH).padLeft(8f);

    frame.add(row).grow();
    return frame;
  }

  private Table createStatusBadge(ItemType item) {
    String status;
    if (item.isCurse()) {
      status = "CURSE";
    } else if (item == ItemType.LUCKY_COIN) {
      status = "ON GOLD";
    } else {
      status = "PASSIVE";
    }
    Color statusColour = item.isCurse() ? CURSE_COLOUR : GOLD_COLOUR;

    Table border = new Table();
    border.setBackground(skin.newDrawable(STYLE_NAME_WHITE, ROW_BORDER_COLOUR));
    border.pad(2f);

    Table badge = new Table();
    badge.setBackground(skin.newDrawable(STYLE_NAME_WHITE, ROW_BACKGROUND_COLOUR));
    badge.add(new Label(status, smallLabelStyle(statusColour))).center();

    border.add(badge).grow();
    return border;
  }

  private Table createCountBadge(int count) {
    Table badge = new Table();
    badge.setBackground(skin.newDrawable(STYLE_NAME_WHITE, ROW_BACKGROUND_COLOUR));
    badge.add(new Label("x" + count, smallLabelStyle(NAME_COLOUR))).center();
    return badge;
  }

  private Table createUseButton(ItemType item) {
    TextButtonStyle style = new TextButtonStyle();
    style.font = skin.getFont(FONT_SMALL);
    style.fontColor = ACTION_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = DISABLED_ACTION_COLOUR;
    style.checkedFontColor = ACTION_COLOUR;
    style.up = skin.newDrawable(STYLE_NAME_WHITE, ROW_BACKGROUND_COLOUR);
    style.over = skin.newDrawable(STYLE_NAME_WHITE, ROW_COLOUR);
    style.down = skin.newDrawable(STYLE_NAME_WHITE, BORDER_COLOUR);
    style.disabled = skin.newDrawable(STYLE_NAME_WHITE, ROW_BACKGROUND_COLOUR);
    style.checked = style.up;
    style.checkedOver = style.over;

    TextButton button = new TextButton("USE", style);
    button.setDisabled(canUseItemNow(item));
    Table border = new Table();
    border.setBackground(
        skin.newDrawable(
            STYLE_NAME_WHITE, button.isDisabled() ? ROW_BORDER_COLOUR : ACTION_COLOUR));
    border.pad(2f);
    border.add(button).grow();

    String tooltip =
        item == ItemType.AMBROSIA_FLASK
            ? "Use during your turn while below max health"
            : "Use during your turn";
    button.addListener(new TextTooltip(tooltip, skin));

    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (canUseItemNow(item)) {
              button.setDisabled(true);
              border.setBackground(skin.newDrawable(STYLE_NAME_WHITE, ROW_BORDER_COLOUR));
              return;
            }

            CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
            int before = effectValue(item, stats);
            boolean used = runState.getOrCreatePlayerState().useBattleItem(item, player);

            if (used) {
              AudioService.playSound(SoundId.ITEM_PICKUP, 0.7f);
              useActionInProgress = true;
              showUseFeedback(item, effectValue(item, stats) - before);
              refresh();
            }
          }
        });

    return border;
  }

  private boolean canUseItemNow(ItemType item) {
    if (useActionInProgress || !canUseBattleItems.getAsBoolean()) {
      return true;
    }
    if (item != ItemType.AMBROSIA_FLASK) {
      return false;
    }
    if (player == null) {
      return true;
    }
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    return stats == null || stats.getHealth() >= stats.getMaxHealth();
  }

  private static int effectValue(ItemType item, CombatStatsComponent stats) {
    return switch (item) {
      case IRON_AEGIS -> stats.getArmour();
      case WARRIORS_CREST -> {
        StatusEffect strength = stats.getStatusEffect("STRENGTH");
        yield strength == null ? 0 : strength.getValue();
      }
      case AMBROSIA_FLASK -> stats.getHealth();
      default -> 0;
    };
  }

  private void showUseFeedback(ItemType itemId, int actualGain) {
    if (useFeedback != null) {
      useFeedback.remove();
      useFeedback = null;
    }

    UseFeedbackDetails feedback =
        switch (itemId) {
          case IRON_AEGIS ->
              new UseFeedbackDetails(
                  AEGIS_EFFECT_COLOUR, "IRON AEGIS USED", "+" + actualGain + " ARMOUR");
          case WARRIORS_CREST -> {
            String detail = "+" + actualGain + " STRENGTH";
            if (actualGain < 1) {
              detail += " (BRAND -" + (1 - actualGain) + ")";
            }
            yield new UseFeedbackDetails(CREST_EFFECT_COLOUR, "WARRIOR'S CREST USED", detail);
          }
          default ->
              new UseFeedbackDetails(
                  AMBROSIA_EFFECT_COLOUR, "AMBROSIA USED", "+" + actualGain + " HEALTH");
        };
    Color effectColour = feedback.colour();
    String title = feedback.title();
    String detail = feedback.detail();

    Table border = new Table();
    border.setTransform(true);
    border.setOrigin(Align.center);
    border.setBackground(skin.newDrawable(STYLE_NAME_WHITE, effectColour));
    border.pad(3f);

    Table message = new Table();
    message.setBackground(skin.newDrawable(STYLE_NAME_WHITE, ROW_BACKGROUND_COLOUR));

    String iconPath = ItemArtwork.cutoutPath(itemId);
    ResourceService resources = ServiceLocator.getResourceService();

    if (iconPath != null && resources != null && resources.containsAsset(iconPath, Texture.class)) {
      Texture texture = resources.getAsset(iconPath, Texture.class);
      ItemIconView icon = new ItemIconView();
      icon.setItem(texture);
      message.add(icon).size(42f).pad(7f, 8f, 7f, 8f);
    }

    Table text = new Table();

    Label titleLabel = new Label(title, labelStyle(NAME_COLOUR));

    Label detailLabel = new Label(detail, smallLabelStyle(effectColour));

    text.add(titleLabel).left().row();
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

  private void showCurseGainFeedback(CursePenalties.GainReduction reduction) {
    // Consumables already have their own actual-value feedback inside the open inventory.
    if (stage == null || popup.isShowing()) {
      return;
    }

    String curseName;
    String statName;
    if (CursePenalties.STRENGTH_PENALTY.equals(reduction.penalty())) {
      curseName = "BRAND OF THE DAMNED";
      statName = "STRENGTH";
    } else if (CursePenalties.BLOCK_PENALTY.equals(reduction.penalty())) {
      curseName = "SHATTERED AEGIS";
      statName = "BLOCK";
    } else {
      return;
    }

    if (curseGainFeedback != null) {
      curseGainFeedback.remove();
    }
    Table feedback = new Table();
    feedback.setName("curseGainFeedback");
    feedback.setBackground(
        skin.newDrawable(STYLE_NAME_WHITE, new Color(0.05f, 0.04f, 0.08f, 0.88f)));
    feedback.pad(5f, 10f, 5f, 10f);
    feedback.add(new Label(curseName, smallLabelStyle(CURSE_COLOUR))).center().row();

    Label gain = new Label("+" + reduction.requested() + " " + statName, labelStyle(TEXT_COLOUR));
    gain.setName("curseGainNumber");
    gain.setAlignment(Align.center);
    Group crack = new Group();
    crack.setName("curseGainCrack");
    crack.setVisible(false);
    crack.getColor().a = 0f;
    Image slash = new Image(skin.newDrawable(STYLE_NAME_WHITE, CURSE_CRACK_COLOUR));
    slash.setSize(144f, 3f);
    slash.setPosition(43f, 15f);
    slash.setOrigin(72f, 1.5f);
    slash.setRotation(-12f);
    crack.addActor(slash);
    Image splinter = new Image(skin.newDrawable(STYLE_NAME_WHITE, CURSE_CRACK_COLOUR));
    splinter.setSize(28f, 2f);
    splinter.setPosition(128f, 20f);
    splinter.setOrigin(14f, 1f);
    splinter.setRotation(35f);
    crack.addActor(splinter);

    Stack number = new Stack();
    number.add(gain);
    number.add(crack);
    feedback.add(number).size(230f, 34f);
    feedback.pack();
    feedback.setPosition((stage.getWidth() - feedback.getWidth()) / 2f, stage.getHeight() - 170f);
    feedback.getColor().a = 0f;
    stage.addActor(feedback);
    curseGainFeedback = feedback;
    crack.addAction(
        Actions.sequence(
            Actions.delay(0.22f),
            Actions.visible(true),
            Actions.fadeIn(0.03f),
            Actions.delay(0.07f),
            Actions.fadeOut(0.15f)));
    feedback.addAction(
        Actions.sequence(
            Actions.fadeIn(0.08f),
            Actions.delay(0.24f),
            Actions.run(() -> gain.setText("+" + reduction.actual() + " " + statName)),
            Actions.delay(0.55f),
            Actions.fadeOut(0.18f),
            Actions.run(
                () -> {
                  if (curseGainFeedback == feedback) {
                    curseGainFeedback = null;
                  }
                }),
            Actions.removeActor()));
  }

  private void clearUseFeedback() {
    if (useFeedback != null) {
      useFeedback.remove();
      useFeedback = null;
    }

    useActionInProgress = false;
  }

  private LabelStyle labelStyle(Color colour) {
    LabelStyle style = new LabelStyle(skin.get(STYLE_NAME_DEFAULT, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private LabelStyle titleStyle() {
    LabelStyle style = new LabelStyle(skin.get(STYLE_NAME_LARGE, LabelStyle.class));
    style.fontColor = TEXT_COLOUR;
    return style;
  }

  private LabelStyle nameStyle() {
    LabelStyle style = new LabelStyle(skin.get(STYLE_NAME_DEFAULT, LabelStyle.class));
    style.fontColor = TEXT_COLOUR;
    return style;
  }

  private LabelStyle detailStyle() {
    return smallLabelStyle(DETAIL_COLOUR);
  }

  private LabelStyle smallLabelStyle(Color colour) {
    LabelStyle style = new LabelStyle(skin.get(STYLE_NAME_SMALL, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private TextButton createCloseButton() {
    TextButtonStyle style =
        new TextButtonStyle(skin.get(STYLE_NAME_DEFAULT, TextButtonStyle.class));

    style.fontColor = NAME_COLOUR;
    style.up = skin.newDrawable(STYLE_NAME_WHITE, ROW_BACKGROUND_COLOUR);
    style.over = skin.newDrawable(STYLE_NAME_WHITE, ROW_BORDER_COLOUR);

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
    return new Image(skin.newDrawable(STYLE_NAME_WHITE, ROW_BORDER_COLOUR));
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
