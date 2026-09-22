package com.csse3200.game.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * MapDisplay
 *
 * <p>Displays the procedural map for the game. This class is responsible for displaying the map to
 * the player. It converts nodes given by {@link MapGraph} to {@link MapNodeActor} which then is
 * wrapped with the {@link MapInputHandler} to handle clicking by the player. The nodes are arranged
 * in a grid layout 7 wide and MAP_HEIGHT tall. UI elements are Actors which are displayed in the
 * group.
 */
public class MapDisplay extends UIComponent {

  private final MapGraph mapGraph;
  private final RunState runState;
  private final MapInputHandler mapInputHandler;
  private final MapSelectionController mapSelectionController;
  private static final String LARGE = "large";

  private Group group;
  private ScrollPane scrollPane;
  private final float mapHeight;
  private final float mapWidth = Gdx.graphics.getWidth();
  private final float nodeWidth = mapWidth / 13f; // default size
  // to store positions
  private final Map<Integer, Vector2> nodePositions = new HashMap<>();
  // Node-id labels, shown only while debug rendering is active — same toggle 'debug on'
  // already controls elsewhere, so this reuses it instead of adding a new one.
  private final java.util.List<Label> nodeIdLabels = new java.util.ArrayList<>();

  /**
   * Constructer method to initialize mapGraph
   *
   * @param mapGraph
   */
  public MapDisplay(MapGraph mapGraph) {
    this(mapGraph, null);
  }

  public MapDisplay(MapGraph mapGraph, RunState runState) {
    this.mapGraph = mapGraph;
    this.runState = runState;
    this.mapSelectionController = new MapSelectionController(mapGraph);
    this.mapInputHandler = new MapInputHandler(mapSelectionController);
    this.mapHeight = (MapGenerationConfig.MAP_HEIGHT + 1) * 2f * nodeWidth;
  }

  /**
   * Adds the background to the screen by accessing it through ResourceService backgroud is set to
   * the size of the group.
   */
  private void addBackground() {
    Image background =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/map/background.png", Texture.class));
    background.setSize(group.getWidth(), group.getHeight() - 100);
    background.setPosition(256, 0);
    group.addActor(background);
  }

  /**
   * Creates the three visual elements of the UI. Background, Nodes, Connections, legend, and player
   * stats
   */
  @Override
  public void create() {
    super.create();
    loadMapAssets();

    group = new Group();
    group.setSize(mapWidth - 512, mapHeight);
    addBackground();
    addNodes();
    addConnections();

    // Ensure connections are behind nodes
    for (int i = group.getChildren().size - 1; i >= 0; i--) {
      Actor actor = group.getChildren().get(i);

      if (actor instanceof MapNodeActor) {
        actor.toFront();
      }
    }

    scrollPane = new ScrollPane(group);
    scrollPane.setActor(group);
    scrollPane.setFillParent(true);
    scrollPane.setScrollingDisabled(true, false);
    scrollPane.setOverscroll(false, false);

    stage.addActor(scrollPane);

    scrollPane.layout();
    scrollPane.setScrollPercentY(1f);

    addPlayerStats();
    addLegend();
  }

  /**
   * Iterates through the list of nodes provided by @param mapGraph and attaches the @param
   * MapNodeActor and @param mapInputHandler to each Node The node position is stored for
   * Connections to create a line between nodes
   */
  private void addNodes() {
    for (MapNode node : mapGraph.getNodes().values()) {
      MapNodeActor nodeActor = new MapNodeActor(node);
      mapInputHandler.attach(nodeActor);
      float x =
          (node.getRoomType() == RoomType.FINAL || node.getRoomType() == RoomType.START)
              ? mapWidth / 2f - nodeActor.getNodeSize() / 2f
              : getNodeX(node.getNodeId(), nodeWidth);
      float y = getNodeY(node);

      nodeActor.setPosition(x, y);
      nodePositions.put(
          node.getNodeId(),
          new Vector2(x + nodeActor.getNodeSize() / 2f, y + nodeActor.getNodeSize() / 2f));

      group.addActor(nodeActor);

      Label idLabel = new Label(String.valueOf(node.getNodeId()), skin);
      idLabel.setFontScale(0.6f);
      idLabel.setPosition(x, y + nodeActor.getNodeSize());
      idLabel.setVisible(false);
      nodeIdLabels.add(idLabel);
      group.addActor(idLabel);
    }
  }

  /**
   * Calculates the x position a node needs to be to spread it evenly along each layer. nodeId % 7
   * is done because there are 7 nodes max per layer
   *
   * @param nodeId ID of the node
   * @param nodeWidth Width to ensure spacing is equal
   * @return float x value to position the Node and Connection
   */
  private float getNodeX(int nodeId, float nodeWidth) {
    float mapStart = 352f;
    float mapEnd = mapWidth - 352f;

    float spacing = (mapEnd - mapStart - nodeWidth) / 6f;

    return mapStart + nodeWidth / 8f + (nodeId % 7) * spacing;
  }

  /**
   * Calculates the y position for a node
   *
   * @param node Node needed to find it's layer
   * @return float y value to position the Node and Connection
   */
  private float getNodeY(MapNode node) {
    return node.getHeight() * 1.5f * nodeWidth + 2.5f * nodeWidth;
  }

  /**
   * Adds connections between nodes that have a link. Iterates through each node to find it's
   * connections and iterates through each one to find a start and end Vector 2 position to
   * calculate length and angle to draw a line
   */
  private void addConnections() {
    for (MapNode node : mapGraph.getNodes().values()) {
      for (MapNode connection : node.getConnections()) {
        // To not draw a connection on itself
        if (node.getNodeId() >= connection.getNodeId()) {
          continue;
        }

        Vector2 start = nodePositions.get(node.getNodeId());
        Vector2 end = nodePositions.get(connection.getNodeId());
        // Only draw connections whose two endpoint actors were created.
        if (start != null && end != null) {
          MapConnectionGroup mapConnectionGroup = new MapConnectionGroup(start, end);
          group.addActor(mapConnectionGroup);
        }
      }
    }
  }

  /**
   * Renders the player Stats at the top of the screen.
   *
   * <p>Player stats is assumed to be stored in "configs/player.json"
   *
   * <p>Piety is the height of the current node
   */
  private void addPlayerStats() {
    Table playerTable = new Table();

    playerTable.setSize(mapWidth, 100);
    playerTable.setPosition(0, Gdx.graphics.getHeight() - 100);
    playerTable.setBackground(skin.newDrawable("color", new Color(0.105f, 0.070f, 0.065f, 0.98f)));
    playerTable.setDebug(false); // for testing
    playerTable.left();
    stage.addActor(playerTable);

    Table table = new Table();
    table.left();
    table.setFillParent(true);
    table.padLeft(15f);
    table.padTop(25);

    // Image size
    float imageSideLength = 48f;

    PlayerRunState playerState = runState == null ? null : runState.getOrCreatePlayerState();

    // Heart image
    Image heartImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/heart.png", Texture.class));

    // Health text
    int currentHealth;
    int maxHealth;

    if (playerState != null) {
      currentHealth = playerState.getCurrentHealth();
      maxHealth = playerState.getMaxHealth();
    } else {
      PlayerConfig stats = FileLoader.readClass(PlayerConfig.class, "configs/player.json");
      currentHealth = stats.health;
      maxHealth = stats.maxHealth;
    }

    String healthText = String.format("Health: %d / %d", currentHealth, maxHealth);
    Label.LabelStyle healthStyle = new Label.LabelStyle(skin.get(LARGE, Label.LabelStyle.class));
    healthStyle.fontColor = new Color(0.75f, 0.18f, 0.16f, 1f);

    Label healthLabel = new Label(healthText, healthStyle);
    healthLabel.setFontScale(0.75f);

    // Money image
    Image moneyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/money.png", Texture.class));

    // Money text
    int money;

    if (playerState != null) {
      money = playerState.getGold();
    } else {
      PlayerConfig stats = FileLoader.readClass(PlayerConfig.class, "configs/player.json");
      money = stats.gold;
    }

    Label.LabelStyle moneyStyle = new Label.LabelStyle(skin.get(LARGE, Label.LabelStyle.class));
    moneyStyle.fontColor = new Color(0.95f, 0.73f, 0.28f, 1f);
    String moneyText = String.format("Gold: $%d", money);
    Label moneyLabel = new Label(moneyText, moneyStyle);
    moneyLabel.setFontScale(0.75f);

    // Piety image
    Image pietyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/piety.png", Texture.class));

    // Piety text
    Label.LabelStyle pietyStyle = new Label.LabelStyle(skin.get(LARGE, Label.LabelStyle.class));
    pietyStyle.fontColor = new Color(0.95f, 0.73f, 0.28f, 1f);
    String pietyText = String.format("Piety: %d", mapGraph.getCurrentNode().getHeight());
    Label pietyLabel = new Label(pietyText, pietyStyle);
    pietyLabel.setFontScale(0.75f);

    // Add stats to table
    table.add(heartImage).size(imageSideLength).padRight(5f).center();
    table.add(healthLabel).padRight(25f).center();

    table.add(moneyImage).size(imageSideLength).padRight(5f).center();
    table.add(moneyLabel).padRight(25f).center();

    table.add(pietyImage).size(imageSideLength).padRight(5f).center();
    table.add(pietyLabel).padRight(25f).center();

    playerTable.add(table);
  }

  /**
   * Renders a basic legend on the right of the screen to clearly state what each nodeIcon
   * represents
   */
  private void addLegend() {
    Table playerTable = new Table();

    playerTable.setSize(192, (32 + 16) * 7);
    Image legend =
        new Image(
            ServiceLocator.getResourceService().getAsset("images/map/legend.png", Texture.class));
    playerTable.add(legend);
    playerTable.setPosition(
        Gdx.graphics.getWidth() - 224,
        Gdx.graphics.getHeight() / 2f - playerTable.getHeight() / 2f);

    stage.addActor(playerTable);
  }

  /**
   * Returns the group to access UI elements
   *
   * @return group of Nodes, connections and background
   */
  /**
   * Gets the selection controller driving this display, so a screen can listen for node selection
   * and start the matching encounter.
   *
   * @return selection controller for this map
   */
  public MapSelectionController getMapSelectionController() {
    return mapSelectionController;
  }

  public Group getGroup() {
    return group;
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage; only the node-id label visibility needs a live per-frame
    // check, since it follows the 'debug on' terminal toggle.
    boolean showIds = ServiceLocator.getRenderService().getDebug().getActive();
    for (Label label : nodeIdLabels) {
      label.setVisible(showIds);
    }
  }

  /** Closes the MapUI */
  @Override
  public void dispose() {
    super.dispose();
    scrollPane.remove();
  }

  /** Loads all assets needed to render the Map UI */
  private void loadMapAssets() {
    String[] mapAssets = {
      "images/map/combat.png",
      "images/map/combat_elite.png",
      "images/map/start.png",
      "images/map/boss.png",
      "images/map/event.png",
      "images/map/shop.png",
      "images/map/nodeLine.png",
      "images/map/background.png",
      "images/heart.png",
      "images/energy.png",
      "images/piety.png",
      "images/money.png",
      "images/map/cross.png",
      "images/map/legend.png",
      "images/map/main_menu_btn.png"
    };

    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(mapAssets);
    resourceService.loadAll();
  }
}
