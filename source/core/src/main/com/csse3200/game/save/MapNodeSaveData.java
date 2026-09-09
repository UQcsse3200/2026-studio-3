package com.csse3200.game.save;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Serializable map node that stores connection IDs instead of object references. */
public class MapNodeSaveData {
  public int nodeId;
  public String roomType = "";
  public String state = "";
  public List<Integer> connectionIds = new ArrayList<>();

  /** Required for JSON deserialisation. */
  public MapNodeSaveData() {}

  public MapNodeSaveData(
      int nodeId, String roomType, String state, Collection<Integer> connectionIds) {
    this.nodeId = nodeId;
    this.roomType = roomType == null ? "" : roomType;
    this.state = state == null ? "" : state;
    if (connectionIds != null) {
      this.connectionIds.addAll(connectionIds);
    }
  }
}
