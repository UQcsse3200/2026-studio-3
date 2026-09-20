package com.csse3200.game.debug;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.csse3200.game.GdxGame;
import com.csse3200.game.maps.RoomType;
import org.junit.jupiter.api.Test;

class DemoEncounterAccessTest {
  @Test
  void shopDemoOpensAStandaloneEncounter() {
    GdxGame game = mock(GdxGame.class);

    DemoEncounterAccess.open(game, RoomType.SHOP);

    verify(game).openDemoEncounter(RoomType.SHOP);
    verify(game, never()).getRunState();
  }

  @Test
  void eventDemoOpensAStandaloneEncounter() {
    GdxGame game = mock(GdxGame.class);

    DemoEncounterAccess.open(game, RoomType.EVENT);

    verify(game).openDemoEncounter(RoomType.EVENT);
    verify(game, never()).getRunState();
  }

  @Test
  void rejectsNonEncounterRooms() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DemoEncounterAccess.open(mock(GdxGame.class), RoomType.COMBAT));
  }
}
