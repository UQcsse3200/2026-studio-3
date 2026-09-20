package com.csse3200.game.debug;

import com.csse3200.game.GdxGame;
import com.csse3200.game.maps.RoomType;

/**
 * Temporary direct-entry support for reviewing Team 2 encounters without playing through the map.
 *
 * <p>Set {@link #ENABLED} to {@code false} before submission to remove every Demo button. The
 * preview implementation is deliberately isolated here and in {@code DemoEncounterScreen} so the
 * temporary feature is easy to remove completely.
 */
public final class DemoEncounterAccess {
  public static final boolean ENABLED = true;

  private DemoEncounterAccess() {
    throw new IllegalStateException("Utility class");
  }

  /** Opens a standalone encounter preview that never creates or changes map state. */
  public static void open(GdxGame game, RoomType roomType) {
    if (!ENABLED) {
      return;
    }
    if (roomType != RoomType.SHOP && roomType != RoomType.EVENT) {
      throw new IllegalArgumentException("Demo access only supports SHOP and EVENT rooms");
    }

    game.openDemoEncounter(roomType);
  }
}
