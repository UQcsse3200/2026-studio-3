package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GameStateSnapshotProviderTest {

  @Test
  void capturesPlayerHealthAndGold() {
    PlayerRunState playerState = new PlayerRunState(80, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    SaveGameData data = new GameStateSnapshotProvider(playerState, deck, runState).capture();

    assertEquals(80, data.player.currentHealth);
    assertEquals(100, data.player.maxHealth);
    assertEquals(50, data.player.gold);
  }

  @Test
  void capturesMapNodesWithConnectionIdsNotObjectReferences() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    SaveGameData data = new GameStateSnapshotProvider(playerState, deck, runState).capture();

    assertEquals(1, data.map.nodes.size());
    assertEquals(0, data.map.nodes.get(0).nodeId);
  }

  private RunState buildRunStateWithSingleNode() {
    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(0, new MapNode(0, RoomType.COMBAT));
    MapGraph mapGraph = new MapGraph(nodes, false);
    RunState runState = new RunState();
    runState.setMapGraph(mapGraph);
    return runState;
  }
}
