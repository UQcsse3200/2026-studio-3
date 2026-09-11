package com.csse3200.game.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
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
  private final MapInputHandler mapInputHandler;
  private final MapSelectionController mapSelectionController;

  private Group group;
  private ScrollPane scrollPane;
  private final float mapHeight;
  private final float mapWidth = Gdx.graphics.getWidth();
  private final float nodeWidth = mapWidth / 13f; // default size
  // to store positions
  private final Map<Integer, Vector2> nodePositions = new HashMap<>();

  /**
   * Constructer method to initialize mapGraph
   *
   * @param mapGraph
   */
  public MapDisplay(MapGraph mapGraph) {
    this.mapGraph = mapGraph;
    this.mapSelectionController = new MapSelectionController(mapGraph);
    this.mapInputHandler = new MapInputHandler(mapSelectionController);
    this.mapHeight =
        (MapGenerationConfig.MAP_HEIGHT + 1)
            * 2f
            * nodeWidth;
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
    background.setSize(group.getWidth(), group.getHeight());
    background.setPosition(256, 0);
    group.addActor(background);
  }

  /** Creates the three visual elements of the UI. Background, Nodes,
   *  Connections, legend, and player stats */
  @Override
  public void create() {
    super.create();
    loadMapAssets();

    group = new Group();
    group.setSize(mapWidth - 512, mapHeight);
    addBackground();
    addNodes();
    addConnections();
    

    // Ensure connections are behind Nodes by placing Nodes on top
    for (Actor actor : group.getChildren()) {
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
      nodePositions.put( node.getNodeId(), new Vector2(
              x + nodeActor.getNodeSize() / 2f,
              y + nodeActor.getNodeSize() / 2f));

      group.addActor(nodeActor);
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
    float mapStart = 256f;
    float mapEnd = mapWidth - 256f;

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

  private void addExitButton(Table table) {
    TextButton exitButton = new TextButton("EXIT", skin);
    exitButton.setColor(new Color(0.75f, 0.18f, 0.16f, 1f));
    exitButton.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, Actor actor) {
        // exit to main menu
      }
    });
    table.add(exitButton).right().expandX().padRight(25);
  }
  
  private void addPlayerStats() {
    Table playerTable = new Table();

    playerTable.setSize(mapWidth, 100);
    playerTable.setPosition(0, Gdx.graphics.getHeight() - 100);
    playerTable.setBackground(
        skin.newDrawable("color", new Color(0.105f, 0.070f, 0.065f, 0.98f)));
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

    // Heart image
    Image heartImage =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/heart.png", Texture.class));

    // Health text
    int currentHealth;
    int maxHealth;

    CombatStatsComponent combatStats =
        entity.getComponent(CombatStatsComponent.class);

    if (combatStats != null) {
      currentHealth = combatStats.getHealth();
      maxHealth = combatStats.getMaxHealth();
    } else {
      PlayerConfig stats =
          FileLoader.readClass(PlayerConfig.class, "configs/player.json");
      currentHealth = stats.health;
      maxHealth = stats.maxHealth;
    }

    String healthText =
        String.format("Health: %d / %d", currentHealth, maxHealth);
    Label.LabelStyle healthStyle = new Label.LabelStyle(skin.get("large",
     Label.LabelStyle.class));
    healthStyle.fontColor = new Color(0.75f, 0.18f, 0.16f, 1f);

    Label healthLabel = new Label(healthText, healthStyle);
    healthLabel.setFontScale(0.75f);

    // Money image
    Image moneyImage =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/money.png", Texture.class));

    // Money text
    int money;

    InventoryComponent inventoryComponent =
        entity.getComponent(InventoryComponent.class);

    if (inventoryComponent != null) {
      money = inventoryComponent.getGold();
    } else {
      PlayerConfig stats =
          FileLoader.readClass(PlayerConfig.class,
             "configs/player.json");
      money = stats.gold;
    }

    Label.LabelStyle moneyStyle = new Label.LabelStyle(skin.get("large",
     Label.LabelStyle.class));
    moneyStyle.fontColor = new Color(0.95f, 0.73f, 0.28f, 1f);
    String moneyText = String.format("Gold: $%d", money);
    Label moneyLabel = new Label(moneyText, moneyStyle);
    moneyLabel.setFontScale(0.75f);

    // Add stats to table
    table.add(heartImage).size(imageSideLength).padRight(5f).center();
    table.add(healthLabel).padRight(25f).center();

    table.add(moneyImage).size(imageSideLength).padRight(5f).center();
    table.add(moneyLabel).center();

    playerTable.add(table);
    addExitButton(playerTable);
  }

  private void addLegendRow(String imageUrl, String text, Table table) {
    Label.LabelStyle textStyle = new Label.LabelStyle(skin.get("large", 
    Label.LabelStyle.class));
    textStyle.fontColor = new Color(1,1,1,1);
    
    Table row = new Table();
    Image key =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset(imageUrl, Texture.class));

    Label bossLabel = new Label(text, textStyle);
    bossLabel.setFontScale(0.5f);
    
    row.add(key);
    row.add(bossLabel);
    table.add(row).left();
    table.row();
  }

  private void addLegend() {
    Table playerTable = new Table();

    playerTable.setSize(192, (32 + 16) * 7);
    playerTable.setPosition(Gdx.graphics.getWidth() - 224,
     Gdx.graphics.getHeight() / 2f - playerTable.getHeight() /2f);
    playerTable.setBackground(
        skin.newDrawable("color", new Color(0.105f, 0.070f, 0.065f, 0.98f)));
    playerTable.setDebug(false); // for testing
    stage.addActor(playerTable);

    addLegendRow("images/map/boss.png", "BOSS", playerTable);
    addLegendRow("images/map/combat_elite.png", "ELITE COMBAT", playerTable);
    addLegendRow("images/map/combat.png", "COMBAT", playerTable);
    addLegendRow("images/map/shop.png", "SHOP", playerTable);
    addLegendRow("images/map/event.png", "EVENT", playerTable);
    addLegendRow("images/map/start.png", "START", playerTable);
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
    // draw is handled by the stage
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
      "images/nodeLine.png",
      "images/map/background.png",
      "images/heart.png",
      "images/energy.png",
      "images/piety.png",
      "images/money.png"
  };

    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(mapAssets);
    resourceService.loadAll();
  }
}
