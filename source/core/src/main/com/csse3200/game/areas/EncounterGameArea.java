package com.csse3200.game.areas;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.chance.ChanceEncounterFactory;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.chance.ChanceEncounterDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.encounters.integration.ComponentPlayerStateAdapter;
import com.csse3200.game.encounters.integration.EncounterFlowController;
import com.csse3200.game.encounters.integration.FunctionalCardCatalogAdapter;
import com.csse3200.game.encounters.integration.IntegratedShopTransactionGateway;
import com.csse3200.game.encounters.integration.InventoryDeckAdapter;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ShopConfig;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-combat encounter area that displays the Team 2 Chance or Shop UI belonging to the active map
 * node.
 *
 * <p>Chance and Shop encounters are intentionally not chained here. Each encounter is selected and
 * started independently from its map node, then reports completion through the shared encounter
 * callback.
 *
 * <p>The Sprint 1 forest demo is deliberately not created here. Encounter screens retain a logical
 * player entity for the existing health, currency and deck adapters, while the full-screen Chance
 * or Shop display owns everything the player sees.
 *
 * <p>Encounter completion is reported to the game's {@link RunState}. Returning therefore restores
 * the same generated map and its progress instead of exposing this area's demo forest. If the area
 * is opened from the main-menu debug shortcut with no active node, it displays a standalone Shop
 * preview.
 */
public class EncounterGameArea extends GameArea {
  private static final Logger logger = LoggerFactory.getLogger(EncounterGameArea.class);
  private static final String SHOP_CONFIG = "configs/shopItems.json";
  private static final String[] encounterTextures = {"images/star_player.png"};
  private final GdxGame game;

  private Entity player;
  private EncounterFlowController encounterFlow;

  /**
   * Initialise this EncounterGameArea to use the provided TerrainFactory.
   *
   * @param terrainFactory TerrainFactory used to create the terrain for the GameArea
   * @param game game that owns the current run and screen navigation
   * @requires terrainFactory != null && game != null
   */
  public EncounterGameArea(TerrainFactory terrainFactory, GdxGame game) {
    super();
    this.game = game;
  }

  /** Creates the logical encounter state and the UI for the active map node. */
  @Override
  public void create() {
    loadAssets();
    player = PlayerFactory.createPlayer();
    initialiseEncounterFlow();
    displaySelectedEncounter();
  }

  public Entity getPlayer() {
    return player;
  }

  private void displayShop(Integer nodeId) {
    ShopService shopService = new ShopService(FileLoader.readClass(ShopConfig.class, SHOP_CONFIG));
    ShopEncounter shopEncounter = encounterFlow.startShop(nodeId, shopService);
    Entity shopUi = new Entity();
    shopUi.addComponent(new ShopDisplay(shopEncounter, ServiceLocator.getCardLibrary()));
    spawnEntity(shopUi);
  }

  private void displayChanceEncounter(Integer nodeId) {
    Entity chanceUi = new Entity();
    chanceUi.addComponent(
        new ChanceEncounterDisplay(
            encounterFlow.startChance(
                nodeId, ChanceEncounterFactory.createInitialEncounters().get(0))));
    spawnEntity(chanceUi);
  }

  private void displaySelectedEncounter() {
    RunState runState = game.getRunState();
    Integer nodeId = runState.getActiveNodeId();

    if (nodeId == null) {
      logger.info("No active map node; opening standalone Shop preview");
      displayShop(ShopEncounter.DEFAULT_NODE_ID);
      return;
    }

    MapNode node = runState.getMapGraph() == null ? null : runState.getMapGraph().getNode(nodeId);
    RoomType roomType = node == null ? null : node.getRoomType();

    try {
      switch (encounterTypeFor(roomType)) {
        case CHANCE:
          displayChanceEncounter(nodeId);
          break;
        case SHOP:
          displayShop(nodeId);
          break;
        default:
          throw new IllegalStateException("Unhandled non-combat encounter type");
      }
    } catch (IllegalArgumentException exception) {
      logger.error(
          "Node {} has unsupported encounter room type {}; returning to map", nodeId, roomType);
      completeRunEncounterAndReturnToMap(nodeId, false);
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

  private void initialiseEncounterFlow() {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    ComponentPlayerStateAdapter playerState =
        new ComponentPlayerStateAdapter(player.getComponent(CombatStatsComponent.class), inventory);
    IntegratedShopTransactionGateway shopTransactions =
        new IntegratedShopTransactionGateway(
            playerState,
            new FunctionalCardCatalogAdapter(cardId -> true),
            new InventoryDeckAdapter(inventory));

    encounterFlow =
        new EncounterFlowController(
            playerState, shopTransactions, this::completeRunEncounterAndReturnToMap);
  }

  private void completeRunEncounterAndReturnToMap(Integer nodeId, boolean success) {
    RunState runState = game.getRunState();
    Integer activeNodeId = runState.getActiveNodeId();

    if (activeNodeId != null && !activeNodeId.equals(nodeId)) {
      logger.warn(
          "Ignoring completion for node {} because active map node is {}", nodeId, activeNodeId);
      return;
    }

    if (activeNodeId != null) {
      runState.completeEncounter(success);
    }

    Runnable returnToMap = () -> game.setScreen(GdxGame.ScreenType.MAP);
    if (Gdx.app == null) {
      returnToMap.run();
    } else {
      Gdx.app.postRunnable(returnToMap);
    }
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(encounterTextures);

    while (!resourceService.loadForMillis(10)) {
      // This could be upgraded to a loading screen
      logger.info("Loading... {}%", resourceService.getProgress());
    }
  }

  private void unloadAssets() {
    logger.debug("Unloading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.unloadAssets(encounterTextures);
  }

  @Override
  public void dispose() {
    super.dispose();
    this.unloadAssets();
  }
}
