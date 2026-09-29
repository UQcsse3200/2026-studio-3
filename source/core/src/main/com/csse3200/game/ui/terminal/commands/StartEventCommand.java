package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.GdxGame;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug/cheat command: jumps into a specific Chance encounter by ID, bypassing the normal random
 * ChanceEncounterSelector output while still using the real production Event lifecycle. Built
 * against Team 2's hook (Yihan, #302): selecting a real EVENT node and calling
 * GdxGame.startEvent(eventId) instead of the normal screen transition.
 *
 * <p>Deliberately does NOT reuse GotoCommand's MapSelectionController.onNodeClicked() path — that
 * fires the "nodeSelected" event, which MapScreen listens for and immediately transitions to the
 * normal (randomly-selected) EncounterScreen before this command gets a chance to force a specific
 * event ID. Instead, MapGraph.moveToNode() and RunState.enterEncounter() are called directly,
 * bypassing that event chain, then game.startEvent(eventId) is called explicitly.
 */
public class StartEventCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(StartEventCommand.class);
  private final RunState runState;
  private final GdxGame game;

  public StartEventCommand(RunState runState, GdxGame game) {
    if (runState == null) {
      throw new IllegalArgumentException("runState must not be null");
    }
    if (game == null) {
      throw new IllegalArgumentException("game must not be null");
    }
    this.runState = runState;
    this.game = game;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      logger.debug("Invalid arguments received for 'start-event' command: {}", args);
      return false;
    }
    String eventId = args.get(0);

    MapGraph mapGraph = runState.getMapGraph();
    if (mapGraph == null) {
      logger.warn("No active map graph; cannot start-event {}", eventId);
      return false;
    }

    MapNode target = findAvailableEventNode(mapGraph);
    if (target == null) {
      logger.info("start-event: no available EVENT node found on the current map");
      return false;
    }

    // Same unlock/connect pattern as GotoCommand, so this works regardless of where the target
    // node sits relative to the player's current position.
    MapNode currentNode = mapGraph.getCurrentNode();
    if (currentNode != null) {
      mapGraph.connectNodes(currentNode.getNodeId(), target.getNodeId());
    }
    if (target.getState() == NodeState.LOCKED) {
      target.setState(NodeState.AVAILABLE);
    }

    // Bypass MapSelectionController.onNodeClicked() deliberately -- see class doc. moveToNode()
    // and enterEncounter() replicate what onNodeClicked() would have done to the map/run state,
    // without firing the "nodeSelected" event that would otherwise auto-open the normal (random)
    // EncounterScreen before we get to force a specific event ID.
    if (!mapGraph.moveToNode(target.getNodeId())) {
      logger.warn("start-event: moveToNode rejected node {}", target.getNodeId());
      return false;
    }
    runState.enterEncounter(target.getNodeId());

    try {
      game.startEvent(eventId);
      return true;
    } catch (IllegalArgumentException | IllegalStateException e) {
      logger.warn("start-event: game.startEvent({}) rejected: {}", eventId, e.getMessage());
      return false;
    }
  }

  private MapNode findAvailableEventNode(MapGraph mapGraph) {
    // Searches every floor, not just the current one -- start-event only takes an event id, so
    // requiring the player to already be on the right floor for an EVENT node would defeat the
    // point of a quick debug jump.
    for (MapNode node : mapGraph.getNodes().values()) {
      if (node.getRoomType() == RoomType.EVENT
          && node.getState() != NodeState.COMPLETED
          && node.getState() != NodeState.CURRENT) {
        return node;
      }
    }
    return null;
  }
}
