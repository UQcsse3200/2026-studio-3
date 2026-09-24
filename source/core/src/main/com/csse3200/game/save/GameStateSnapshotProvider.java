package com.csse3200.game.save;

import com.csse3200.game.GdxGame;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  private final BestiaryService bestiaryService;
  private final CardDiscoveryService cardDiscoveryService;

  public GameStateSnapshotProvider(
      PlayerRunState playerState,
      PlayerDeck playerDeck,
      RunState runState,
      BestiaryService bestiaryService,
      CardDiscoveryService cardDiscoveryService) {
    if (playerState == null) {
      throw new IllegalArgumentException("playerState must not be null");
    }
    if (playerDeck == null) {
      throw new IllegalArgumentException("playerDeck must not be null");
    }
    if (runState == null) {
      throw new IllegalArgumentException("runState must not be null");
    }
    if (bestiaryService == null) {
      throw new IllegalArgumentException("bestiaryService must not be null");
    }
    if (cardDiscoveryService == null) {
      throw new IllegalArgumentException("cardDiscoveryService must not be null");
    }
    this.playerState = playerState;
    this.playerDeck = playerDeck;
    this.runState = runState;
    this.bestiaryService = bestiaryService;
    this.cardDiscoveryService = cardDiscoveryService;
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
    // PlayerSaveData.piety is a snapshot-only mirror of the player's map-progression "level" —
    // see #Sprint 3 rename discussion with Josie/Aidan/Linh, 9/22 (flagged by Amber in review,
    // PR #303). MapGraph is the single authoritative source for level: it is restored directly
    // by SaveGameRestoreService (restoreCurrentNode), and PlayerStatsTopDisplay reads the live
    // value straight from RunState.getMapProgression(), never from this field. This field is
    // therefore captured for save-file schema completeness only — nothing currently reads it
    // back on restore, and nothing needs to, since the real source of truth is never lost.
    // Derived from the current node's height rather than RunState.getMapProgression() itself,
    // since that only returns non-zero while mid-encounter and would read 0 for essentially
    // every real save (saves happen from the map screen).
    MapGraph mapGraph = runState.getMapGraph();
    MapNode currentNode = mapGraph == null ? null : mapGraph.getCurrentNode();
    int level = currentNode == null ? 0 : currentNode.getHeight();

    return new PlayerSaveData(
        playerState.getCurrentHealth(), playerState.getMaxHealth(), playerState.getGold(), level);
  }

  private DeckSaveData captureDeck() {
    List<CardInstanceSaveData> cardSnapshots = new ArrayList<>();
    for (CardInstance card : playerDeck.getCards()) {
      cardSnapshots.add(
          new CardInstanceSaveData(card.instanceId(), card.cardId(), card.upgradeLevel()));
    }
    return new DeckSaveData(cardSnapshots);
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

    List<BestiaryProgressSaveData> bestiaryProgress = new ArrayList<>();
    for (Map.Entry<String, BestiaryUnlockState> entry :
        bestiaryService.getProgressSnapshot().entrySet()) {
      if (entry.getValue() != BestiaryUnlockState.LOCKED) {
        bestiaryProgress.add(new BestiaryProgressSaveData(entry.getKey(), entry.getValue().name()));
      }
    }

    List<CardProgressSaveData> cardProgress = new ArrayList<>();
    for (Map.Entry<String, CardUnlockState> entry :
        cardDiscoveryService.getProgressSnapshot().entrySet()) {
      if (entry.getValue() != CardUnlockState.LOCKED) {
        cardProgress.add(new CardProgressSaveData(entry.getKey(), entry.getValue().name()));
      }
    }

    ProgressSaveData progress =
        new ProgressSaveData(pendingRewardId, resumeScreen, bestiaryProgress, cardProgress);
    progress.encounterSeed = runState.getEncounterSeed();
    return progress;
  }
}
