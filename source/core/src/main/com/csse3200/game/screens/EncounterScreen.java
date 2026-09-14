package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.EncounterGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
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

    logger.info(
        "Opening {} encounter for map node {}", roomType, activeNode.getNodeId());

    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

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
            this::onEncounterComplete);

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
    inputEntity.addComponent(
        new InputDecorator(ServiceLocator.getRenderService().getStage(), 10));
    ServiceLocator.getEntityService().register(inputEntity);
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

    logger.info("Encounter node {} completed with success={}", nodeId, success);

    runState.completeEncounter(success);

    Gdx.app.postRunnable(() -> game.setScreen(GdxGame.ScreenType.MAP));
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
    ServiceLocator.getResourceService().dispose();

    ServiceLocator.clear();
  }
}