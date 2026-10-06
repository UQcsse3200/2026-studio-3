package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.components.battle.InventoryPopupComponent;
import com.csse3200.game.components.pausemenu.PauseMenuFactory;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.components.spritedisplay.clickable.BattleMenuSkins;
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
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.TerminalDisplay;
import com.csse3200.game.ui.terminal.commands.DiscoverAllCardsCommand;
import com.csse3200.game.ui.terminal.commands.GotoCommand;
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
  private ImageButton exitButton;
  private MapDisplay mapDisplay;

  public MapScreen(GdxGame game) {
    this.game = game;
    logger.debug("Initialising map screen services");
    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();

    RunState runState = game.getRunState();
    AudioService.load();

    if (!runState.isRunActive()) {
      logger.info("No run in progress, generating a new map");
      MapGenerationController mapGen = new MapGenerationController();

      startNewRun(runState, mapGen.getMap());
      runState.createStarterDeckForNewRun(new CardLibrary(CardConfigLoader.loadCards()));
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
    mapDisplay = new MapDisplay(runState.getMapGraph(), runState);

    mapDisplay
        .getMapSelectionController()
        .getEvents()
        .addListener(
            "nodeSelected",
            (Integer nodeId) -> {
              enterEncounter(game, runState, nodeId);
            });

    // PROPOSED: debug terminal for cheats/commands on the map (unlock nodes, etc.). Same
    // Terminal/KeyboardTerminalInputComponent/TerminalDisplay trio used elsewhere; F1 toggles it.
    Terminal terminal = new Terminal();
    terminal.addCommand("unlocknode", new UnlockNodeCommand(runState));
    terminal.addCommand("listnodes", new ListNodesCommand(runState));
    terminal.addCommand("goto", new GotoCommand(runState, mapDisplay.getMapSelectionController()));
    terminal.addCommand(
        "discoverallcards", new DiscoverAllCardsCommand(game.getCardDiscoveryService()));

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(ServiceLocator.getRenderService().getStage(), 10))
        .addComponent(mapDisplay)
        .addComponent(terminal)
        .addComponent(new KeyboardTerminalInputComponent())
        .addComponent(new TerminalDisplay());

    // Pause menu + in-place save/load overlay (the map is the natural place to save a run).
    // No on-screen pause button here (it didn't fit the map HUD); Escape still opens the menu.
    SaveLoadPanel savePanel = PauseMenuFactory.attachWithoutButton(ui, game);
    ServiceLocator.getEntityService().register(ui);
    savePanel.hide(); // save overlay starts hidden, opened by the Save & Load button

    createExitButton(game);
    createInventoryButton(runState);
  }

  /**
   * Adds an "Item Inventory" button and its popup to the map screen, so players can check their
   * owned items between encounters. Backed by its own entity (not the shared {@code ui} entity in
   * {@link #createUi}) since an entity can only hold one component of a given class, mirroring the
   * pattern BattleScreen uses for the same popup.
   *
   * @param runState shared run state, used to read the player's owned items
   */
  private void createInventoryButton(RunState runState) {
    ResourceService resourceService = ServiceLocator.getResourceService();
    String[] inventoryTextures = {
      "images/ui/inventory-panel.png",
      "images/ui/lucky-coin.png",
      "images/ui/energy-crystal.png",
      "images/ui/merchants-favor.png",
      "images/ui/iron-aegis.png",
      "images/ui/warriors-crest.png"
    };
    for (String texturePath : inventoryTextures) {
      if (!resourceService.containsAsset(texturePath, Texture.class)) {
        resourceService.loadTextures(new String[] {texturePath});
      }
    }
    resourceService.loadAll();

    Stage stage = ServiceLocator.getRenderService().getStage();

    PopupDisplay itemInventory = new PopupDisplay("Item Inventory");
    itemInventory.setMinSize(400f, 400f);

    // Map screen has no live player entity (only battles do), and item USE actions only make
    // sense mid-combat — so canUseBattleItems always returns false here, which means
    // InventoryPopupComponent never actually dereferences the null player.
    InventoryPopupComponent inventoryPopup =
        new InventoryPopupComponent(runState, itemInventory, null, () -> false);

    Entity itemInventoryEntity =
        new Entity().addComponent(itemInventory).addComponent(inventoryPopup);
    ServiceLocator.getEntityService().register(itemInventoryEntity);

    ImageTextButton inventoryButton =
        new ImageTextButton(
            "Item Inventory", BattleMenuSkins.forIcon(BattleMenuSkins.Icon.INVENTORY));
    inventoryButton.pad(6f, 12f, 6f, 18f);
    inventoryButton.getImageCell().size(48f);
    inventoryButton.getLabelCell().expandX().right();

    float buttonWidth = 247f;
    float buttonHeight = 48f;
    float offset = 24f;
    inventoryButton.setSize(buttonWidth, buttonHeight);
    inventoryButton.setPosition(offset, stage.getHeight() - buttonHeight - offset);

    inventoryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            inventoryPopup.open();
          }
        });

    stage.addActor(inventoryButton);
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

    GdxGame.ScreenType destination =
        switch (roomType) {
          case FINAL, COMBAT, ELITE -> GdxGame.ScreenType.BATTLE_SCREEN;
          case CAMPFIRE -> GdxGame.ScreenType.CAMPFIRE;
          case null, default -> GdxGame.ScreenType.ENCOUNTER;
        };
    logger.info("Node {} ({}) selected, entering {}", nodeId, roomType, destination);
    if (roomType == RoomType.FINAL) {
      game.showNarration("pre_boss", GdxGame.ScreenType.BATTLE_SCREEN);
    } else {
      game.setScreen(destination);
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

    exitButton = new ImageButton(style);

    exitButton.setSize(150f, 50f);
    positionExitButton();

    exitButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    stage.addActor(exitButton);
  }

  private void positionExitButton() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    float offset = 52f;

    exitButton.setPosition(
        stage.getWidth() - exitButton.getWidth() - offset,
        stage.getHeight() - exitButton.getHeight() - offset / 2f);
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

    if (mapDisplay != null) {
      mapDisplay.resizeHud();
    }

    positionExitButton();
  }

  @Override
  public void dispose() {
    logger.debug("Disposing map screen");
    renderer.dispose();
    mapDisplay.dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ScreenUtils.clear(new Color(248f / 255f, 249f / 255f, 178f / 255f, 1f));
  }
}
