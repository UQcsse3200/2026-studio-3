package com.csse3200.game.save;

import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures the live run as a {@link SaveGameData} snapshot.
 *
 * <p>Player/deck/map references are passed in explicitly, matching how the rest of the codebase
 * holds them (no global "get the player" service exists) — construct this with whatever screen
 * currently owns the player entity, deck, and run state.
 */
public class GameStateSnapshotProvider implements SaveGameSnapshotProvider {
  private final Entity player;
  private final PlayerDeck playerDeck;
  private final RunState runState;

  public GameStateSnapshotProvider(Entity player, PlayerDeck playerDeck, RunState runState) {
    this.player = player;
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
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    int health = stats != null ? stats.getHealth() : 0;
    int maxHealth = stats != null ? stats.getMaxHealth() : 0;
    int gold = inventory != null ? inventory.getGold() : 0;

    // Piety is confirmed not implemented for this sprint (Amber_Teng, Team 7, 9/10) — dropped
    // from scope in favor of concrete Status Effects. PlayerSaveData.piety is a leftover field
    // from an earlier design; left at 0 intentionally, not a placeholder awaiting a real source.
    int piety = 0;

    return new PlayerSaveData(health, maxHealth, gold, piety);
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
    // TODO: no encounter-progress tracking exists yet anywhere in the codebase. Confirmed with
    // Team 2 (message sent 9/9) — implement once they clarify ownership/source.
    return new ProgressSaveData(List.of(), "", "");
  }
}
