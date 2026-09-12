package com.csse3200.game.save;

import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Applies validated {@link SaveGameData} to live run objects.
 *
 * <p>This service sits after {@link SaveGameService#loadGame(int)}: loadGame reads and validates
 * the save file shape, while this class validates that the loaded IDs and values can be restored
 * into current gameplay systems before mutating them.
 */
public class SaveGameRestoreService {
  private final PlayerRunState playerState;
  private final PlayerDeck playerDeck;
  private final RunState runState;

  public SaveGameRestoreService(
      PlayerRunState playerState, PlayerDeck playerDeck, RunState runState) {
    if (playerState == null) {
      throw new IllegalArgumentException("playerState must not be null");
    }
    if (playerDeck == null) {
      throw new IllegalArgumentException("playerDeck must not be null");
    }
    if (runState == null) {
      throw new IllegalArgumentException("runState must not be null");
    }
    this.playerState = playerState;
    this.playerDeck = playerDeck;
    this.runState = runState;
  }

  /**
   * Restores player values, deck card IDs and map state from loaded save data.
   *
   * <p>The method validates every currently-supported section before applying anything, so rejected
   * save data leaves the existing live state unchanged.
   */
  public RestoreResult restore(SaveGameData data) {
    RestoreResult validation = validate(data);
    if (!validation.success()) {
      return validation;
    }

    MapGraph restoredMap = buildMapGraph(data.map);

    try {
      restorePlayer(data.player);
      restoreDeck(data.deck);
      if (!runState.restoreRun(restoredMap, data.map.activeEncounterNodeId)) {
        return RestoreResult.failure(RestoreError.APPLY_FAILED, "Unable to restore run state");
      }
      return RestoreResult.success(resolveResumeScreen(data));
    } catch (RuntimeException exception) {
      return RestoreResult.failure(RestoreError.APPLY_FAILED, "Unable to apply loaded save data");
    }
  }

  private RestoreResult validate(SaveGameData data) {
    if (data == null) {
      return RestoreResult.failure(RestoreError.NO_SAVE_DATA, "Save data must not be null");
    }
    RestoreResult playerResult = validatePlayer(data.player);
    if (!playerResult.success()) {
      return playerResult;
    }
    RestoreResult deckResult = validateDeck(data.deck);
    if (!deckResult.success()) {
      return deckResult;
    }
    return validateMap(data.map);
  }

  private RestoreResult validatePlayer(PlayerSaveData playerData) {
    if (playerData == null) {
      return RestoreResult.failure(RestoreError.MISSING_PLAYER_DATA, "Save is missing player data");
    }
    if (playerData.maxHealth <= 0) {
      return RestoreResult.failure(
          RestoreError.INVALID_PLAYER_STATE, "Saved max health must be positive");
    }
    if (playerData.currentHealth < 0 || playerData.currentHealth > playerData.maxHealth) {
      return RestoreResult.failure(
          RestoreError.INVALID_PLAYER_STATE, "Saved current health is outside the valid range");
    }
    if (playerData.gold < 0 || playerData.piety < 0) {
      return RestoreResult.failure(
          RestoreError.INVALID_PLAYER_STATE, "Saved player resources cannot be negative");
    }
    return RestoreResult.success("");
  }

  private RestoreResult validateDeck(DeckSaveData deckData) {
    if (deckData == null || deckData.cardIds == null) {
      return RestoreResult.failure(RestoreError.MISSING_DECK_DATA, "Save is missing deck data");
    }
    for (String cardId : deckData.cardIds) {
      if (!playerDeck.canAddCard(cardId)) {
        return RestoreResult.failure(
            RestoreError.INVALID_DECK_STATE, "Saved deck contains an unknown card ID: " + cardId);
      }
    }
    return RestoreResult.success("");
  }

  private RestoreResult validateMap(MapSaveData mapData) {
    if (mapData == null || mapData.nodes == null) {
      return RestoreResult.failure(RestoreError.MISSING_MAP_DATA, "Save is missing map data");
    }
    if (mapData.nodes.isEmpty()) {
      return RestoreResult.failure(RestoreError.INVALID_MAP_STATE, "Saved map has no nodes");
    }

    Set<Integer> nodeIds = new HashSet<>();
    for (MapNodeSaveData nodeData : mapData.nodes) {
      RestoreResult nodeResult = validateMapNode(nodeData, nodeIds);
      if (!nodeResult.success()) {
        return nodeResult;
      }
    }

    if (mapData.currentNodeId != null && !nodeIds.contains(mapData.currentNodeId)) {
      return RestoreResult.failure(
          RestoreError.INVALID_MAP_STATE, "Saved current node does not exist in the map");
    }
    if (mapData.activeEncounterNodeId != null && !nodeIds.contains(mapData.activeEncounterNodeId)) {
      return RestoreResult.failure(
          RestoreError.INVALID_MAP_STATE, "Saved active encounter node does not exist in the map");
    }
    for (MapNodeSaveData nodeData : mapData.nodes) {
      for (Integer connectionId : nodeData.connectionIds) {
        if (connectionId == null || !nodeIds.contains(connectionId)) {
          return RestoreResult.failure(
              RestoreError.INVALID_MAP_STATE, "Saved map contains a connection to an unknown node");
        }
      }
    }
    return RestoreResult.success("");
  }

  private RestoreResult validateMapNode(MapNodeSaveData nodeData, Set<Integer> nodeIds) {
    if (nodeData == null) {
      return RestoreResult.failure(
          RestoreError.INVALID_MAP_STATE, "Saved map contains a null node");
    }
    if (!nodeIds.add(nodeData.nodeId)) {
      return RestoreResult.failure(
          RestoreError.INVALID_MAP_STATE, "Saved map contains duplicate node IDs");
    }
    try {
      RoomType.valueOf(nodeData.roomType);
      NodeState.valueOf(nodeData.state);
    } catch (RuntimeException exception) {
      return RestoreResult.failure(
          RestoreError.INVALID_MAP_STATE, "Saved map contains an unknown room type or node state");
    }
    if (nodeData.connectionIds == null) {
      return RestoreResult.failure(
          RestoreError.INVALID_MAP_STATE, "Saved map node is missing connection data");
    }
    return RestoreResult.success("");
  }

  private void restorePlayer(PlayerSaveData playerData) {
    playerState.restore(playerData.currentHealth, playerData.maxHealth, playerData.gold);
  }

  private void restoreDeck(DeckSaveData deckData) {
    playerDeck.clear();
    playerDeck.addCards(deckData.cardIds);
  }

  private MapGraph buildMapGraph(MapSaveData mapData) {
    Map<Integer, MapNode> nodes = new HashMap<>();
    for (MapNodeSaveData nodeData : mapData.nodes) {
      MapNode node = new MapNode(nodeData.nodeId, RoomType.valueOf(nodeData.roomType));
      node.setState(NodeState.valueOf(nodeData.state));
      nodes.put(nodeData.nodeId, node);
    }

    MapGraph mapGraph = new MapGraph(nodes, false);
    for (MapNodeSaveData nodeData : mapData.nodes) {
      for (Integer connectionId : nodeData.connectionIds) {
        mapGraph.connectNodes(nodeData.nodeId, connectionId);
      }
    }
    mapGraph.restoreCurrentNode(mapData.currentNodeId);
    return mapGraph;
  }

  private String resolveResumeScreen(SaveGameData data) {
    if (data.progress != null
        && data.progress.resumeScreen != null
        && !data.progress.resumeScreen.isBlank()) {
      return data.progress.resumeScreen;
    }
    if (data.metadata != null && data.metadata.resumeScreen != null) {
      return data.metadata.resumeScreen;
    }
    return "";
  }
}
