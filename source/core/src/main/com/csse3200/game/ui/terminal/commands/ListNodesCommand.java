package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debug/cheat command: logs every map node's id, room type and state. */
public class ListNodesCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(ListNodesCommand.class);
  private final RunState runState;

  public ListNodesCommand(RunState runState) {
    if (runState == null) {
      throw new IllegalArgumentException("runState must not be null");
    }
    this.runState = runState;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      logger.debug("Unexpected arguments received for 'listnodes' command: {}", args);
      return false;
    }

    MapGraph mapGraph = runState.getMapGraph();
    if (mapGraph == null) {
      logger.warn("No active map graph; nothing to list");
      return false;
    }

    mapGraph.getNodes().values().stream()
        .sorted(Comparator.comparingInt(MapNode::getNodeId))
        .forEach(
            node ->
                logger.info(
                    "Node {} (height {}, {}) - {}",
                    node.getNodeId(),
                    node.getHeight(),
                    node.getRoomType(),
                    node.getState()));
    return true;
  }
}
