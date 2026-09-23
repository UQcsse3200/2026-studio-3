package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.MapSelectionController;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug/cheat command: jumps to a node matching a given floor (height) and room type, going through
 * the normal unlock/connect/select flow (same as UnlockNodeCommand) rather than faking a screen
 * transition, so encounter selection logic (which picks enemies based on floor/room type) runs
 * exactly as it would in a real playthrough.
 *
 * <p>Unlocking/connecting the target still uses MapGraph directly (same as UnlockNodeCommand), but
 * actually entering the node goes through MapSelectionController.onNodeClicked() — the same entry
 * point a real click uses — rather than MapGraph.moveToNode() directly, since only the selection
 * controller's click path fires the "nodeSelected" event that MapScreen listens for to transition
 * into the battle/shop/event screen. Calling moveToNode() alone silently updates the graph with no
 * screen transition.
 */
public class GotoCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GotoCommand.class);
  private final RunState runState;
  private final MapSelectionController mapSelectionController;

  public GotoCommand(RunState runState, MapSelectionController mapSelectionController) {
    if (runState == null) {
      throw new IllegalArgumentException("runState must not be null");
    }
    if (mapSelectionController == null) {
      throw new IllegalArgumentException("mapSelectionController must not be null");
    }
    this.runState = runState;
    this.mapSelectionController = mapSelectionController;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 2) {
      logger.debug("Invalid arguments received for 'goto' command: {}", args);
      return false;
    }

    int floor;
    try {
      floor = Integer.parseInt(args.get(0));
    } catch (NumberFormatException e) {
      logger.debug("Non-numeric floor received for 'goto' command: {}", args.get(0));
      return false;
    }

    RoomType roomType;
    try {
      roomType = RoomType.valueOf(args.get(1).toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.debug("Unknown room type received for 'goto' command: {}", args.get(1));
      return false;
    }

    MapGraph mapGraph = runState.getMapGraph();
    if (mapGraph == null) {
      logger.warn("No active map graph; cannot goto floor {} {}", floor, roomType);
      return false;
    }

    MapNode target = findMatchingNode(mapGraph, floor, roomType);
    if (target == null) {
      logger.info("goto: no {} node found on floor {}", roomType, floor);
      return false;
    }
    logger.info("goto: found target node {} (state {})", target.getNodeId(), target.getState());

    // Always ensure the connection exists, regardless of the target's current state: a
    // node generated AVAILABLE via a different path on the real map graph is not necessarily
    // connected to wherever the player actually is right now, and moveToNode() requires a real
    // connection, not just an AVAILABLE state. Connecting two nodes that are already connected
    // is a harmless no-op.
    MapNode currentNode = mapGraph.getCurrentNode();
    if (currentNode != null) {
      mapGraph.connectNodes(currentNode.getNodeId(), target.getNodeId());
    }
    if (target.getState() == NodeState.LOCKED) {
      target.setState(NodeState.AVAILABLE);
    }

    // Go through the real click entry point so the nodeSelected event fires and MapScreen
    // actually transitions into the encounter, same as if the player had clicked it.
    return mapSelectionController.onNodeClicked(target.getNodeId());
  }

  private MapNode findMatchingNode(MapGraph mapGraph, int floor, RoomType roomType) {
    List<MapNode> candidates = mapGraph.getNodesByHeight(floor);
    for (MapNode node : candidates) {
      // Skip nodes that are already done or already the player's position: re-selecting either
      // is a no-op in real gameplay (MapSelectionController rejects both), so returning one here
      // would make goto silently fail even when a genuinely reachable match exists on the same
      // floor.
      if (node.getRoomType() == roomType
          && node.getState() != NodeState.COMPLETED
          && node.getState() != NodeState.CURRENT) {
        return node;
      }
    }
    return null;
  }
}
