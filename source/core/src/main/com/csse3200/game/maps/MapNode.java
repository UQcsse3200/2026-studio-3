package com.csse3200.game.maps;

import java.util.HashSet;

/** Represents a single node in the map graph. */
public class MapNode {

  private Integer nodeId;

  /** Refers to the layer of the map the node is placed */
  private Integer height;

  private RoomType roomType;

  private NodeState state;

  private final HashSet<MapNode> connections;

  /**
   * Creates a map node.
   *
   * @param nodeId unique identifier
   * @param roomType type of room
   */
  public MapNode(Integer nodeId, RoomType roomType) {
    this.nodeId = nodeId;
    this.roomType = roomType;

    this.state = NodeState.LOCKED;
    this.height =
        nodeId / MapGenerationConfig.MAP_WIDTH; // NOTE: this could be a problem but depends
    this.connections = new HashSet<>();
  }

  public Integer getNodeId() {
    return nodeId;
  }

  public Integer getHeight() {
    return height;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public void setRoomType(RoomType type) {
    roomType = type;
  }

  // made for testing purposes since height when theres few nodes will be the 
  // same causing test to fail
  public void setHeight(Integer height) {
    this.height = height;
  }

  public NodeState getState() {
    return state;
  }

  public void setState(NodeState state) {
    this.state = state;
  }

  public HashSet<MapNode> getConnections() {
    return connections;
  }

  public void addConnection(MapNode node) {
    connections.add(node);
  }
}
