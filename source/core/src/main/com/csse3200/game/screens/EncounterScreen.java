package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.EncounterGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.pausemenu.PauseMenuFactory;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hosts Team 2 non-battle encounters entered from the run map.
 *
 * <p>The active map node is stored in {@link RunState}. EVENT nodes launch the Chance encounter
 * flow, while SHOP nodes launch the Shop encounter flow. Completion is reported back to the same
 * RunState before returning to the map.
 */
public class EncounterScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(EncounterScreen.class);
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);
  private static final String[] SHOP_CARD_TEXTURES = {
    "images/shop/cards/bandage.png",
    "images/shop/cards/defend.png",
    "images/shop/cards/expose.png",
    "images/shop/cards/inner_focus.png",
    "images/shop/cards/poison_dagger.png",
    "images/shop/cards/strike.png"
  };
  private final String[] cardTexturePaths;
  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private final EncounterGameArea encounterGameArea;

  public EncounterScreen(GdxGame game) {
    this.game = game;
    this.runState = game.getRunState();

    MapNode activeNode = getActiveEncounterNode();
    RoomType roomType = activeNode.getRoomType();

    if (roomType != RoomType.EVENT && roomType != RoomType.SHOP) {
      throw new IllegalStateException(
          "EncounterScreen only handles EVENT and SHOP nodes, but received " + roomType);
    }

    logger.info("Opening {} encounter for map node {}", roomType, activeNode.getNodeId());

    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    List<CardConfig> cards = CardConfigLoader.loadCards();
    CardLibrary cardLibrary = new CardLibrary(cards);

    cardTexturePaths =
        cards.stream()
            .map(card -> card.texturePath)
            .filter(path -> path != null && !path.isBlank())
            .distinct()
            .toArray(String[]::new);

    ServiceLocator.registerCardLibrary(cardLibrary);
    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardLibrary);

    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(cardTexturePaths);
    resourceService.loadTextures(SHOP_CARD_TEXTURES);
    resourceService.loadAll();

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

    renderer = RenderFactory.createRenderer();
    renderer.getCamera().getEntity().setPosition(CAMERA_POSITION);

    createInput();

    TerrainFactory terrainFactory = new TerrainFactory(renderer.getCamera());
    encounterGameArea =
        new EncounterGameArea(
            terrainFactory,
            activeNode.getNodeId(),
            roomType,
            this::onEncounterComplete,
            runState,
            playerDeck);
    encounterGameArea.create();
  }

  /**
   * Resolves the currently active map node.
   *
   * @return active EVENT or SHOP node
   * @throws IllegalStateException if no valid map encounter is active
   */
  private MapNode getActiveEncounterNode() {
    if (runState.getMapGraph() == null) {
      throw new IllegalStateException("Cannot open EncounterScreen without an active map");
    }

    Integer nodeId = runState.getActiveNodeId();
    if (nodeId == null) {
      throw new IllegalStateException("Cannot open EncounterScreen without an active node");
    }

    MapNode node = runState.getMapGraph().getNode(nodeId);
    if (node == null) {
      throw new IllegalStateException("Active map node " + nodeId + " does not exist");
    }

    return node;
  }

  /** Makes the shared render stage receive input from Chance and Shop UI components. */
  private void createInput() {
    Entity inputEntity = new Entity();
    inputEntity.addComponent(new InputDecorator(ServiceLocator.getRenderService().getStage(), 10));

    // Pause menu + in-place save/load overlay.
    SaveLoadPanel savePanel = PauseMenuFactory.attach(inputEntity, game);
    ServiceLocator.getEntityService().register(inputEntity);
    savePanel.hide(); // save overlay starts hidden, opened by the Save & Load button
  }

  /**
   * Handles the final result from the shared encounter lifecycle.
   *
   * <p>The RunState remains the owner of map progression. Screen switching is deferred until the
   * current UI event has completed so the Chance/Shop display is not disposed in the middle of its
   * own button callback.
   */
  private void onEncounterComplete(Integer nodeId, boolean success) {
    Integer activeNodeId = runState.getActiveNodeId();

    if (activeNodeId == null || !activeNodeId.equals(nodeId)) {
      logger.warn(
          "Ignoring completion for node {} because active node is {}", nodeId, activeNodeId);
      return;
    }

    boolean playerDefeated = isPlayerDefeated(encounterGameArea.getPlayer());
    boolean effectiveSuccess = success && !playerDefeated;

    logger.info(
        "Encounter node {} completed with success={}, playerDefeated={}",
        nodeId,
        effectiveSuccess,
        playerDefeated);

    runState.completeEncounter(effectiveSuccess);
    if (effectiveSuccess) {
      game.requestAutosaveAfterEncounter();
    }

    GdxGame.ScreenType targetScreen =
        playerDefeated ? GdxGame.ScreenType.DEFEAT : GdxGame.ScreenType.MAP;

    Gdx.app.postRunnable(() -> game.setScreen(targetScreen));
  }

  static boolean isPlayerDefeated(Entity player) {
    if (player == null) {
      return false;
    }

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  @Override
  public void render(float delta) {
    physicsEngine.update();
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing encounter screen");

    encounterGameArea.dispose();
    renderer.dispose();

    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().unloadAssets(cardTexturePaths);
    ServiceLocator.getResourceService().unloadAssets(SHOP_CARD_TEXTURES);
    ServiceLocator.getResourceService().dispose();

    ServiceLocator.clear();
  }
}
