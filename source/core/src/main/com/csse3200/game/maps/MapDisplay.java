package com.csse3200.game.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
            * nodeWidth; // this to be changed for a constant in MapGenerationConfig
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
    background.setPosition(0, 0);
    group.addActor(background);
  }

  /** Creates the three visual elements of the UI. Background, Nodes, and Connections. */
  @Override
  public void create() {
    super.create();
    loadMapAssets();

    group = new Group();
    group.setSize(mapWidth, mapHeight);

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

      nodePositions.put(node.getNodeId(), new Vector2(x, y).add(nodeWidth / 2f, nodeWidth / 2f));

      nodeActor.setPosition(x, y);
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

    return nodeWidth * 1.5f * ((nodeId % 7) + 1);
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
      "images/map/background.png"
    };

    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(mapAssets);
    resourceService.loadAll();
  }
}
