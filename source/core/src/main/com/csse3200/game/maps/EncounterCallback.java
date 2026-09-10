package com.csse3200.game.maps;

/** Callback interface used by encounters to report completion. */
public interface EncounterCallback {

  /**
   * Called when an encounter finishes.
   *
   * @param nodeId id of the completed map node
   * @param success whether the encounter was completed successfully
   */
  void onEncounterComplete(Integer nodeId, boolean success);
}
