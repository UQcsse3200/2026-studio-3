package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EncounterScreenTest {

  @Test
  void shouldDetectZeroHealthPlayerAsDefeated() {
    CombatStatsComponent stats = new CombatStatsComponent(5, 1);
    stats.setHealth(0);
    Entity player = new Entity().addComponent(stats);

    assertTrue(EncounterScreen.isPlayerDefeated(player));
  }

  @Test
  void shouldNotDetectPositiveHealthPlayerAsDefeated() {
    CombatStatsComponent stats = new CombatStatsComponent(5, 1);
    Entity player = new Entity().addComponent(stats);

    assertFalse(EncounterScreen.isPlayerDefeated(player));
  }

  @Test
  void shouldSafelyHandleMissingPlayerStats() {
    assertFalse(EncounterScreen.isPlayerDefeated(null));
    assertFalse(EncounterScreen.isPlayerDefeated(new Entity()));
  }

  @Test
  void shouldRequestAutosaveAfterSuccessfulShopOrEvent() {
    for (RoomType roomType : new RoomType[] {RoomType.SHOP, RoomType.EVENT}) {
      RunState runState = activeEncounter(roomType);
      GdxGame game = mock(GdxGame.class);

      EncounterScreen.completeEncounterAndRequestAutosave(game, runState, true);

      assertEquals(NodeState.COMPLETED, runState.getMapGraph().getNode(1).getState());
      verify(game).requestAutosaveAfterEncounter();
    }
  }

  @Test
  void shouldNotRequestAutosaveAfterFailedOrAbandonedShopOrEvent() {
    for (RoomType roomType : new RoomType[] {RoomType.SHOP, RoomType.EVENT}) {
      RunState runState = activeEncounter(roomType);
      GdxGame game = mock(GdxGame.class);

      EncounterScreen.completeEncounterAndRequestAutosave(game, runState, false);

      assertEquals(NodeState.CURRENT, runState.getMapGraph().getNode(1).getState());
      verify(game, never()).requestAutosaveAfterEncounter();
    }
  }

  private RunState activeEncounter(RoomType roomType) {
    MapNode start = new MapNode(0, RoomType.COMBAT);
    MapNode encounter = new MapNode(1, roomType);
    start.addConnection(encounter);
    RunState runState = new RunState();
    assertTrue(runState.startRun(new MapGraph(Map.of(0, start, 1, encounter), false), 0));
    assertTrue(runState.getMapGraph().moveToNode(1));
    runState.enterEncounter(1);
    return runState;
  }
}
