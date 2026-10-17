package com.csse3200.game.components.enemy;

import com.csse3200.game.components.enemy.EnemyAI.BossAI.BossPhase;
import com.csse3200.game.files.FileLoader;
import java.util.Arrays;
import java.util.Objects;

/** Configured story images, transcripts and display times for the Boss encounter. */
public class BossDialogueConfig {
  public static final String DEFAULT_FILE = "configs/bossDialogue.json";

  public Dialogue opening;
  public Dialogue phaseTwo;
  public Dialogue enraged;

  /** One phase's dialogue presentation. Public fields are populated by LibGDX JSON. */
  public static class Dialogue {
    public String text;
    public String image;
    public int frameCount;
    public float durationSeconds;
  }

  /** Loads and validates the normal Boss dialogue configuration. */
  public static BossDialogueConfig load() {
    return load(DEFAULT_FILE);
  }

  /** Loads and validates Boss dialogue configuration from an internal asset path. */
  public static BossDialogueConfig load(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("filename cannot be blank");
    }

    BossDialogueConfig config = FileLoader.readClass(BossDialogueConfig.class, filename);
    if (config == null) {
      throw new IllegalStateException("Unable to load Boss dialogue config: " + filename);
    }
    config.validate();
    return config;
  }

  /** Returns the presentation assigned to a Boss phase. */
  public Dialogue dialogueFor(BossPhase phase) {
    Objects.requireNonNull(phase, "phase cannot be null");
    return switch (phase) {
      case PHASE_ONE -> opening;
      case PHASE_TWO -> phaseTwo;
      case ENRAGED -> enraged;
    };
  }

  /** Returns every image path for preloading by the battle screen. */
  public String[] texturePaths() {
    return Arrays.stream(BossPhase.values())
        .map(this::dialogueFor)
        .map(d -> d.image)
        .toArray(String[]::new);
  }

  private void validate() {
    for (BossPhase phase : BossPhase.values()) {
      Dialogue dialogue = dialogueFor(phase);
      if (dialogue == null
          || dialogue.text == null
          || dialogue.text.isBlank()
          || dialogue.image == null
          || dialogue.image.isBlank()
          || dialogue.frameCount <= 0
          || dialogue.durationSeconds <= 0f) {
        throw new IllegalStateException("Invalid Boss dialogue entry for " + phase);
      }
      dialogue.text = dialogue.text.trim();
      dialogue.image = dialogue.image.trim();
    }
  }
}
