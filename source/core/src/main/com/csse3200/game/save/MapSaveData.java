package com.csse3200.game.save;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Serializable map graph and current run position. */
public class MapSaveData {
  public List<MapNodeSaveData> nodes = new ArrayList<>();
  public Integer currentNodeId;
  public Integer activeEncounterNodeId;

  /** Required for JSON deserialisation. */
  public MapSaveData() {}

  public MapSaveData(
      Collection<MapNodeSaveData> nodes, Integer currentNodeId, Integer activeEncounterNodeId) {
    if (nodes != null) {
      this.nodes.addAll(nodes);
    }
    this.currentNodeId = currentNodeId;
    this.activeEncounterNodeId = activeEncounterNodeId;
  }
}
