package com.csse3200.game.save;

import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures the live run as a {@link SaveGameData} snapshot.
 *
 * <p>The player, deck and map references are all run-scoped rather than screen-owned, so capturing
 * a save never depends on a rendered player entity still being alive.
 */
public class GameStateSnapshotProvider implements SaveGameSnapshotProvider {
  private final PlayerRunState playerState;
  private final PlayerDeck playerDeck;
  private final RunState runState;

  public GameStateSnapshotProvider(
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

  @Override
  public SaveGameData capture() {
    SaveGameData data = new SaveGameData();
    data.player = capturePlayer();
    data.deck = captureDeck();
    data.map = captureMap();
    data.progress = captureProgress();
    return data;
  }

  private PlayerSaveData capturePlayer() {
    // Piety is confirmed not implemented for this sprint (Amber_Teng, Team 7, 9/10) — dropped
    // from scope in favor of concrete Status Effects. PlayerSaveData.piety is a leftover field
    // from an earlier design; left at 0 intentionally, not a placeholder awaiting a real source.
    int piety = 0;

    return new PlayerSaveData(
        playerState.getCurrentHealth(), playerState.getMaxHealth(), playerState.getGold(), piety);
  }

  private DeckSaveData captureDeck() {
    return new DeckSaveData(playerDeck.getCardIds());
  }

  private MapSaveData captureMap() {
    List<MapNodeSaveData> nodeSnapshots = new ArrayList<>();
    MapGraph mapGraph = runState.getMapGraph();

    for (MapNode node : mapGraph.getNodes().values()) {
      List<Integer> connectionIds = new ArrayList<>();
      for (MapNode connected : node.getConnections()) {
        connectionIds.add(connected.getNodeId());
      }
      nodeSnapshots.add(
          new MapNodeSaveData(
              node.getNodeId(), node.getRoomType().name(), node.getState().name(), connectionIds));
    }

    Integer currentNodeId =
        mapGraph.getCurrentNode() != null ? mapGraph.getCurrentNode().getNodeId() : null;

    // RunState.activeNodeId tracks the node whose encounter is currently being resolved, which
    // is exactly the 'active encounter node' concept — not the player's physical position (that
    // was an earlier mistake, caught by the end-to-end test). No Team 2 clarification needed
    // for this field.
    Integer activeEncounterNodeId = runState.getActiveNodeId();

    return new MapSaveData(nodeSnapshots, currentNodeId, activeEncounterNodeId);
  }

  private ProgressSaveData captureProgress() {
    // pendingRewardId: confirmed empty with Team 2 (Joel, 9/10) — Chance/Shop outcomes apply
    // immediately, no pending-reward phase exists. Revisit only if a reward-claim screen is
    // added later.
    String pendingRewardId = "";

    // resumeScreen: agreed with Team 2 (Joel, 9/10) that a load should always return to the Map,
    // never resume mid-encounter, since outcomes apply immediately and nothing is ever left
    // in-progress to replay. Always MAP for now — revisit if that rule changes.
    String resumeScreen = GdxGame.ScreenType.MAP.name();

    return new ProgressSaveData(pendingRewardId, resumeScreen);
  }
}
