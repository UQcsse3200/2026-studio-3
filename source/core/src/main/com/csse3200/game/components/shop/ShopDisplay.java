package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.components.encounters.AcquisitionResultDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.encounters.results.AcquisitionPresentation;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.shop.PurchaseResult;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopInventoryGenerator;
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
 * <p>Offered cards use the same {@link CardWidget} face as the Card Library and post-battle
 * rewards, so name, cost, type, rarity and rules text stay readable even when the artwork has no
 * baked-in labels. A placeholder is retained when a card cannot be resolved.
 */
public class ShopDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(ShopDisplay.class);
  private static final String LARGE = "large";
  private static final String DEFAULT = "default";
  public static final String BACKGROUND_TEXTURE = "images/shop/shop_outpost_background_v3.png";
  public static final String ENCOUNTER_TEXTURE = "images/shop/merchant_encounter_v2.png";
  public static final String CLOSE_SHOP_TEXTURE = "images/shop/merchant_table_extra_thin_frame.png";
  public static final String MERCHANT_TEXTURE = "images/shop/wandering_merchant_v3.png";
  public static final String PANEL_FRAME_TEXTURE = "images/shop/shop_panel_worn.png";
  public static final String CARD_FRAME_TEXTURE = "images/shop/shop_card_worn.png";
  public static final String PLAQUE_FRAME_TEXTURE = "images/shop/shop_plaque_worn.png";
  private static final String SMALL = "small";
  private static final String WHITE = "white";
  private static final float Z_INDEX = 2f;
  private static final Color ART_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.95f, 0.73f, 0.28f, 1f);
  private static final Color BODY_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color MUTED_COLOUR = new Color(0.65f, 0.58f, 0.52f, 1f);
  private static final Color AVAILABLE_COLOUR = new Color(0.82f, 0.73f, 0.56f, 1f);
  private static final Color UNAFFORDABLE_COLOUR = new Color(0.88f, 0.4f, 0.34f, 1f);

  enum ShopItemState {
    AVAILABLE,
    UNAFFORDABLE,
    SOLD,
    UNAVAILABLE
  }

  private record ItemWidgets(TextButton buyButton) {}

  private final ShopEncounter shopEncounter;
  private final CardService cardService;
  private final PlayerRunState playerRunState;
  private final CardResolver cardResolver = new CardResolver();
  private Label merchantsFavorLabel;
  private final Map<String, ItemWidgets> itemWidgets = new HashMap<>();
  private final Set<String> purchasedItemIds = new HashSet<>();
  private final java.util.List<CardDisplaySlot> displaySlots = new java.util.ArrayList<>();
  private TextButton refreshButton;
  private boolean shopOpen;
  private Image sceneBackground;
  private Label sceneSubtitle;
  private Label sceneTitle;
  private Label merchantDialogue;
  private Table merchantDialoguePanel;
  private MerchantDisplayCounter products;
  private com.badlogic.gdx.scenes.scene2d.Group shopControls;
  private com.badlogic.gdx.scenes.scene2d.Group approachControls;
  private long displayedRevision;
  private CardWidgetAssets cardWidgetAssets;
  private AcquisitionPresentation acquisitionPresentation;
  private final java.util.List<com.csse3200.game.encounters.results.AcquisitionResult.Entry>
      purchasedResults = new java.util.ArrayList<>();
  private boolean leaving;

  private Stack rootStack;
  private Table titleFrame;
  private Label goldLabel;
  private Label statusLabel;
  private TextButtonStyle availableButtonStyle;

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
    this(inventory, new ShopEncounter(nodeId, inventory, createGeneratedShop(), callback));
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
    this(inventory, shopEncounter, null, null);
  }

  private ShopDisplay(
      InventoryComponent inventory,
      ShopEncounter shopEncounter,
      CardService cardService,
      PlayerRunState playerRunState) {
    this.shopEncounter =
        shopEncounter == null ? new ShopEncounter(inventory, createGeneratedShop()) : shopEncounter;
    this.cardService = cardService == null ? ServiceLocator.getCardLibrary() : cardService;
    this.playerRunState = playerRunState;
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
    this(null, shopEncounter, cardService, null);
  }

  /** Creates an integrated shop that also displays durable item discounts. */
  public ShopDisplay(
      ShopEncounter shopEncounter, CardService cardService, PlayerRunState playerRunState) {
    this(null, shopEncounter, cardService, playerRunState);
  }

  /**
   * Builds a live shop inventory from the registered card library using {@link
   * ShopInventoryGenerator} and the shared acquisition pool.
   *
   * <p>Requires {@link ServiceLocator#getCardLibrary()} to be registered. A missing library is a
   * setup error, not something to paper over by re-reading {@code cards.json}.
   */
  static ShopService createGeneratedShop() {
    CardService cards = ServiceLocator.getCardLibrary();
    if (cards == null) {
      throw new IllegalStateException(
          "Card library must be registered with ServiceLocator before opening the shop");
    }
    return new ShopInventoryGenerator(cards).createShop();
  }

  @Override
  public void create() {
    super.create();
    acquisitionPresentation =
        new AcquisitionPresentation(
            new AcquisitionResultDisplay(
                stage,
                skin,
                resolveCardService(),
                ServiceLocator.getResourceService(),
                "SHOP RESULTS"));
    createStyles();
    addActors();
  }

  private void createStyles() {
    availableButtonStyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);
    rootStack.setName("merchant-scene");
    MerchantSceneLayout layout = new MerchantSceneLayout();
    rootStack.add(layout);
    var scene = layout.scene;
    sceneBackground = new Image();
    sceneBackground.setBounds(0, 0, 1280, 800);
    sceneBackground.setTouchable(Touchable.disabled);
    scene.addActor(sceneBackground);
    titleFrame = new Table();
    titleFrame.setName("merchant-title");
    titleFrame.setBackground(sceneFrame());
    titleFrame.left();
    titleFrame.pad(15f, 18f, 15f, 18f);
    Label title = new Label("Wayside Merchant", createLabelStyle(LARGE, BODY_COLOUR));
    sceneTitle = title;
    title.setName("merchant-title-text");
    title.setFontScale(1.12f);
    sceneSubtitle = new Label("", createLabelStyle(SMALL, BODY_COLOUR));
    sceneSubtitle.setName("merchant-subtitle-text");
    sceneSubtitle.setFontScale(0.8f);
    titleFrame.add(title).left();
    titleFrame.row();
    titleFrame.add(sceneSubtitle).left().padTop(8f);
    titleFrame.setBounds(36, 650, 470, 106);
    scene.addActor(titleFrame);
    approachControls = new com.badlogic.gdx.scenes.scene2d.Group();
    approachControls.setTouchable(Touchable.childrenOnly);
    scene.addActor(approachControls);
    Actor stallHover =
        new Actor() {
          private boolean hovered;

          {
            setName("merchant-stall-hover");
            setBounds(820, 160, 365, 335);
            addListener(
                new com.badlogic.gdx.scenes.scene2d.InputListener() {
                  @Override
                  public void enter(
                      com.badlogic.gdx.scenes.scene2d.InputEvent event,
                      float x,
                      float y,
                      int pointer,
                      Actor from) {
                    if (pointer == -1) hovered = true;
                  }

                  @Override
                  public void exit(
                      com.badlogic.gdx.scenes.scene2d.InputEvent event,
                      float x,
                      float y,
                      int pointer,
                      Actor to) {
                    if (pointer == -1) hovered = false;
                  }
                });
          }

          @Override
          public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float alpha) {
            if (!hovered) return;
            Color old = new Color(batch.getColor());
            batch.setColor(0.8f, 0.6f, 0.3f, alpha * 0.18f);
            skin.getDrawable(WHITE).draw(batch, getX() + 50, getY() + 132, 235, 2);
            batch.setColor(old);
          }
        };
    approachControls.addActor(stallHover);
    Actor marker = new MerchantInteractionMarker(skin.getDrawable(WHITE), this::openMerchant);
    marker.setBounds(935, 443, 56, 64);
    approachControls.addActor(marker);
    TextButton approachLeave = formalShopButton("Leave", this::leaveShop);
    approachLeave.setName("merchant-encounter-leave");
    approachLeave.setBounds(1040, 36, 190, 48);
    approachControls.addActor(approachLeave);
    com.badlogic.gdx.scenes.scene2d.Group narration = new com.badlogic.gdx.scenes.scene2d.Group();
    narration.setName("merchant-encounter-narration");
    narration.setTouchable(Touchable.disabled);
    narration.setBounds(260, 42, 740, 38);
    Label narrationShadow =
        new Label(
            "There is a wandering merchant...",
            createLabelStyle(DEFAULT, new Color(0f, 0f, 0f, 0.85f)));
    narrationShadow.setAlignment(Align.center);
    narrationShadow.setFontScale(0.9f);
    narrationShadow.setBounds(2, -2, 740, 38);
    narration.addActor(narrationShadow);
    Label narrationText =
        new Label("There is a wandering merchant...", createLabelStyle(DEFAULT, Color.WHITE));
    narrationText.setName("merchant-narration-text");
    narrationText.setAlignment(Align.center);
    narrationText.setFontScale(0.9f);
    narrationText.setBounds(0, 0, 740, 38);
    narration.addActor(narrationText);
    approachControls.addActor(narration);
    shopControls = new com.badlogic.gdx.scenes.scene2d.Group();
    shopControls.setTouchable(Touchable.childrenOnly);
    scene.addActor(shopControls);
    products = new MerchantDisplayCounter();
    shopControls.addActor(products);
    Table purse = new Table();
    purse.setBackground(formalShopFrame());
    ResourceService coinResources = ServiceLocator.getResourceService();
    if (coinResources != null && isLoadedTexture(coinResources, "images/money.png")) {
      Image coin = new Image(coinResources.getAsset("images/money.png", Texture.class));
      coin.setScaling(com.badlogic.gdx.utils.Scaling.fit);
      purse.add(coin).size(24).padRight(8);
    }
    goldLabel = new Label("", createLabelStyle(DEFAULT, GOLD_COLOUR));
    goldLabel.setName("merchant-gold-balance");
    purse.add(goldLabel).pad(10);
    purse.setBounds(1020, 684, 220, 56);
    shopControls.addActor(purse);
    Table dialogue = new Table();
    merchantDialoguePanel = dialogue;
    dialogue.setBackground(new MerchantDialogueFrame(skin.getDrawable(WHITE)));
    merchantDialogue = new Label("Take a look.", createLabelStyle(DEFAULT, BODY_COLOUR));
    merchantDialogue.setName("merchant-dialogue-text");
    merchantDialogue.setFontScale(0.78f);
    merchantDialogue.setWrap(true);
    dialogue.add(merchantDialogue).width(249).pad(12);
    setMerchantDialogue("Take a look.");
    shopControls.addActor(dialogue);
    refreshButton = formalShopButton("Refresh (Free)", this::refreshStock);
    refreshButton.setName("shop-refresh");
    refreshButton.setBounds(372, 18, 350, 54);
    shopControls.addActor(refreshButton);
    TextButton leave = formalShopButton("Leave", this::leaveShop);
    leave.setName("merchant-leave");
    leave.setBounds(748, 18, 220, 54);
    shopControls.addActor(leave);
    statusLabel = new Label("", createLabelStyle(SMALL, BODY_COLOUR));
    statusLabel.setFontScale(0.7f);
    statusLabel.setAlignment(Align.center);
    statusLabel.setBounds(210, 63, 860, 16);
    // Retain transaction feedback internally, without the retired bottom text actor.
    stage.addActor(rootStack);
    showMerchantState();
  }

  private Drawable sceneFrame() {
    return MerchantSceneLayout.frame(
        skin.getDrawable(WHITE), new Color(0.13f, 0.068f, 0.035f, 0.98f));
  }

  private TextButtonStyle sceneButtonStyle() {
    TextButtonStyle style = new TextButtonStyle(availableButtonStyle);
    style.up = sceneFrame();
    style.over =
        MerchantSceneLayout.frame(skin.getDrawable(WHITE), new Color(0.22f, 0.125f, 0.065f, 0.98f));
    style.down = sceneFrame();
    style.disabled = sceneFrame();
    style.fontColor = BODY_COLOUR;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private Drawable formalShopFrame() {
    return new MerchantShopFrame(skin.getDrawable(WHITE), new Color(0.11f, 0.055f, 0.03f, 1));
  }

  private TextButton formalShopButton(String text, Runnable action) {
    TextButton button = sceneButton(text, action);
    button.getLabel().setAlignment(Align.center);
    button.setStyle(
        MerchantShopFrame.buttonStyle(
            skin.getDrawable(WHITE), button.getStyle(), MerchantShopFrame.Tone.NORMAL));
    return button;
  }

  private TextButton sceneButton(String text, Runnable action) {
    TextButton button = new TextButton(text, sceneButtonStyle());
    button.getLabel().setFontScale(1f);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            action.run();
          }
        });
    return button;
  }

  void openMerchant() {
    if (shopOpen || shopEncounter.snapshot().completed()) return;
    shopOpen = true;
    showMerchantState();
  }

  private void showMerchantState() {
    ResourceService resources = ServiceLocator.getResourceService();
    String path = shopOpen ? CLOSE_SHOP_TEXTURE : ENCOUNTER_TEXTURE;
    if (resources != null && isLoadedTexture(resources, path)) {
      Texture texture = resources.getAsset(path, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      sceneBackground.setDrawable(
          new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(texture));
    }
    sceneSubtitle.setText(
        shopOpen
            ? "Choose carefully. Each offer may be purchased once."
            : "The world is broken, still trade goes on.");
    approachControls.setVisible(!shopOpen);
    shopControls.setVisible(shopOpen);
    titleFrame.setBackground(formalShopFrame());
    titleFrame.center();
    titleFrame.getCell(sceneTitle).growX().center();
    titleFrame.getCell(sceneSubtitle).growX().center();
    sceneTitle.setAlignment(Align.center);
    sceneSubtitle.setAlignment(Align.center);
    sceneTitle.setStyle(createLabelStyle(LARGE, Color.valueOf("F5E6C5")));
    sceneSubtitle.setStyle(createLabelStyle(SMALL, Color.valueOf("C9BDA7")));
    if (shopOpen) rebuildProducts();
  }

  private void rebuildProducts() {
    if (!displaySlots.isEmpty()) stage.cancelTouchFocus();
    itemWidgets.clear();
    var quote = shopEncounter.snapshot();
    displayedRevision = quote.revision();
    CardDiscoveryService discovery = ServiceLocator.getCardDiscoveryService();
    if (discovery != null)
      discovery.recordSeenAll(shopEncounter.getItems().stream().map(item -> item.cardId).toList());
    int i = 0;
    for (ShopItem item : shopEncounter.getItems()) {
      if (i == displaySlots.size()) {
        CardDisplaySlot slot =
            new CardDisplaySlot(
                skin.getDrawable(WHITE),
                createLabelStyle(DEFAULT, GOLD_COLOUR),
                sceneButtonStyle(),
                this::buyItem);
        slot.setName("shop-slot-" + i);
        products.addSlot(slot, i);
        displaySlots.add(slot);
      }
      CardDisplaySlot slot = displaySlots.get(i);
      var offer =
          quote.offers().stream().filter(o -> o.id().equals(item.id)).findFirst().orElseThrow();
      slot.bind(createArtwork(item), offer.price(), item.id, displayedRevision);
      slot.setVisible(true);
      itemWidgets.put(item.id, new ItemWidgets(slot.purchaseButton()));
      i++;
    }
    for (; i < displaySlots.size(); i++) displaySlots.get(i).setVisible(false);
    refresh();
  }

  private Actor createArtwork(ShopItem item) {
    CardWidget widget = createCardWidget(item);
    if (widget != null) {
      return widget;
    }
    return createUnavailablePlaceholder();
  }

  private CardWidget createCardWidget(ShopItem item) {
    CardService cards = resolveCardService();
    ResourceService resources = ServiceLocator.getResourceService();
    if (cards == null || resources == null || item == null || item.cardId == null) {
      return null;
    }

    CardConfig config = cards.getCard(item.cardId).orElse(null);
    if (config == null) {
      return null;
    }

    try {
      ResolvedCard resolved = cardResolver.resolveBasePreview(config, "shop-preview-" + item.id);
      return new CardWidget(resolved, cardWidgetAssets());
    } catch (RuntimeException exception) {
      logger.warn("Could not resolve shop card {} for offer {}", item.cardId, item.id, exception);
      return null;
    }
  }

  private CardService resolveCardService() {
    if (cardService != null) {
      return cardService;
    }
    return ServiceLocator.getCardLibrary();
  }

  private CardWidgetAssets cardWidgetAssets() {
    if (cardWidgetAssets == null) {
      cardWidgetAssets =
          CardWidgetAssets.fromManagedResources(skin, ServiceLocator.getResourceService());
    }
    return cardWidgetAssets;
  }

  private Table createUnavailablePlaceholder() {
    Table placeholder = new Table();
    placeholder.setBackground(skin.newDrawable(WHITE, ART_COLOUR));
    Label label = new Label("CARD ART\nUNAVAILABLE", createLabelStyle(SMALL, MUTED_COLOUR));
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
    CardService cards = resolveCardService();
    if (cards == null || item == null || item.cardId == null || item.cardId.isBlank()) {
      return Optional.empty();
    }
    return cards.getCard(item.cardId);
  }

  private static boolean isLoadedTexture(ResourceService resources, String texturePath) {
    return texturePath != null && resources.containsAsset(texturePath, Texture.class);
  }

  private LabelStyle createLabelStyle(String baseStyle, Color colour) {
    LabelStyle style = new LabelStyle(skin.get(baseStyle, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private void buyItem(String itemId, long revision) {
    var receipt = shopEncounter.purchase(itemId, revision, java.util.UUID.randomUUID().toString());
    PurchaseResult result = receipt.result();
    logger.debug("Purchase result for {}: {}", itemId, result.getStatus());

    if (result.isSuccess()) {
      purchasedItemIds.add(itemId);
      String itemName = result.getItem() == null ? "the offer" : result.getItem().getDisplayName();
      statusLabel.setStyle(createLabelStyle(SMALL, AVAILABLE_COLOUR));
      AudioService.playSound(SoundId.ITEM_PURCHASE, 0.5f);
      statusLabel.setText(String.format("Purchased %s. It was added to your deck.", itemName));
    } else {
      statusLabel.setStyle(createLabelStyle(SMALL, UNAFFORDABLE_COLOUR));
      AudioService.playSound(SoundId.ERROR, 0.5f);
      statusLabel.setText(result.getMessage());
    }
    if (result.isSuccess()) {
      String message = statusLabel.getText().toString();
      rebuildProducts();
      statusLabel.setText(message);
      setMerchantDialogue("A fine choice.");
      if (receipt.acquisition() != null) {
        purchasedResults.addAll(receipt.acquisition().entries());
      }
    } else {
      refresh();
    }
  }

  private void refreshStock() {
    var result = shopEncounter.refresh(displayedRevision, java.util.UUID.randomUUID().toString());
    if (result.isSuccess()) {
      purchasedItemIds.clear();
      rebuildProducts();
      statusLabel.setText("Stock refreshed. New offers have no discount.");
      setMerchantDialogue("Nothing catch your eye?\nLet me look again.");
    } else {
      statusLabel.setText("Refresh failed: " + result.status().name().replace('_', ' '));
      refresh();
    }
  }

  private void setMerchantDialogue(String text) {
    merchantDialogue.setText(text);
    float height = text.contains("\n") ? 64f : 46f;
    merchantDialoguePanel.setBounds(970, 665f - height, 285, height);
  }

  private void leaveShop() {
    if (leaving) return;
    leaving = true;
    if (!purchasedResults.isEmpty()) {
      acquisitionPresentation.show(
          new com.csse3200.game.encounters.results.AcquisitionResult(
              "shop-" + shopEncounter.getNodeId(), purchasedResults),
          dismissal -> finishLeavingShop());
      return;
    }
    finishLeavingShop();
  }

  private void finishLeavingShop() {
    acquisitionPresentation.close();
    logger.debug("Shop encounter completed for node {}", shopEncounter.getNodeId());
    AudioService.playSound(SoundId.LEAVE_SHOP, 0.3f);
    shopEncounter.leave();
    rootStack.addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.removeActor()));
  }

  private void refresh() {
    updateRefreshButton();
    Integer currency = shopEncounter.getCurrency();
    goldLabel.setText(currency == null ? "GOLD  --" : String.format("GOLD  %d", currency));

    refreshMerchantsFavor();

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

  private void updateRefreshButton() {
    var quote = shopEncounter.snapshot();
    if (refreshButton != null) {
      var fee = quote.nextRefreshCost();
      refreshButton.setText(
          fee.isPresent()
              ? (fee.getAsInt() == 0 ? "Refresh (Free)" : "Refresh (" + fee.getAsInt() + " GOLD)")
              : "Refresh unavailable");
      refreshButton.setDisabled(
          quote.completed()
              || fee.isEmpty()
              || quote.gold() == null
              || (fee.isPresent() && quote.gold() < fee.getAsInt()));
      MerchantShopFrame.Tone refreshTone =
          fee.isEmpty() || quote.completed()
              ? MerchantShopFrame.Tone.MUTED
              : fee.getAsInt() == 0 ? MerchantShopFrame.Tone.FREE : MerchantShopFrame.Tone.PRIMARY;
      refreshButton.setStyle(
          MerchantShopFrame.buttonStyle(
              skin.getDrawable(WHITE), refreshButton.getStyle(), refreshTone));
    }
  }

  private void refreshMerchantsFavor() {
    if (merchantsFavorLabel != null) {
      int favorCount =
          playerRunState == null ? 0 : playerRunState.getOwnedItemCount(ItemType.MERCHANTS_FAVOR);
      merchantsFavorLabel.setText(
          favorCount == 0
              ? ""
              : String.format(
                  "%s x%d  |  %.0f%% off all offers",
                  ItemFormatting.formatItemName(ItemType.MERCHANTS_FAVOR),
                  favorCount,
                  (shopEncounter.getShopDiscount() == null ? 0f : shopEncounter.getShopDiscount())
                      * 100f));
    }
  }

  private void applyItemState(ItemWidgets widgets, ShopItem item, ShopItemState state) {
    MerchantShopFrame.Tone tone =
        switch (state) {
          case AVAILABLE -> MerchantShopFrame.Tone.NORMAL;
          case UNAFFORDABLE -> MerchantShopFrame.Tone.DANGER;
          case SOLD, UNAVAILABLE -> MerchantShopFrame.Tone.MUTED;
        };
    widgets.buyButton.setStyle(
        MerchantShopFrame.buttonStyle(skin.getDrawable(WHITE), widgets.buyButton.getStyle(), tone));
    widgets.buyButton.setText(
        switch (state) {
          case AVAILABLE -> "Purchase";
          case UNAFFORDABLE -> "Not enough gold";
          case SOLD -> "Sold";
          case UNAVAILABLE -> "Unavailable";
        });
    widgets.buyButton.setDisabled(state != ShopItemState.AVAILABLE);
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
    if (acquisitionPresentation != null) {
      acquisitionPresentation.close();
    }
    if (rootStack != null) {
      rootStack.remove();
    }
    super.dispose();
  }
}
