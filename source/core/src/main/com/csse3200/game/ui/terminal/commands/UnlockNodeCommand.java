package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debug/cheat command: unlocks a specific map node by id (LOCKED -> AVAILABLE). */
public class UnlockNodeCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UnlockNodeCommand.class);
  private final RunState runState;

  public UnlockNodeCommand(RunState runState) {
    if (runState == null) {
      throw new IllegalArgumentException("runState must not be null");
    }
    this.runState = runState;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      logger.debug("Invalid arguments received for 'unlocknode' command: {}", args);
      return false;
    }

    int nodeId;
    try {
      nodeId = Integer.parseInt(args.get(0));
    } catch (NumberFormatException e) {
      logger.debug("Non-numeric argument received for 'unlocknode' command: {}", args);
      return false;
    }

    MapGraph mapGraph = runState.getMapGraph();
    if (mapGraph == null) {
      logger.warn("No active map graph; cannot unlock node {}", nodeId);
      return false;
    }

    MapNode node = mapGraph.getNode(nodeId);
    if (node == null) {
      logger.debug("Node {} does not exist", nodeId);
      return false;
    }

    if (node.getState() != NodeState.LOCKED) {
      logger.debug("Node {} is not locked (state: {}), nothing to do", nodeId, node.getState());
      return false;
    }

    // Flipping the state alone isn't enough: moveToNode() also requires the target to be a real
    // graph connection of the current node, not just AVAILABLE, so connect them too.
    MapNode currentNode = mapGraph.getCurrentNode();
    if (currentNode != null) {
      mapGraph.connectNodes(currentNode.getNodeId(), nodeId);
    }

    node.setState(NodeState.AVAILABLE);
    return true;
  }
}
