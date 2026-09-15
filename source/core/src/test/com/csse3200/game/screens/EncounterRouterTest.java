package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.GdxGame;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class EncounterRouterTest {

  @Test
  void combatRoomsOpenTheBattleScreen() {
    assertEquals(GdxGame.ScreenType.BATTLE_SCREEN, EncounterRouter.screenFor(RoomType.COMBAT));
  }

  @Test
  void eliteRoomsOpenTheBattleScreen() {
    assertEquals(GdxGame.ScreenType.BATTLE_SCREEN, EncounterRouter.screenFor(RoomType.ELITE));
  }

  @Test
  void bossRoomsOpenTheBattleScreen() {
    assertEquals(GdxGame.ScreenType.BATTLE_SCREEN, EncounterRouter.screenFor(RoomType.FINAL));
  }

  @Test
  void eventRoomsOpenTheEncounterScreen() {
    assertEquals(GdxGame.ScreenType.ENCOUNTER, EncounterRouter.screenFor(RoomType.EVENT));
  }

  @Test
  void shopRoomsOpenTheEncounterScreen() {
    assertEquals(GdxGame.ScreenType.ENCOUNTER, EncounterRouter.screenFor(RoomType.SHOP));
  }

  /** A start node is where the run begins, not something the player fights or shops in. */
  @Test
  void startRoomsOpenNothing() {
    assertNull(EncounterRouter.screenFor(RoomType.START));
    assertFalse(EncounterRouter.startsEncounter(RoomType.START));
  }

  @Test
  void unknownRoomOpensNothing() {
    assertNull(EncounterRouter.screenFor(null));
    assertFalse(EncounterRouter.startsEncounter(null));
  }

  @Test
  void everyRoomTypeIsAccountedFor() {
    for (RoomType roomType : RoomType.values()) {
      assertDoesNotThrow(() -> EncounterRouter.screenFor(roomType), roomType + " is unhandled");
    }
  }

  @Test
  void startsEncounterMatchesScreenFor() {
    for (RoomType roomType : RoomType.values()) {
      assertEquals(
          EncounterRouter.screenFor(roomType) != null, EncounterRouter.startsEncounter(roomType));
    }
  }
}
