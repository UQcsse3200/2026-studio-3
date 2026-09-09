package com.csse3200.game.save;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Serializable encounter and reward progress for the current run. */
public class ProgressSaveData {
  public List<String> completedEncounterIds = new ArrayList<>();
  public String pendingRewardId = "";
  public String resumeScreen = "";

  /** Required for JSON deserialisation. */
  public ProgressSaveData() {}

  public ProgressSaveData(
      Collection<String> completedEncounterIds, String pendingRewardId, String resumeScreen) {
    if (completedEncounterIds != null) {
      this.completedEncounterIds.addAll(completedEncounterIds);
    }
    this.pendingRewardId = pendingRewardId == null ? "" : pendingRewardId;
    this.resumeScreen = resumeScreen == null ? "" : resumeScreen;
  }
}
