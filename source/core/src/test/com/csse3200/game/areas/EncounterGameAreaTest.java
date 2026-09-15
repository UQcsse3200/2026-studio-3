package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.encounters.integration.EncounterFlowController;
import com.csse3200.game.maps.RoomType;
import org.junit.jupiter.api.Test;

class EncounterGameAreaTest {
  @Test
  void shouldRouteEventNodeToChanceEncounter() {
    assertEquals(
        EncounterFlowController.EncounterType.CHANCE,
        EncounterGameArea.encounterTypeFor(RoomType.EVENT));
  }

  @Test
  void shouldRouteShopNodeToShopEncounter() {
    assertEquals(
        EncounterFlowController.EncounterType.SHOP,
        EncounterGameArea.encounterTypeFor(RoomType.SHOP));
  }

  @Test
  void shouldRejectCombatAndMissingRoomTypes() {
    assertThrows(
        IllegalArgumentException.class, () -> EncounterGameArea.encounterTypeFor(RoomType.COMBAT));
    assertThrows(IllegalArgumentException.class, () -> EncounterGameArea.encounterTypeFor(null));
  }
}
