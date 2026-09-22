package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopConfig;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopItem;
import com.csse3200.game.shop.ShopService;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
 * <p>Card artwork is resolved through the Cards/Library system. A placeholder is retained when a
 * card or its texture is unavailable.
 */
public class ShopDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(ShopDisplay.class);
  public static final String BACKGROUND_TEXTURE = "images/shop/shop_outpost_background_v3.png";
  public static final String MERCHANT_TEXTURE = "images/shop/wandering_merchant_v3.png";
  public static final String PANEL_FRAME_TEXTURE = "images/shop/shop_panel_worn.png";
  public static final String CARD_FRAME_TEXTURE = "images/shop/shop_card_worn.png";
  public static final String PLAQUE_FRAME_TEXTURE = "images/shop/shop_plaque_worn.png";
  private static final String SHOP_CONFIG = "configs/shopItems.json";
  private static final float Z_INDEX = 2f;
  private static final float PANEL_WIDTH = 936f;
  private static final float CARD_WIDTH = 282f;
  private static final float CARD_PADDING = 10f;
  private static final float CARD_CONTENT_WIDTH = CARD_WIDTH - (CARD_PADDING * 2f);
  private static final float CARD_NAME_WIDTH = 166f;
  private static final float CARD_IDENTITY_GAP = 12f;

  private static final Color BACKDROP_COLOUR = new Color(0.025f, 0.012f, 0.018f, 0.34f);
  private static final Color CARD_COLOUR = new Color(0.86f, 0.78f, 0.72f, 1f);
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
  private final CardService cardService;
  private final Map<String, ItemWidgets> itemWidgets = new HashMap<>();
  private final Set<String> purchasedItemIds = new HashSet<>();

  private Stack rootStack;
  private Label goldLabel;
  private Label statusLabel;
  private TextButtonStyle availableButtonStyle;
  private TextButtonStyle unaffordableButtonStyle;
  private TextButtonStyle soldButtonStyle;
  private Drawable panelBackground;
  private Drawable availableCardBackground;
  private Drawable unaffordableCardBackground;
  private Drawable soldCardBackground;
  private Drawable unavailableCardBackground;

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
    this(inventory, shopEncounter, null);
  }

  private ShopDisplay(
      InventoryComponent inventory, ShopEncounter shopEncounter, CardService cardService) {
    this.shopEncounter =
        shopEncounter == null
            ? new ShopEncounter(inventory, new ShopService((ShopConfig) null))
            : shopEncounter;
    this.cardService = cardService;
  }

  /**
   * Creates a display for an encounter already connected through the integration gateways.
   *
   * @param shopEncounter integrated shop session
   */
  public ShopDisplay(ShopEncounter shopEncounter) {
    this(null, shopEncounter);
  }

  /**
   * Creates an integrated Shop display with artwork supplied by the shared Card Service.
   *
   * @param shopEncounter integrated shop session
   * @param cardService read-only source of card definitions and texture paths
   */
  public ShopDisplay(ShopEncounter shopEncounter, CardService cardService) {
    this(null, shopEncounter, cardService);
  }

  @Override
  public void create() {
    super.create();
    createStyles();
    addActors();
  }

  private void createStyles() {
    panelBackground =
        createPatchDrawable(
            PANEL_FRAME_TEXTURE,
            54,
            80,
            1509,
            809,
            76,
            76,
            60,
            60,
            0.38f,
            new Color(0.82f, 0.72f, 0.67f, 0.96f),
            "window-w");
    availableCardBackground = createCardDrawable(CARD_COLOUR);
    unaffordableCardBackground = createCardDrawable(new Color(0.72f, 0.46f, 0.43f, 1f));
    soldCardBackground = createCardDrawable(new Color(0.55f, 0.53f, 0.53f, 1f));
    unavailableCardBackground = createCardDrawable(new Color(0.62f, 0.5f, 0.48f, 1f));

    availableButtonStyle =
        createButtonStyle(
            new Color(0.62f, 0.28f, 0.29f, 1f),
            new Color(0.78f, 0.38f, 0.36f, 1f),
            new Color(0.46f, 0.18f, 0.2f, 1f));

    unaffordableButtonStyle = new TextButtonStyle(availableButtonStyle);
    unaffordableButtonStyle.disabled = createPlaqueDrawable(new Color(0.38f, 0.19f, 0.2f, 1f));
    unaffordableButtonStyle.disabledFontColor = new Color(0.72f, 0.4f, 0.37f, 1f);

    soldButtonStyle = new TextButtonStyle(availableButtonStyle);
    soldButtonStyle.disabled = createPlaqueDrawable(new Color(0.36f, 0.34f, 0.35f, 1f));
    soldButtonStyle.disabledFontColor = SOLD_COLOUR;
  }

  private TextButtonStyle createButtonStyle(Color upColour, Color overColour, Color downColour) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = createPlaqueDrawable(upColour);
    style.over = createPlaqueDrawable(overColour);
    style.down = createPlaqueDrawable(downColour);
    style.fontColor = Color.WHITE;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  private Drawable createPlaqueDrawable(Color tint) {
    return createPatchDrawable(
        PLAQUE_FRAME_TEXTURE, 72, 152, 2031, 409, 105, 105, 78, 78, 0.13f, tint, "button");
  }

  private Drawable createCardDrawable(Color tint) {
    return createPatchDrawable(
        CARD_FRAME_TEXTURE, 70, 64, 887, 1401, 92, 92, 92, 92, 0.17f, tint, "white");
  }

  private Drawable createPatchDrawable(
      String texturePath,
      int regionX,
      int regionY,
      int regionWidth,
      int regionHeight,
      int left,
      int right,
      int top,
      int bottom,
      float scale,
      Color tint,
      String fallbackDrawable) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null || !isLoadedTexture(resources, texturePath)) {
      return skin.newDrawable(fallbackDrawable, tint);
    }

    Texture texture = resources.getAsset(texturePath, Texture.class);
    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    TextureRegion region = new TextureRegion(texture, regionX, regionY, regionWidth, regionHeight);
    NinePatch patch = new NinePatch(region, left, right, top, bottom);
    patch.scale(scale, scale);
    patch.setColor(tint);
    return new NinePatchDrawable(patch);
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);
    rootStack.setTouchable(Touchable.enabled);
    rootStack.getColor().a = 0f;

    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && isLoadedTexture(resources, BACKGROUND_TEXTURE)) {
      Texture backgroundTexture = resources.getAsset(BACKGROUND_TEXTURE, Texture.class);
      backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      Image background = new Image(backgroundTexture);
      background.setScaling(Scaling.fill);
      rootStack.add(background);
    }

    Table backdrop = new Table();
    backdrop.setBackground(skin.newDrawable("white", BACKDROP_COLOUR));
    rootStack.add(backdrop);

    Table screenLayout = new Table();
    screenLayout.setFillParent(true);
    screenLayout.left();
    screenLayout.pad(20f, 18f, 20f, 306f);

    Table shopPanel = new Table();
    shopPanel.setBackground(panelBackground);
    shopPanel.pad(30f, 28f, 28f, 28f);

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

    screenLayout.add(shopPanel).width(PANEL_WIDTH).height(650f);
    rootStack.add(screenLayout);
    rootStack.add(createMerchantLayer());
    rootStack.add(createMerchantSpeechLayer());

    stage.addActor(rootStack);
    refresh();
    rootStack.addAction(Actions.fadeIn(0.25f));
  }

  private Table createMerchantLayer() {
    Table merchantLayer = new Table();
    merchantLayer.setFillParent(true);
    merchantLayer.bottom().right();

    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && isLoadedTexture(resources, MERCHANT_TEXTURE)) {
      Texture merchantTexture = resources.getAsset(MERCHANT_TEXTURE, Texture.class);
      merchantTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      Image merchant = new Image(merchantTexture);
      merchant.setScaling(Scaling.fit);
      merchantLayer.add(merchant).width(310f).height(465f).padRight(14f).padBottom(145f);
    }
    return merchantLayer;
  }

  private Table createMerchantSpeechLayer() {
    Table speechLayer = new Table();
    speechLayer.setFillParent(true);
    speechLayer.bottom().right();

    Table greeting = new Table();
    greeting.setBackground(skin.newDrawable("white", new Color(0.105f, 0.052f, 0.052f, 0.78f)));
    Label greetingLabel =
        new Label("\"Looking for something rare?\"", createLabelStyle("small", BODY_COLOUR));
    greetingLabel.setFontScale(0.86f);
    greetingLabel.setAlignment(Align.center);
    greeting.add(greetingLabel).center().pad(12f, 6f, 12f, 6f);
    speechLayer.add(greeting).width(295f).height(56f).padRight(20f).padBottom(42f);
    return speechLayer;
  }

  private void addHeader(Table shopPanel) {
    Table titleBlock = new Table();
    Label title =
        new Label("The wandering merchant's shop", createLabelStyle("large", BODY_COLOUR));
    Label subtitle =
        new Label(
            "Choose carefully. Each offer may be purchased once.",
            createLabelStyle("small", MUTED_COLOUR));
    title.setFontScale(1.16f);
    title.setWrap(false);
    subtitle.setFontScale(0.92f);
    titleBlock.add(title).left();
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(6f);

    Table purse = new Table();
    purse.setBackground(createPlaqueDrawable(new Color(0.76f, 0.63f, 0.45f, 1f)));
    purse.pad(12f, 18f, 12f, 18f);
    goldLabel = new Label("", createLabelStyle("default", GOLD_COLOUR));
    goldLabel.setFontScale(1.22f);
    purse.add(goldLabel);

    shopPanel.add(titleBlock).left().expandX().colspan(2);
    shopPanel.add(purse).right().width(155f);
  }

  private void addDivider(Table shopPanel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable("white", new Color(0.34f, 0.25f, 0.17f, 0.92f)));
    shopPanel.add(divider).height(2f).expandX().fillX().colspan(3).padTop(14f).padBottom(16f);
  }

  private void addShopItems(Table shopPanel) {
    int itemNumber = 0;
    int itemCount = shopEncounter.getItems().size();
    for (ShopItem item : shopEncounter.getItems()) {
      Table card = createItemCard(item);
      float rightPadding = itemNumber < itemCount - 1 ? 20f : 0f;
      shopPanel.add(card).top().width(CARD_WIDTH).height(384f).padRight(rightPadding);
      itemNumber++;
    }
  }

  private Table createItemCard(ShopItem item) {
    Table card = new Table();
    card.setBackground(availableCardBackground);
    card.pad(CARD_PADDING);

    Actor artwork = createArtwork(item);

    String cardName = resolveCardName(item);
    Label nameLabel = new Label(cardName, createLabelStyle("default", BODY_COLOUR));
    Label energyLabel = new Label(resolveEnergyText(item), createLabelStyle("small", GOLD_COLOUR));
    String cardDescription = resolveCardDescription(item);
    Label descriptionLabel = new Label(cardDescription, createLabelStyle("small", MUTED_COLOUR));
    float nameWidth = new GlyphLayout(nameLabel.getStyle().font, cardName).width;
    nameLabel.setFontScale(Math.min(0.95f, CARD_NAME_WIDTH / Math.max(nameWidth, 1f)));
    energyLabel.setFontScale(0.76f);
    energyLabel.setAlignment(Align.right);
    descriptionLabel.setFontScale(cardDescription.length() > 65 ? 0.68f : 0.78f);
    descriptionLabel.setWrap(true);
    descriptionLabel.setAlignment(Align.topLeft, Align.left);

    Table cardIdentity = new Table();
    cardIdentity.add(nameLabel).width(CARD_NAME_WIDTH).left();
    cardIdentity
        .add(energyLabel)
        .width(CARD_CONTENT_WIDTH - CARD_NAME_WIDTH - CARD_IDENTITY_GAP)
        .padLeft(CARD_IDENTITY_GAP)
        .right();

    Label priceLabel =
        new Label(String.format("%d GOLD", item.price), createLabelStyle("default", GOLD_COLOUR));
    Label stockLabel = new Label("", createLabelStyle("small", MUTED_COLOUR));
    Label stateLabel = new Label("", createLabelStyle("small", AVAILABLE_COLOUR));
    priceLabel.setFontScale(0.98f);
    stockLabel.setFontScale(0.8f);
    stateLabel.setFontScale(0.82f);

    Table detailsRow = new Table();
    detailsRow.add(priceLabel).left().expandX();
    detailsRow.add(stockLabel).right();

    TextButton buyButton = new TextButton("Purchase", availableButtonStyle);
    buyButton.getLabel().setFontScale(1f);
    buyButton.getLabel().setWrap(true);
    buyButton.getLabel().setAlignment(Align.center);
    buyButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            buyItem(item.id);
          }
        });

    card.add(artwork).width(CARD_CONTENT_WIDTH).height(198f).top();
    card.row();
    card.add(cardIdentity).width(CARD_CONTENT_WIDTH).height(25f).fillX().padTop(6f);
    card.row();
    card.add(descriptionLabel).width(CARD_CONTENT_WIDTH).height(36f).left().top();
    card.row();
    card.add(detailsRow).width(CARD_CONTENT_WIDTH).fillX().padTop(6f);
    card.row();
    card.add(stateLabel).left().padTop(3f);
    card.row();
    card.add(buyButton).bottom().width(CARD_CONTENT_WIDTH).height(46f).padTop(6f);

    itemWidgets.put(item.id, new ItemWidgets(card, stateLabel, stockLabel, buyButton));
    return card;
  }

  private Actor createArtwork(ShopItem item) {
    String texturePath = resolveArtworkPath(item);
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && isLoadedTexture(resources, texturePath)) {
      Texture texture = resources.getAsset(texturePath, Texture.class);
      texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      Image artwork = new Image(texture);
      artwork.setScaling(Scaling.stretch);
      return artwork;
    }

    Table placeholder = new Table();
    placeholder.setBackground(skin.newDrawable("white", ART_COLOUR));
    Label label = new Label("CARD ART\nUNAVAILABLE", createLabelStyle("small", MUTED_COLOUR));
    label.setFontScale(1.3f);
    label.setAlignment(Align.center);
    placeholder.add(label).center();
    return placeholder;
  }

  String resolveArtworkPath(ShopItem item) {
    return resolveCard(item)
        .map(card -> card.texturePath)
        .filter(path -> path != null && !path.isBlank())
        .orElse(null);
  }

  String resolveCardName(ShopItem item) {
    return resolveCard(item)
        .map(card -> card.name)
        .filter(name -> name != null && !name.isBlank())
        .orElseGet(() -> item == null ? "Unknown card" : item.getDisplayName());
  }

  String resolveCardDescription(ShopItem item) {
    return resolveCard(item)
        .map(card -> card.description)
        .filter(description -> description != null && !description.isBlank())
        .orElseGet(
            () -> {
              String description = item == null ? "" : item.getDescription();
              return description.isBlank() ? "No description available." : description;
            });
  }

  String resolveEnergyText(ShopItem item) {
    return resolveCard(item).map(card -> card.cost + " ENERGY").orElse("-- ENERGY");
  }

  private Optional<CardConfig> resolveCard(ShopItem item) {
    if (cardService == null || item == null || item.cardId == null || item.cardId.isBlank()) {
      return Optional.empty();
    }
    return cardService.getCard(item.cardId);
  }

  private static boolean isLoadedTexture(ResourceService resources, String texturePath) {
    return texturePath != null && resources.containsAsset(texturePath, Texture.class);
  }

  private void addFooter(Table shopPanel) {
    statusLabel =
        new Label(
            "The world is broken. Still trade goes on.", createLabelStyle("small", MUTED_COLOUR));
    statusLabel.setFontScale(0.88f);
    statusLabel.setWrap(true);

    TextButton leaveButton =
        new TextButton(
            "Leave Shop",
            createButtonStyle(
                new Color(0.55f, 0.52f, 0.5f, 1f),
                new Color(0.68f, 0.63f, 0.58f, 1f),
                new Color(0.4f, 0.38f, 0.38f, 1f)));
    leaveButton.getLabel().setFontScale(1.12f);
    leaveButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            leaveShop();
          }
        });

    shopPanel.add(statusLabel).left().expandX().fillX().colspan(2).padTop(18f).padRight(20f);
    shopPanel.add(leaveButton).right().width(250f).height(60f).padTop(18f);
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
    shopEncounter.leave();
    rootStack.addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.removeActor()));
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
        widgets.card.setBackground(availableCardBackground);
        widgets.stateLabel.setStyle(createLabelStyle("small", AVAILABLE_COLOUR));
        widgets.stateLabel.setText("AVAILABLE");
        widgets.buyButton.setStyle(availableButtonStyle);
        widgets.buyButton.setText("Purchase");
        widgets.buyButton.setDisabled(false);
        break;
      case UNAFFORDABLE:
        widgets.card.setBackground(unaffordableCardBackground);
        widgets.stateLabel.setStyle(createLabelStyle("small", UNAFFORDABLE_COLOUR));
        widgets.stateLabel.setText("UNAFFORDABLE");
        widgets.buyButton.setStyle(unaffordableButtonStyle);
        widgets.buyButton.setText("Not enough gold");
        widgets.buyButton.setDisabled(true);
        break;
      case SOLD:
        widgets.card.setBackground(soldCardBackground);
        widgets.stateLabel.setStyle(createLabelStyle("small", SOLD_COLOUR));
        widgets.stateLabel.setText("SOLD");
        widgets.buyButton.setStyle(soldButtonStyle);
        widgets.buyButton.setText("Sold");
        widgets.buyButton.setDisabled(true);
        break;
      case UNAVAILABLE:
      default:
        widgets.card.setBackground(unavailableCardBackground);
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
    if (rootStack != null) {
      rootStack.remove();
    }
    super.dispose();
  }
}
