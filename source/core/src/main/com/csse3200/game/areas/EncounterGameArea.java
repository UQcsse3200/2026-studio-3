package com.csse3200.game.areas;

import java.util.Objects;
import java.util.Random;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.TerrainFactory.TerrainType;
import com.csse3200.game.chance.ChanceEncounterFactory;
import com.csse3200.game.chance.ChanceEncounterSelector;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.chance.ChanceEncounterDisplay;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.encounters.integration.ComponentPlayerStateAdapter;
import com.csse3200.game.encounters.integration.EncounterFlowController;
import com.csse3200.game.encounters.integration.FunctionalCardCatalogAdapter;
import com.csse3200.game.encounters.integration.IntegratedShopTransactionGateway;
import com.csse3200.game.encounters.integration.InventoryDeckAdapter;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ShopConfig;
import com.csse3200.game.shop.ShopEncounter;
import com.csse3200.game.shop.ShopService;
import com.csse3200.game.utils.math.GridPoint2Utils;
import com.csse3200.game.utils.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-combat encounter area used to host Team 2 encounter UI and lifecycle integration.
 *
 * <p>Chance and Shop encounters are intentionally not chained here. Each encounter is started
 * independently, and completion is reported through the shared encounter callback. Map screen
 * navigation and map refresh behaviour are left to the Map integration layer.
 *
 * <p>This is Team 2's {@code ForestGameArea} from the {@code Feature-2} branch, restored as its own
 * area after the battle team's {@code ForestGameArea} replaced it on {@code main}. It still needs a
 * host screen that has registered the entity, render, resource and physics services (as {@code
 * MainGameScreen} does) before {@link #create()} is called.
 *
 * <p>The one deviation from the original: the throwaway demo {@code MapGraph} it built is gone (the
 * current {@code MapGraph} constructor forces full path generation and cannot hold three hand-made
 * nodes). Nothing read that graph's state; the Chance -> Shop hand-off it drove now lives directly
 * in the encounter-flow callback below.
 */
public class EncounterGameArea extends GameArea {
  private static final Logger logger = LoggerFactory.getLogger(EncounterGameArea.class);
  private static final int NUM_TREES = 7;
  private static final int NUM_GHOSTS = 2;
  private static final Integer SHOP_NODE_ID = 1;
  private static final Integer CHANCE_NODE_ID = 2;
  private static final String SHOP_CONFIG = "configs/shopItems.json";
  private static final GridPoint2 PLAYER_SPAWN = new GridPoint2(10, 10);
  private static final float WALL_WIDTH = 0.1f;
  private static final String[] forestTextures = {
    "images/star_player.png",
    "images/tree.png",
    "images/ghost_king.png",
    "images/ghost_1.png",
    "images/grass_1.png",
    "images/grass_2.png",
    "images/grass_3.png",
    "images/hex_grass_1.png",
    "images/hex_grass_2.png",
    "images/hex_grass_3.png",
    "images/iso_grass_1.png",
    "images/iso_grass_2.png",
    "images/iso_grass_3.png",
    "images/heart.png",
    "images/energy.png",
    "images/piety.png",
    "images/money.png"
  };
  private static final String[] forestTextureAtlases = {
    "images/terrain_iso_grass.atlas", "images/ghost.atlas", "images/ghostKing.atlas"
  };
  private static final String[] forestSounds = {"sounds/Impact4.ogg"};
  private static final String BACKGROUND_MUSIC = "sounds/BGM_03_mp3.mp3";
  private static final String[] forestMusic = {BACKGROUND_MUSIC};

  private final TerrainFactory terrainFactory;
  private final Integer nodeId;
  private final RoomType roomType;
  private final EncounterCallback completionCallback;

  private Entity player;
  private EncounterFlowController encounterFlow;

  /**
   * Initialise this EncounterGameArea to use the provided TerrainFactory.
   *
   * @param terrainFactory TerrainFactory used to create the terrain for the GameArea.
   * @requires terrainFactory != null
   */
  public EncounterGameArea(TerrainFactory terrainFactory) {
    this(
        terrainFactory,
        CHANCE_NODE_ID,
        RoomType.EVENT,
        (nodeId, success) ->
            logger.debug(
                "Demo encounter completed for node {} with success={}", nodeId, success));
  }

  public EncounterGameArea(
      TerrainFactory terrainFactory,
      Integer nodeId,
      RoomType roomType,
      EncounterCallback completionCallback) {
    super();
    this.terrainFactory = Objects.requireNonNull(terrainFactory, "terrainFactory cannot be null");
    this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");
    this.roomType = Objects.requireNonNull(roomType, "roomType cannot be null");
    this.completionCallback =
        Objects.requireNonNull(completionCallback, "completionCallback cannot be null");
  }

  /** Create the game area, including terrain, static entities (trees), dynamic entities (player) */
  @Override
  public void create() {
    loadAssets();

    displayUI();

    spawnTerrain();
    spawnTrees();
    player = spawnPlayer();
    initialiseEncounterFlow();
    displayEncounter();
    spawnGhosts();
    spawnGhostKing();

    playMusic();
  }

  private void displayEncounter() {
    switch (roomType) {
      case EVENT:
        displayChanceEncounter();
        break;
      case SHOP:
        displayShop();
        break;
      default:
        throw new IllegalStateException(
            "EncounterGameArea cannot handle room type " + roomType);
    }
  }

  public Entity getPlayer() {
    return player;
  }

  private void displayUI() {
    Entity ui = new Entity();
    ui.addComponent(new GameAreaDisplay("Box Forest"));
    spawnEntity(ui);
  }

  private void displayShop() {
    ShopService shopService = new ShopService(FileLoader.readClass(ShopConfig.class, SHOP_CONFIG));
    ShopEncounter shopEncounter = encounterFlow.startShop(nodeId, shopService);
    Entity shopUi = new Entity();
    shopUi.addComponent(new ShopDisplay(shopEncounter));
    spawnEntity(shopUi);
  }

  private void displayChanceEncounter() {
    Entity chanceUi = new Entity();
    ChanceEncounterSelector selector =
      new ChanceEncounterSelector(ChanceEncounterFactory.createInitialEncounters(), new Random());

  chanceUi.addComponent(
      new ChanceEncounterDisplay(
          encounterFlow.startChance(nodeId, selector.select())));
    spawnEntity(chanceUi);
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
          playerState,
          shopTransactions,
          completionCallback);
  }

  private void spawnTerrain() {
    // Background terrain
    terrain = terrainFactory.createTerrain(TerrainType.FOREST_DEMO);
    spawnEntity(new Entity().addComponent(terrain));

    // Terrain walls
    float tileSize = terrain.getTileSize();
    GridPoint2 tileBounds = terrain.getMapBounds(0);
    Vector2 worldBounds = new Vector2(tileBounds.x * tileSize, tileBounds.y * tileSize);

    // Left
    spawnEntityAt(
        ObstacleFactory.createWall(WALL_WIDTH, worldBounds.y), GridPoint2Utils.ZERO, false, false);
    // Right
    spawnEntityAt(
        ObstacleFactory.createWall(WALL_WIDTH, worldBounds.y),
        new GridPoint2(tileBounds.x, 0),
        false,
        false);
    // Top
    spawnEntityAt(
        ObstacleFactory.createWall(worldBounds.x, WALL_WIDTH),
        new GridPoint2(0, tileBounds.y),
        false,
        false);
    // Bottom
    spawnEntityAt(
        ObstacleFactory.createWall(worldBounds.x, WALL_WIDTH), GridPoint2Utils.ZERO, false, false);
  }

  private void spawnTrees() {
    GridPoint2 minPos = new GridPoint2(0, 0);
    GridPoint2 maxPos = terrain.getMapBounds(0).sub(2, 2);

    for (int i = 0; i < NUM_TREES; i++) {
      GridPoint2 randomPos = RandomUtils.random(minPos, maxPos);
      Entity tree = ObstacleFactory.createTree();
      spawnEntityAt(tree, randomPos, true, false);
    }
  }

  private Entity spawnPlayer() {
    Entity newPlayer = PlayerFactory.createPlayer();
    spawnEntityAt(newPlayer, PLAYER_SPAWN, true, true);
    return newPlayer;
  }

  private void spawnGhosts() {
    GridPoint2 minPos = new GridPoint2(0, 0);
    GridPoint2 maxPos = terrain.getMapBounds(0).sub(2, 2);

    for (int i = 0; i < NUM_GHOSTS; i++) {
      GridPoint2 randomPos = RandomUtils.random(minPos, maxPos);
      Entity ghost = NPCFactory.createGhost(player);
      spawnEntityAt(ghost, randomPos, true, true);
    }
  }

  private void spawnGhostKing() {
    GridPoint2 minPos = new GridPoint2(0, 0);
    GridPoint2 maxPos = terrain.getMapBounds(0).sub(2, 2);

    GridPoint2 randomPos = RandomUtils.random(minPos, maxPos);
    Entity ghostKing = NPCFactory.createGhostKing(player);
    spawnEntityAt(ghostKing, randomPos, true, true);
  }

  private void playMusic() {
    Music music = ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class);
    music.setLooping(true);
    music.setVolume(0.3f);
    music.play();
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(forestTextures);
    resourceService.loadTextureAtlases(forestTextureAtlases);
    resourceService.loadSounds(forestSounds);
    resourceService.loadMusic(forestMusic);

    while (!resourceService.loadForMillis(10)) {
      // This could be upgraded to a loading screen
      logger.info("Loading... {}%", resourceService.getProgress());
    }
  }

  private void unloadAssets() {
    logger.debug("Unloading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.unloadAssets(forestTextures);
    resourceService.unloadAssets(forestTextureAtlases);
    resourceService.unloadAssets(forestSounds);
    resourceService.unloadAssets(forestMusic);
  }

  @Override
  public void dispose() {
    super.dispose();
    ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class).stop();
    this.unloadAssets();
  }
}
