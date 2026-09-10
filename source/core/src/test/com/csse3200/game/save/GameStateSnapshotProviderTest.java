package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
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
    // health=80, baseAttack=5 (arbitrary, unused here), maxHealth=100
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(80, 5, 100))
            .addComponent(new InventoryComponent(50));
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    SaveGameData data = new GameStateSnapshotProvider(player, deck, runState).capture();

    assertEquals(80, data.player.currentHealth);
    assertEquals(100, data.player.maxHealth);
    assertEquals(50, data.player.gold);
  }

  @Test
  void capturesMapNodesWithConnectionIdsNotObjectReferences() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 5, 100));
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck();
    RunState runState = buildRunStateWithSingleNode();

    SaveGameData data = new GameStateSnapshotProvider(player, deck, runState).capture();

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
