package com.csse3200.game.encounters.integration;

import com.csse3200.game.maps.EncounterCallback;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Adapts map completion functions to Team 2's encounter callback.
 *
 * <p>This keeps the encounter layer independent from whether Team 4 ultimately uses string or
 * integer node identifiers.
 */
public final class MapCompletionAdapter implements EncounterCallback {
  private final BiConsumer<Integer, Boolean> completionHandler;

  /**
   * Creates a callback adapter around a map completion function.
   *
   * @param completionHandler function that receives a node ID and completion status
   */
  public MapCompletionAdapter(BiConsumer<Integer, Boolean> completionHandler) {
    this.completionHandler =
        Objects.requireNonNull(completionHandler, "completionHandler cannot be null");
  }

  @Override
  public void onEncounterComplete(Integer nodeId, boolean success) {
    completionHandler.accept(nodeId, success);
  }
}
