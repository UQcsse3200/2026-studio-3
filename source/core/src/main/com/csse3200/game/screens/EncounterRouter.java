package com.csse3200.game.screens;

import com.csse3200.game.GdxGame;
import com.csse3200.game.maps.RoomType;

/**
 * Works out which screen owns the encounter for a given room type.
 *
 * <p>This sits outside {@link MapScreen} so the decision can be unit tested. Screens need a live
 * graphics context, so routing logic kept inside one cannot be covered by tests.
 *
 * <p>Combat, elite and boss rooms are run by Team 3's battle system. Shop and event rooms are run
 * by Team 2's encounter system. A start node is a position on the map rather than an encounter, so
 * it opens nothing.
 */
public final class EncounterRouter {

  private EncounterRouter() {}

  /**
   * Returns the screen to open for a room type.
   *
   * @param roomType type of the selected room, may be null
   * @return screen that owns the encounter, or null if the room does not start one
   */
  public static GdxGame.ScreenType screenFor(RoomType roomType) {
    if (roomType == null) {
      return null;
    }

    return switch (roomType) {
      case COMBAT, ELITE, FINAL -> GdxGame.ScreenType.BATTLE_SCREEN;
      case EVENT, SHOP -> GdxGame.ScreenType.ENCOUNTER;
      case START -> null;
    };
  }

  /**
   * Whether selecting a room of this type should start an encounter.
   *
   * @param roomType type of the selected room, may be null
   * @return true if the room opens an encounter screen
   */
  public static boolean startsEncounter(RoomType roomType) {
    return screenFor(roomType) != null;
  }
}
