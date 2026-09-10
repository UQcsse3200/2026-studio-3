package com.csse3200.game.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.csse3200.game.services.ServiceLocator;

/**
 * MapNodeActor
 *
 * <p>The UI element to visually render each node the player can travel to
 */
public class MapNodeActor extends Group {

  private final MapNode node;
  public float size;
  private Image nodeIcon;

  /**
   * Constructer class to intialize a MapNodeActor. The size of the node is determined from the
   * width of the window to fill out the screen. 13f is derived from there being 7 nodes in each
   * layer, and thus 6 gaps between them 6 + 7 = 13
   *
   * @param node Node to be rendered
   */
  public MapNodeActor(MapNode node) {
    this.node = node;
    float mapWidth = Gdx.graphics.getWidth();
    this.size = mapWidth / 13f;
    nodeIcon =
        new Image(ServiceLocator.getResourceService().getAsset(getNodeIcon(), Texture.class));

    nodeIcon.setSize(size, size);
    checkNodeState();
    addActor(nodeIcon);
  }

  /**
   * Constructer class to intialize a MapNodeActor using a custom size.
   *
   * @param node Node to be rendered
   * @param size Size of the node
   */
  public MapNodeActor(MapNode node, int size) {
    this.node = node;
    this.size = size;

    nodeIcon =
        new Image(ServiceLocator.getResourceService().getAsset(getNodeIcon(), Texture.class));

    nodeIcon.setSize(size, size);
    checkNodeState();
    addActor(nodeIcon);
  }

  /**
   * Constructer class used for testing where loading assets is not needed
   *
   * @param node node to make actor
   * @param test parameter to change signature to not load assets
   */
  public MapNodeActor(MapNode node, boolean test) {
    this.node = node;
    float mapWidth = Gdx.graphics.getWidth();
    this.size = mapWidth / 13f;
    nodeIcon = new Image();
    nodeIcon.setSize(size, size);
  }

  /**
   * Gets the Node
   *
   * @return the node the actor uses
   */
  public MapNode getNode() {
    return node;
  }

  /**
   * Returns the node's ID
   *
   * @return NodeId
   */
  public int getNodeId() {
    return node.getNodeId();
  }

  /**
   * Size of the Node used typically for padding in MapDisplay
   *
   * @return Node Size
   */
  public float getNodeSize() {
    return this.size;
  }

  /**
   * Sets the node to a specified size
   *
   * @param size size of node
   */
  public void setNodeSize(float size) {
    this.size = size;
    nodeIcon.setSize(size, size);
  }

  /**
   * Defines what happens when a node is hovered
   *
   * @param hovered state of if the mouse is over the node
   */
  public void setHovered(boolean hovered) {
    if (hovered) {
      nodeIcon.setScale(1.25f);
    } else {
      nodeIcon.setScale(1f);
    }
  }

  /**
   * Determines which image should be used for the node based on it's NodeState and RoomType
   *
   * @return the path of the correct node image
   */
  private String getNodeIcon() {
    switch (node.getRoomType()) {
      case COMBAT:
        if (node.getState() == NodeState.COMPLETED) {
          return "images/combat_icon_completed.png";
        }
        if (node.getState() == NodeState.CURRENT) {
          return "images/combat_icon_current.png";
        } else {
          return "images/combat_icon.png";
        }
      case SHOP:
        if (node.getState() == NodeState.COMPLETED) {
          return "images/shop_icon_completed.png";
        }
        if (node.getState() == NodeState.CURRENT) {
          return "images/shop_icon_current.png";
        } else {
          return "images/shop_icon.png";
        }
      case EVENT:
        if (node.getState() == NodeState.COMPLETED) {
          return "images/event_icon_completed.png";
        }
        if (node.getState() == NodeState.CURRENT) {
          return "images/event_icon_current.png";
        } else {
          return "images/event_icon.png";
        }
      case FINAL:
        if (node.getState() == NodeState.CURRENT) {
          return "images/final_icon_current.png";
        } else {
          return "images/final_icon.png";
        }

      default:
        return "images/event_icon.png";
    }
  }

  /** Modifies the node based on the NodeState to visually indicate the player */
  private void checkNodeState() {
    float iconSize = size;
    switch (node.getState()) {
      case LOCKED:
        nodeIcon.getColor().a = 0.5f;
        break;
      case AVAILABLE:
        iconSize = size * 1.25f;
        break;
      case COMPLETED:
        nodeIcon.getColor().a = 0.5f;
        break;
      case CURRENT:
        iconSize = size * 1.25f;
        break;
      default:
        break;
    }
    setNodeSize(iconSize);
  }
}
