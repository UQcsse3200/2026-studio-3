package com.csse3200.game.areas;

import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.chance.ChanceEncounterFactory;
import com.csse3200.game.chance.ChanceEncounterSelector;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.chance.ChanceEncounterDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.encounters.integration.CardCatalogGateway;
import com.csse3200.game.encounters.integration.CardServiceCatalogAdapter;
import com.csse3200.game.encounters.integration.ComponentPlayerStateAdapter;
import com.csse3200.game.encounters.integration.DeckGateway;
import com.csse3200.game.encounters.integration.EncounterFlowController;
import com.csse3200.game.encounters.integration.IntegratedShopTransactionGateway;
import com.csse3200.game.encounters.integration.InventoryDeckAdapter;
import com.csse3200.game.encounters.integration.PlayerDeckAdapter;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopInventoryGenerator;
import com.csse3200.game.shop.ShopService;
import java.util.Objects;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-combat encounter area used to host Team 2 Chance and Shop encounter content.
 *
 * <p>The owning screen supplies the active map node and completion callback. EVENT and SHOP
 * encounters are started independently and do not use the old Sprint 1 forest demo flow.
 */
public class EncounterGameArea extends GameArea {
  private static final Logger logger = LoggerFactory.getLogger(EncounterGameArea.class);
  private static final String[] encounterTextures = {"images/star_player.png"};

  private final Integer nodeId;
  private final RoomType roomType;
  private final EncounterCallback completionCallback;
  private final RunState runState;
  private final PlayerDeck sharedPlayerDeck;

  private Entity player;
  private EncounterFlowController encounterFlow;
  private CardCatalogGateway cardCatalog;

  /**
   * Creates the standalone Shop preview used by the legacy MainGameScreen shortcut.
   *
   * @param terrainFactory retained for compatibility with existing screen construction
   * @param runState persistent state for the current run
   */
  public EncounterGameArea(TerrainFactory terrainFactory, RunState runState) {
    this(
        terrainFactory,
        ShopEncounter.DEFAULT_NODE_ID,
        RoomType.SHOP,
        (nodeId, success) ->
            logger.debug("Standalone shop completed for node {} with success={}", nodeId, success),
        runState,
        null);
  }

  /**
   * Creates an isolated encounter area.
   *
   * <p>This constructor is retained for tests and standalone encounter usage.
   *
   * @param terrainFactory terrain factory supplied by the hosting screen
   * @param nodeId encounter node ID
   * @param roomType encounter room type
   * @param completionCallback callback used to report encounter completion
   */
  public EncounterGameArea(
      TerrainFactory terrainFactory,
      Integer nodeId,
      RoomType roomType,
      EncounterCallback completionCallback) {
    this(terrainFactory, nodeId, roomType, completionCallback, new RunState(), null);
  }

  /**
   * Creates an encounter backed by the supplied run and deck state.
   *
   * @param terrainFactory terrain factory supplied by the hosting screen
   * @param nodeId encounter node ID
   * @param roomType encounter room type
   * @param completionCallback callback used to report encounter completion
   * @param runState persistent state for the current run
   * @param sharedPlayerDeck persistent player deck, or null for temporary inventory storage
   */
  public EncounterGameArea(
      TerrainFactory terrainFactory,
      Integer nodeId,
      RoomType roomType,
      EncounterCallback completionCallback,
      RunState runState,
      PlayerDeck sharedPlayerDeck) {
    super();

    Objects.requireNonNull(terrainFactory, "terrainFactory cannot be null");

    this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");

    this.roomType = Objects.requireNonNull(roomType, "roomType cannot be null");

    this.completionCallback =
        Objects.requireNonNull(completionCallback, "completionCallback cannot be null");

    this.runState = Objects.requireNonNull(runState, "runState cannot be null");

    this.sharedPlayerDeck = sharedPlayerDeck;
  }

  /** Creates the logical player state and launches the selected encounter. */
  @Override
  public void create() {
    loadAssets();

    /*
     * PlayerFactory restores PlayerRunState exactly once. Do not call
     * runState.getOrCreatePlayerState().applyTo(player) again here because permanent item effects
     * such as Energy Crystal would otherwise be applied twice.
     */
    player = PlayerFactory.createPlayer(runState);

    initialiseEncounterFlow();
    displayEncounter();
  }

  public Entity getPlayer() {
    return player;
  }

  private void displayEncounter() {
    switch (encounterTypeFor(roomType)) {
      case CHANCE:
        displayChanceEncounter();
        break;

      case SHOP:
        displayShop();
        break;

      default:
        throw new IllegalStateException("Unhandled non-combat encounter type");
    }
  }

  static EncounterFlowController.EncounterType encounterTypeFor(RoomType roomType) {
    if (roomType == RoomType.EVENT) {
      return EncounterFlowController.EncounterType.CHANCE;
    }

    if (roomType == RoomType.SHOP) {
      return EncounterFlowController.EncounterType.SHOP;
    }

    throw new IllegalArgumentException("Room type is not a non-combat encounter: " + roomType);
  }

  private void displayShop() {
    ShopService shopService = createMapShop(ServiceLocator.getCardLibrary());
    ShopEncounter shopEncounter = encounterFlow.startShop(nodeId, shopService);

    Entity shopUi = new Entity();
    shopUi.addComponent(
        new ShopDisplay(
            shopEncounter, ServiceLocator.getCardLibrary(), runState.getOrCreatePlayerState()));
    spawnEntity(shopUi);
  }

  /** Builds the three generated offers shown when the player enters a map Shop node. */
  static ShopService createMapShop(CardService cardService) {
    return new ShopInventoryGenerator(cardService).createShop();
  }

  private void displayChanceEncounter() {
    ChanceEncounterSelector selector =
        new ChanceEncounterSelector(
            ChanceEncounterFactory.createInitialEncounters(cardCatalog), new Random());

    Entity chanceUi = new Entity();
    chanceUi.addComponent(
        new ChanceEncounterDisplay(encounterFlow.startChance(nodeId, selector.select())));

    spawnEntity(chanceUi);
  }

  private void initialiseEncounterFlow() {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);

    ComponentPlayerStateAdapter playerState =
        new ComponentPlayerStateAdapter(combatStats, inventory);

    cardCatalog = new CardServiceCatalogAdapter(ServiceLocator.getCardLibrary());

    DeckGateway deck =
        sharedPlayerDeck != null
            ? new PlayerDeckAdapter(sharedPlayerDeck)
            : new InventoryDeckAdapter(inventory);

    IntegratedShopTransactionGateway shopTransactions =
        new IntegratedShopTransactionGateway(playerState, cardCatalog, deck);

    encounterFlow =
        new EncounterFlowController(
            playerState, cardCatalog, deck, shopTransactions, completionCallback);
  }

  private void loadAssets() {
    logger.debug("Loading assets");

    ResourceService resourceService = ServiceLocator.getResourceService();

    resourceService.loadTextures(encounterTextures);

    while (!resourceService.loadForMillis(10)) {
      logger.info("Loading... {}%", resourceService.getProgress());
    }
  }

  private void unloadAssets() {
    logger.debug("Unloading assets");

    ServiceLocator.getResourceService().unloadAssets(encounterTextures);
  }

  /**
   * Captures gold and health changes made by Shop or Chance encounters before disposing the player.
   */
  @Override
  public void dispose() {
    if (player != null) {
      runState.getOrCreatePlayerState().captureFrom(player);
    }

    super.dispose();
    unloadAssets();
  }
}
