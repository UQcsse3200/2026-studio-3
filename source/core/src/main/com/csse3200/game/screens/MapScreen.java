package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.*;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.TerminalDisplay;
import com.csse3200.game.ui.terminal.commands.ListNodesCommand;
import com.csse3200.game.ui.terminal.commands.UnlockNodeCommand;
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

  private final GdxGame game;
  private final Renderer renderer;

  public MapScreen(GdxGame game) {
    this.game = game;
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
      MapGenerationController mapGen = new MapGenerationController();

      startNewRun(runState, mapGen.getMap());
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
    MapDisplay mapDisplay = new MapDisplay(runState.getMapGraph(), runState);

    mapDisplay
        .getMapSelectionController()
        .getEvents()
        .addListener("nodeSelected", (Integer nodeId) -> enterEncounter(game, runState, nodeId));

    // PROPOSED: debug terminal for cheats/commands on the map (unlock nodes, etc.). Same
    // Terminal/KeyboardTerminalInputComponent/TerminalDisplay trio used elsewhere; F1 toggles it.
    Terminal terminal = new Terminal();
    terminal.addCommand("unlocknode", new UnlockNodeCommand(runState));
    terminal.addCommand("listnodes", new ListNodesCommand(runState));

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(ServiceLocator.getRenderService().getStage(), 10))
        .addComponent(mapDisplay)
        .addComponent(terminal)
        .addComponent(new KeyboardTerminalInputComponent())
        .addComponent(new TerminalDisplay());

    ServiceLocator.getEntityService().register(ui);

    createExitButton(game);
  }

  /**
   * Records the node being entered and switches to the screen that owns it: a battle for combat and
   * boss nodes, the Team 2 encounter screen for shop and event nodes. The destination screen owns
   * completion: it reports the result to the run state and returns to this same map.
   */
  private void enterEncounter(GdxGame game, RunState runState, Integer nodeId) {
    runState.enterEncounter(nodeId);

    MapNode node = runState.getMapGraph() == null ? null : runState.getMapGraph().getNode(nodeId);
    RoomType roomType = node == null ? null : node.getRoomType();

    if (roomType == RoomType.COMBAT || roomType == RoomType.FINAL || roomType == RoomType.ELITE) {
      logger.info("Node {} ({}) selected, entering battle", nodeId, roomType);
      game.setScreen(GdxGame.ScreenType.BATTLE_SCREEN);
    } else {
      logger.info("Node {} ({}) selected, entering encounter", nodeId, roomType);
      game.setScreen(GdxGame.ScreenType.ENCOUNTER);
    }
  }

  /**
   * Creates the exit button on the mapscreen to allow the player to leave
   *
   * @param game the GdxGame required to change the game screen.
   */
  private void createExitButton(GdxGame game) {
    Stage stage = ServiceLocator.getRenderService().getStage();
    // not hover
    Texture buttonTexture = new Texture(Gdx.files.internal("images/map/main_menu_btn.png"));

    // hover stuff
    TextureRegionDrawable buttonDrawable =
        new TextureRegionDrawable(new TextureRegion(buttonTexture));

    Drawable buttonDrawableHover = buttonDrawable.tint(new Color(0.8f, 0.8f, 0.8f, 1f));

    ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
    style.imageUp = buttonDrawable;
    style.imageOver = buttonDrawableHover;

    ImageButton exitButton = new ImageButton(style);

    float buttonWidth = 150f;
    float buttonHeight = 50f;
    float offset = 52f;

    exitButton.setSize(buttonWidth, buttonHeight);

    exitButton.setPosition(
        stage.getWidth() - buttonWidth - offset, stage.getHeight() - offset * 1.5f);

    exitButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    stage.addActor(exitButton);
  }

  @Override
  public void show() {
    game.autosaveOnMapReady();
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.105f, 0.070f, 0.120f, 1f);
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
    ScreenUtils.clear(new Color(248f / 255f, 249f / 255f, 178f / 255f, 1f));
    ServiceLocator.clear();
  }
}
