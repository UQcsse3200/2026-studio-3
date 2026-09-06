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
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Comparator;
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
      MapGraph graph = new MapGraph(NodePoolGenerator.generate(config));
      startNewRun(runState, graph);
    }

    createUi(game, runState);
  }

  /**
   * Places the player on a bottom-row node so the map is actually playable: {@link
   * RunState#startRun} flips that node to {@code CURRENT} and its neighbours to {@code AVAILABLE},
   * which is what makes {@code MapInputHandler} clicks fire {@code nodeSelected} instead of {@code
   * nodeLocked}. Falls back to just holding the map (no start node) if seeding fails.
   */
  private void startNewRun(RunState runState, MapGraph graph) {
    int lowestHeight =
        graph.getNodes().values().stream().mapToInt(MapNode::getHeight).min().orElse(0);
    MapNode start =
        graph.getNodesByHeight(lowestHeight).stream()
            .min(Comparator.comparingInt(MapNode::getNodeId))
            .orElse(null);

    if (start == null || !runState.startRun(graph, start.getNodeId())) {
      logger.warn("Could not seed a start node, map will open with everything locked");
      runState.setMapGraph(graph);
    }
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

  /**
   * Records the node being entered and switches to the screen that owns it: a battle for combat and
   * boss nodes, the placeholder encounter screen for everything else (shop, event). Coming back is
   * handled by whichever screen the run lands on ({@code BattleActions} for a battle, {@code
   * EncounterScreen} otherwise), which reports the result to the run state and returns here.
   */
  private void enterEncounter(GdxGame game, RunState runState, Integer nodeId) {
    runState.enterEncounter(nodeId);

    MapNode node = runState.getMapGraph() == null ? null : runState.getMapGraph().getNode(nodeId);
    RoomType roomType = node == null ? null : node.getRoomType();

    if (roomType == RoomType.COMBAT || roomType == RoomType.FINAL) {
      logger.info("Node {} ({}) selected, entering battle", nodeId, roomType);
      game.setScreen(GdxGame.ScreenType.BATTLE_SCREEN);
    } else {
      logger.info("Node {} ({}) selected, entering encounter", nodeId, roomType);
      game.setScreen(GdxGame.ScreenType.ENCOUNTER);
    }
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
