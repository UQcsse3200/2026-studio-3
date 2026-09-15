package com.csse3200.game.save;

import java.util.ArrayList;
import java.util.List;

/** Serializable encounter and reward progress for the current run. */
public class ProgressSaveData {
  public String pendingRewardId = "";
  public String resumeScreen = "";
  public List<BestiaryProgressSaveData> bestiary = new ArrayList<>();

  /** Required for JSON deserialisation. */
  public ProgressSaveData() {}

  public ProgressSaveData(String pendingRewardId, String resumeScreen) {
    this(pendingRewardId, resumeScreen, List.of());
  }

  public ProgressSaveData(
      String pendingRewardId, String resumeScreen, List<BestiaryProgressSaveData> bestiary) {
    this.pendingRewardId = pendingRewardId == null ? "" : pendingRewardId;
    this.resumeScreen = resumeScreen == null ? "" : resumeScreen;
    this.bestiary = bestiary == null ? new ArrayList<>() : new ArrayList<>(bestiary);
  }
}
