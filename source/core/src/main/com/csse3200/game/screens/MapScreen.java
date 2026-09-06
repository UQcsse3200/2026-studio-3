package com.csse3200.game.screens;

import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.MapDisplay;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodePoolGenerator;
import com.csse3200.game.maps.RoomDistributionConfig;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screen that shows the run's map. Kept separate from MainGameScreen so the map and the battle are
 * not drawn on the same screen.
 *
 * <p>The map is read from {@link RunState}, which is owned by the game rather than by a screen, so
 * leaving the map for an encounter and coming back shows the same map with the same progress
 * instead of generating a new one.
 */
public class MapScreen extends com.badlogic.gdx.ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(MapScreen.class);

  private static final int COMBAT_WEIGHT = 70;
  private static final int EVENT_WEIGHT = 20;
  private static final int SHOP_WEIGHT = 10;
  private static final int STARTING_ROW_HEIGHT = 1;

  private final Renderer renderer;

  public MapScreen(GdxGame game) {
    logger.debug("Initialising map screen services");
    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();

    RunState runState = game.getRunState();

    if (!runState.isRunActive()) {
      logger.info("No run in progress, generating a new map");
      RoomDistributionConfig config =
          new RoomDistributionConfig(
              MapGraph.MAX_NODE_COUNT, COMBAT_WEIGHT, EVENT_WEIGHT, SHOP_WEIGHT);
      MapGraph mapGraph = new MapGraph(NodePoolGenerator.generate(config));
      Integer startNodeId =
          mapGraph.getNodesByHeight(STARTING_ROW_HEIGHT).stream()
              .map(MapNode::getNodeId)
              .min(Integer::compareTo)
              .orElseThrow(() -> new IllegalStateException("Generated map has no starting node"));

      if (!runState.startRun(mapGraph, startNodeId)) {
        throw new IllegalStateException("Generated map could not be started");
      }
    }

    createUi(game, runState);
  }

  /** Puts the map display on a UI entity so it is rendered and receives input. */
  private void createUi(GdxGame game, RunState runState) {
    MapDisplay mapDisplay = new MapDisplay(runState.getMapGraph());

    mapDisplay
        .getMapSelectionController()
        .getEvents()
        .addListener("nodeSelected", (Integer nodeId) -> enterEncounter(game, runState, nodeId));

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(ServiceLocator.getRenderService().getStage(), 10))
        .addComponent(mapDisplay);

    ServiceLocator.getEntityService().register(ui);
  }

  /** Records the node being entered and switches to its encounter. */
  private void enterEncounter(GdxGame game, RunState runState, Integer nodeId) {
    logger.info("Node {} selected, entering encounter", nodeId);
    runState.enterEncounter(nodeId);
    game.setScreen(GdxGame.ScreenType.ENCOUNTER);
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing map screen");
    renderer.dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
