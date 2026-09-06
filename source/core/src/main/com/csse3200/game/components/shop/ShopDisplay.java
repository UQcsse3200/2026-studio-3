package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopConfig;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays the Shop Encounter interface and sends purchase requests to {@link ShopEncounter}.
 *
 * <p>The UI displays the player's current gold, available shop items, prices, stock, and purchase
 * states. Purchase validation and inventory changes are handled by the underlying Shop Encounter
 * and Shop Service rather than this display component.
 *
 * <p>Placeholder card artwork is displayed until visual assets are provided by the Cards/Library
 * system.
 */
public class ShopDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(ShopDisplay.class);
  private static final String SHOP_CONFIG = "configs/shopItems.json";
  private static final float Z_INDEX = 2f;
  private static final float PANEL_WIDTH = 1080f;
  private static final float CARD_WIDTH = 300f;
  private static final float CARD_PADDING = 30f;
  private static final float CARD_CONTENT_WIDTH = CARD_WIDTH - (CARD_PADDING * 2f);

  private static final Color BACKDROP_COLOUR = new Color(0.018f, 0.012f, 0.02f, 0.84f);
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.98f);
  private static final Color CARD_COLOUR = new Color(0.16f, 0.11f, 0.1f, 1f);
  private static final Color ART_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.95f, 0.73f, 0.28f, 1f);
  private static final Color BODY_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color MUTED_COLOUR = new Color(0.65f, 0.58f, 0.52f, 1f);
  private static final Color AVAILABLE_COLOUR = new Color(0.45f, 0.78f, 0.47f, 1f);
  private static final Color UNAFFORDABLE_COLOUR = new Color(0.88f, 0.4f, 0.34f, 1f);
  private static final Color SOLD_COLOUR = new Color(0.48f, 0.46f, 0.47f, 1f);

  enum ShopItemState {
    AVAILABLE,
    UNAFFORDABLE,
    SOLD,
    UNAVAILABLE
  }

  private static class ItemWidgets {
    private final Table card;
    private final Label stateLabel;
    private final Label stockLabel;
    private final TextButton buyButton;

    private ItemWidgets(Table card, Label stateLabel, Label stockLabel, TextButton buyButton) {
      this.card = card;
      this.stateLabel = stateLabel;
      this.stockLabel = stockLabel;
      this.buyButton = buyButton;
    }
  }

  private final ShopEncounter shopEncounter;
  private final Map<String, ItemWidgets> itemWidgets = new HashMap<>();
  private final Set<String> purchasedItemIds = new HashSet<>();

  private Table rootTable;
  private Label goldLabel;
  private Label statusLabel;
  private TextButtonStyle availableButtonStyle;
  private TextButtonStyle unaffordableButtonStyle;
  private TextButtonStyle soldButtonStyle;

  /**
   * Creates a Shop display using the default shop configuration without a map callback.
   *
   * @param inventory player inventory used for displaying gold and completing purchases
   */
  public ShopDisplay(InventoryComponent inventory) {
    this(inventory, null, ShopEncounter.DEFAULT_NODE_ID);
  }

  /**
   * Creates a Shop display connected to the supplied encounter lifecycle.
   *
   * @param inventory player inventory used for displaying current gold
   * @param callback callback notified when the player leaves the shop
   * @param nodeId identifier of the associated map node
   */
  public ShopDisplay(InventoryComponent inventory, EncounterCallback callback, Integer nodeId) {
    this(
        inventory,
        new ShopEncounter(
            nodeId,
            inventory,
            new ShopService(FileLoader.readClass(ShopConfig.class, SHOP_CONFIG)),
            callback));
  }

  /**
   * Creates a Shop display backed by a supplied Shop Service.
   *
   * @param inventory player inventory used for purchases
   * @param shopService service providing shop items and purchase validation
   */
  public ShopDisplay(InventoryComponent inventory, ShopService shopService) {
    this(inventory, new ShopEncounter(inventory, shopService));
  }

  /**
   * Creates a Shop display backed by an existing encounter session.
   *
   * @param inventory player inventory used for displaying current gold
   * @param shopEncounter encounter session receiving purchase and completion actions
   */
  public ShopDisplay(InventoryComponent inventory, ShopEncounter shopEncounter) {
    this.shopEncounter =
        shopEncounter == null
            ? new ShopEncounter(inventory, new ShopService((ShopConfig) null))
            : shopEncounter;
  }

  /**
   * Creates a display for an encounter already connected through the integration gateways.
   *
   * @param shopEncounter integrated shop session
   */
  public ShopDisplay(ShopEncounter shopEncounter) {
    this(null, shopEncounter);
  }

  @Override
  public void create() {
    super.create();
    createStyles();
    addActors();
  }

  private void createStyles() {
    availableButtonStyle = createButtonStyle(new Color(0.46f, 0.28f, 0.1f, 1f));
    availableButtonStyle.over = skin.newDrawable("button", new Color(0.72f, 0.46f, 0.16f, 1f));
    availableButtonStyle.down =
        skin.newDrawable("button-pressed", new Color(0.38f, 0.22f, 0.08f, 1f));

    unaffordableButtonStyle = new TextButtonStyle(availableButtonStyle);
    unaffordableButtonStyle.disabled =
        skin.newDrawable("button", new Color(0.25f, 0.105f, 0.095f, 1f));
    unaffordableButtonStyle.disabledFontColor = new Color(0.72f, 0.4f, 0.37f, 1f);

    soldButtonStyle = new TextButtonStyle(availableButtonStyle);
    soldButtonStyle.disabled = skin.newDrawable("button", new Color(0.105f, 0.09f, 0.095f, 1f));
    soldButtonStyle.disabledFontColor = SOLD_COLOUR;
  }

  private TextButtonStyle createButtonStyle(Color colour) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable("button", colour);
    style.fontColor = Color.WHITE;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setTouchable(Touchable.enabled);
    rootTable.setBackground(skin.newDrawable("white", BACKDROP_COLOUR));
    rootTable.center();
    rootTable.getColor().a = 0f;

    Table shopPanel = new Table();
    shopPanel.setBackground(skin.newDrawable("window-w", PANEL_COLOUR));
    shopPanel.pad(34f, 52f, 36f, 52f);

    addHeader(shopPanel);
    shopPanel.row();
    addDivider(shopPanel);
    shopPanel.row();

    if (shopEncounter.getItems().isEmpty()) {
      Label emptyLabel =
          new Label("The merchant has nothing to sell.", createLabelStyle("default", MUTED_COLOUR));
      shopPanel.add(emptyLabel).center().pad(80f).colspan(3);
    } else {
      addShopItems(shopPanel);
    }

    shopPanel.row();
    addFooter(shopPanel);

    rootTable.add(shopPanel).width(PANEL_WIDTH);
    stage.addActor(rootTable);
    refresh();
    rootTable.addAction(Actions.fadeIn(0.25f));
  }

  private void addHeader(Table shopPanel) {
    Table titleBlock = new Table();
    Label eyebrow = new Label("SHOP ENCOUNTER", createLabelStyle("small", GOLD_COLOUR));
    Label title = new Label("The Wandering Merchant", createLabelStyle("large", BODY_COLOUR));
    Label subtitle =
        new Label(
            "Choose carefully. Each offer may be purchased once.",
            createLabelStyle("small", MUTED_COLOUR));
    eyebrow.setFontScale(1.35f);
    title.setFontScale(1.55f);
    subtitle.setFontScale(1.3f);
    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(4f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(7f);

    Table purse = new Table();
    purse.setBackground(skin.newDrawable("white", new Color(0.18f, 0.12f, 0.07f, 1f)));
    purse.pad(14f, 22f, 14f, 22f);
    goldLabel = new Label("", createLabelStyle("default", GOLD_COLOUR));
    goldLabel.setFontScale(1.5f);
    purse.add(goldLabel);

    shopPanel.add(titleBlock).left().expandX().colspan(2);
    shopPanel.add(purse).right();
  }

  private void addDivider(Table shopPanel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable("white", GOLD_COLOUR));
    shopPanel.add(divider).height(3f).expandX().fillX().colspan(3).padTop(20f).padBottom(24f);
  }

  private void addShopItems(Table shopPanel) {
    int itemNumber = 0;
    int itemCount = shopEncounter.getItems().size();
    for (ShopItem item : shopEncounter.getItems()) {
      Table card = createItemCard(item);
      float rightPadding = itemNumber < itemCount - 1 ? 34f : 0f;
      shopPanel.add(card).top().width(CARD_WIDTH).minHeight(505f).padRight(rightPadding);
      itemNumber++;
    }
  }

  private Table createItemCard(ShopItem item) {
    Table card = new Table();
    card.setBackground(skin.newDrawable("white", CARD_COLOUR));
    card.pad(CARD_PADDING);

    Label categoryLabel = new Label("CARD OFFER", createLabelStyle("small", MUTED_COLOUR));
    Label nameLabel = new Label(item.getDisplayName(), createLabelStyle("default", BODY_COLOUR));
    categoryLabel.setFontScale(1.3f);
    nameLabel.setFontScale(1.45f);
    nameLabel.setWrap(true);

    Table artPlaceholder = new Table();
    artPlaceholder.setBackground(skin.newDrawable("white", ART_COLOUR));
    Label artLabel = new Label("CARD ART\nCOMING SOON", createLabelStyle("small", MUTED_COLOUR));
    artLabel.setFontScale(1.3f);
    artLabel.setAlignment(Align.center);
    artPlaceholder.add(artLabel).center();

    Label descriptionLabel =
        new Label(item.getDescription(), createLabelStyle("small", BODY_COLOUR));
    descriptionLabel.setWrap(true);
    Label priceLabel =
        new Label(String.format("%d GOLD", item.price), createLabelStyle("default", GOLD_COLOUR));
    Label stockLabel = new Label("", createLabelStyle("small", MUTED_COLOUR));
    Label stateLabel = new Label("", createLabelStyle("small", AVAILABLE_COLOUR));
    descriptionLabel.setFontScale(1.35f);
    priceLabel.setFontScale(1.45f);
    stockLabel.setFontScale(1.3f);
    stateLabel.setFontScale(1.3f);

    TextButton buyButton = new TextButton("Purchase", availableButtonStyle);
    buyButton.getLabel().setFontScale(1.42f);
    buyButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            buyItem(item.id);
          }
        });

    card.add(categoryLabel).left().width(CARD_CONTENT_WIDTH);
    card.row();
    card.add(nameLabel).left().width(CARD_CONTENT_WIDTH).padTop(8f);
    card.row();
    card.add(artPlaceholder).width(CARD_CONTENT_WIDTH).height(147f).padTop(18f);
    card.row();
    card.add(descriptionLabel).left().top().width(CARD_CONTENT_WIDTH).height(84f).padTop(18f);
    card.row();
    card.add(priceLabel).left().padTop(16f);
    card.row();
    card.add(stockLabel).left().padTop(4f);
    card.row();
    card.add(stateLabel).left().padTop(8f);
    card.row();
    card.add(buyButton).bottom().width(CARD_CONTENT_WIDTH).height(78f).padTop(18f);

    itemWidgets.put(item.id, new ItemWidgets(card, stateLabel, stockLabel, buyButton));
    return card;
  }

  private void addFooter(Table shopPanel) {
    statusLabel =
        new Label(
            "Select an offer to inspect its purchase state.",
            createLabelStyle("small", MUTED_COLOUR));
    statusLabel.setFontScale(1.3f);
    statusLabel.setWrap(true);

    TextButton leaveButton =
        new TextButton("Leave Shop", createButtonStyle(new Color(0.22f, 0.16f, 0.15f, 1f)));
    leaveButton.getLabel().setFontScale(1.42f);
    leaveButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            leaveShop();
          }
        });

    shopPanel.add(statusLabel).left().expandX().fillX().colspan(2).padTop(28f).padRight(28f);
    shopPanel.add(leaveButton).right().width(310f).height(78f).padTop(28f);
  }

  private LabelStyle createLabelStyle(String baseStyle, Color colour) {
    LabelStyle style = new LabelStyle(skin.get(baseStyle, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private void buyItem(String itemId) {
    PurchaseResult result = shopEncounter.purchase(itemId);
    logger.debug("Purchase result for {}: {}", itemId, result.getStatus());

    if (result.isSuccess()) {
      purchasedItemIds.add(itemId);
      String itemName = result.getItem() == null ? "the offer" : result.getItem().getDisplayName();
      statusLabel.setStyle(createLabelStyle("small", AVAILABLE_COLOUR));
      statusLabel.setText(String.format("Purchased %s. It was added to your deck.", itemName));
    } else {
      statusLabel.setStyle(createLabelStyle("small", UNAFFORDABLE_COLOUR));
      statusLabel.setText(result.getMessage());
    }
    refresh();
  }

  private void leaveShop() {
    logger.debug("Shop encounter completed for node {}", shopEncounter.getNodeId());
    shopEncounter.complete(true);
    rootTable.addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.removeActor()));
  }

  private void refresh() {
    Integer currency = shopEncounter.getCurrency();
    goldLabel.setText(currency == null ? "GOLD  --" : String.format("GOLD  %d", currency));

    for (ShopItem item : shopEncounter.getItems()) {
      ItemWidgets widgets = itemWidgets.get(item.id);
      if (widgets == null) {
        continue;
      }

      PurchaseResult availability = shopEncounter.canPurchase(item.id);
      ShopItemState state = getItemState(availability, purchasedItemIds.contains(item.id));
      applyItemState(widgets, item, state);
    }
  }

  private void applyItemState(ItemWidgets widgets, ShopItem item, ShopItemState state) {
    widgets.stockLabel.setText(String.format("Stock: %d", Math.max(item.stock, 0)));

    switch (state) {
      case AVAILABLE:
        widgets.card.setBackground(skin.newDrawable("white", CARD_COLOUR));
        widgets.stateLabel.setStyle(createLabelStyle("small", AVAILABLE_COLOUR));
        widgets.stateLabel.setText("AVAILABLE");
        widgets.buyButton.setStyle(availableButtonStyle);
        widgets.buyButton.setText("Purchase");
        widgets.buyButton.setDisabled(false);
        break;
      case UNAFFORDABLE:
        widgets.card.setBackground(skin.newDrawable("white", new Color(0.15f, 0.085f, 0.08f, 1f)));
        widgets.stateLabel.setStyle(createLabelStyle("small", UNAFFORDABLE_COLOUR));
        widgets.stateLabel.setText("UNAFFORDABLE");
        widgets.buyButton.setStyle(unaffordableButtonStyle);
        widgets.buyButton.setText("Not enough gold");
        widgets.buyButton.setDisabled(true);
        break;
      case SOLD:
        widgets.card.setBackground(skin.newDrawable("white", new Color(0.095f, 0.08f, 0.085f, 1f)));
        widgets.stateLabel.setStyle(createLabelStyle("small", SOLD_COLOUR));
        widgets.stateLabel.setText("SOLD");
        widgets.buyButton.setStyle(soldButtonStyle);
        widgets.buyButton.setText("Sold");
        widgets.buyButton.setDisabled(true);
        break;
      case UNAVAILABLE:
      default:
        widgets.card.setBackground(skin.newDrawable("white", new Color(0.11f, 0.085f, 0.085f, 1f)));
        widgets.stateLabel.setStyle(createLabelStyle("small", UNAFFORDABLE_COLOUR));
        widgets.stateLabel.setText("UNAVAILABLE");
        widgets.buyButton.setStyle(unaffordableButtonStyle);
        widgets.buyButton.setText("Unavailable");
        widgets.buyButton.setDisabled(true);
        break;
    }
  }

  /**
   * Converts a purchase availability result into a visual shop item state.
   *
   * @param availability result returned by the Shop Encounter system
   * @param purchased whether this offer was already purchased during the current visit
   * @return visual state displayed by the Shop UI
   */
  static ShopItemState getItemState(PurchaseResult availability, boolean purchased) {
    if (purchased) {
      return ShopItemState.SOLD;
    }
    if (availability == null) {
      return ShopItemState.UNAVAILABLE;
    }
    if (availability.isSuccess()) {
      return ShopItemState.AVAILABLE;
    }
    if (availability.getStatus() == PurchaseResult.Status.INSUFFICIENT_GOLD) {
      return ShopItemState.UNAFFORDABLE;
    }
    if (availability.getStatus() == PurchaseResult.Status.OUT_OF_STOCK) {
      return ShopItemState.SOLD;
    }
    return ShopItemState.UNAVAILABLE;
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    if (rootTable != null) {
      rootTable.remove();
    }
    super.dispose();
  }
}
