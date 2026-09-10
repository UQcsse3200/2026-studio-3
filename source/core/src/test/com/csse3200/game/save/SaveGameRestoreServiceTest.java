package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SaveGameRestoreServiceTest {
  private static final String STRIKE = "strike";
  private static final String DEFEND = "defend";
  private static final String BANDAGE = "bandage";

  @Test
  void restoresPlayerDeckAndMapState() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(12, 5, 50))
            .addComponent(new InventoryComponent(3));
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);

    SaveGameData saveData = validSaveData();
    saveData.player = new PlayerSaveData(80, 100, 42, 0);
    saveData.deck = new DeckSaveData(List.of(DEFEND, BANDAGE));
    saveData.progress.resumeScreen = "MAP";

    RestoreResult result = new SaveGameRestoreService(player, deck, runState).restore(saveData);

    assertTrue(result.success());
    assertEquals("MAP", result.resumeScreen());
    assertEquals(80, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getMaxHealth());
    assertEquals(42, player.getComponent(InventoryComponent.class).getGold());
    assertEquals(List.of(DEFEND, BANDAGE), deck.getCardIds());
    assertNotNull(runState.getMapGraph());
    assertEquals(1, runState.getMapGraph().getCurrentNode().getNodeId());
    assertEquals(2, runState.getActiveNodeId());
    assertEquals(NodeState.COMPLETED, runState.getMapGraph().getNode(0).getState());
    assertEquals(NodeState.CURRENT, runState.getMapGraph().getNode(1).getState());
  }

  @Test
  void rejectsInvalidDeckWithoutMutatingLiveState() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(12, 5, 50))
            .addComponent(new InventoryComponent(3));
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);

    SaveGameData saveData = validSaveData();
    saveData.player = new PlayerSaveData(80, 100, 42, 0);
    saveData.deck = new DeckSaveData(List.of("unknown_card"));

    RestoreResult result = new SaveGameRestoreService(player, deck, runState).restore(saveData);

    assertFalse(result.success());
    assertEquals(RestoreError.INVALID_DECK_STATE, result.error());
    assertEquals(12, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(3, player.getComponent(InventoryComponent.class).getGold());
    assertEquals(List.of(STRIKE), deck.getCardIds());
    assertEquals(0, runState.getMapGraph().getCurrentNode().getNodeId());
  }

  @Test
  void rejectsInvalidMapWithoutMutatingLiveState() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(12, 5, 50))
            .addComponent(new InventoryComponent(3));
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();
    runState.startRun(existingMap(), 0);

    SaveGameData saveData = validSaveData();
    saveData.map.nodes.get(0).connectionIds.add(99);

    RestoreResult result = new SaveGameRestoreService(player, deck, runState).restore(saveData);

    assertFalse(result.success());
    assertEquals(RestoreError.INVALID_MAP_STATE, result.error());
    assertEquals(12, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(List.of(STRIKE), deck.getCardIds());
    assertEquals(0, runState.getMapGraph().getCurrentNode().getNodeId());
  }

  @Test
  void validatesConstructorArguments() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(12, 5, 50))
            .addComponent(new InventoryComponent(3));
    PlayerDeck deck = testDeck(List.of(STRIKE));
    RunState runState = new RunState();

    assertThrows(
        IllegalArgumentException.class, () -> new SaveGameRestoreService(null, deck, runState));
    assertThrows(
        IllegalArgumentException.class, () -> new SaveGameRestoreService(player, null, runState));
    assertThrows(
        IllegalArgumentException.class, () -> new SaveGameRestoreService(player, deck, null));
  }

  private SaveGameData validSaveData() {
    SaveGameData data = new SaveGameData();
    data.player = new PlayerSaveData(30, 60, 10, 0);
    data.deck = new DeckSaveData(List.of(STRIKE));
    data.map =
        new MapSaveData(
            List.of(
                new MapNodeSaveData(
                    0, RoomType.COMBAT.name(), NodeState.COMPLETED.name(), List.of(1)),
                new MapNodeSaveData(
                    1, RoomType.SHOP.name(), NodeState.CURRENT.name(), List.of(0, 2)),
                new MapNodeSaveData(
                    2, RoomType.EVENT.name(), NodeState.AVAILABLE.name(), List.of(1))),
            1,
            2);
    data.progress = new ProgressSaveData("", "");
    return data;
  }

  private MapGraph existingMap() {
    MapNode node = new MapNode(0, RoomType.COMBAT);
    node.setState(NodeState.CURRENT);
    return new MapGraph(Map.of(0, node), false);
  }

  private PlayerDeck testDeck(List<String> cardIds) {
    return new PlayerDeck(TestCardService.withCards(STRIKE, DEFEND, BANDAGE), cardIds);
  }
}
