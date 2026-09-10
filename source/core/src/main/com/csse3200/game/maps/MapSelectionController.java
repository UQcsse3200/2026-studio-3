package com.csse3200.game.maps;

import com.csse3200.game.events.EventHandler;
import java.util.ArrayList;
import java.util.List;

/** Handles player clicks on map nodes and requests moves through MapGraph. */
public class MapSelectionController {

  private final MapGraph mapGraph;
  private final EventHandler events;

  public MapSelectionController(MapGraph mapGraph) {
    this(mapGraph, new EventHandler());
  }

  public MapSelectionController(MapGraph mapGraph, EventHandler events) {
    this.mapGraph = mapGraph;
    this.events = events;
  }

  /**
   * Gets the event handler used to notify listeners of selection results.
   *
   * @return event handler for this controller
   */
  public EventHandler getEvents() {
    return events;
  }

  /**
   * Gets the ids of every node the player can currently select (state AVAILABLE). The UI can use
   * this to highlight selectable nodes.
   *
   * @return ids of all currently selectable nodes
   */
  public List<Integer> getSelectableNodeIds() {
    List<Integer> ids = new ArrayList<>();
    for (MapNode node : mapGraph.getNodesByState(NodeState.AVAILABLE)) {
      ids.add(node.getNodeId());
    }
    return ids;
  }

  /**
   * Checks whether a node can be selected right now.
   *
   * @param nodeId id of the node to check
   * @return true if the node exists and is AVAILABLE
   */
  public boolean isSelectable(Integer nodeId) {
    if (nodeId == null) {
      return false;
    }
    MapNode node = mapGraph.getNode(nodeId);
    return node != null && node.getState() == NodeState.AVAILABLE;
  }

  /**
   * Handles a click on a node. Requests a move if the node is available. Fires a different event
   * depending on why a node can't be selected, so the UI can distinguish "not unlocked yet" from
   * "already completed".
   *
   * @param nodeId id of the clicked node
   * @return true if the move was accepted, false otherwise
   */
  public boolean onNodeClicked(Integer nodeId) {

    if (nodeId == null) {
      return false;
    }

    MapNode node = mapGraph.getNode(nodeId);

    if (node == null) {
      events.trigger("nodeLocked", nodeId);
      return false;
    }

    if (node.getState() == NodeState.COMPLETED) {
      events.trigger("nodeCompleted", nodeId);
      return false;
    }

    if (node.getState() != NodeState.AVAILABLE) {
      events.trigger("nodeLocked", nodeId);
      return false;
    }

    boolean accepted = mapGraph.moveToNode(nodeId);

    if (accepted) {
      events.trigger("nodeSelected", nodeId);
    } else {
      events.trigger("nodeSelectionRejected", nodeId);
    }

    return accepted;
  }
}
