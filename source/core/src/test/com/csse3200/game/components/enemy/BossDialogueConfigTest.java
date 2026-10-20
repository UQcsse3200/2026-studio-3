package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.csse3200.game.components.enemy.enemyai.BossAI.BossPhase;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BossDialogueConfigTest {
  @Test
  void shouldLoadConfiguredImagesAndTranscripts() {
    BossDialogueConfig config = BossDialogueConfig.load();

    assertEquals(
        "Existence could be beautiful, but for you, it is not.",
        config.dialogueFor(BossPhase.PHASE_ONE).text);
    assertEquals(
        "images/enemies/dialogue/boss_dialogue_phase_two.png",
        config.dialogueFor(BossPhase.PHASE_TWO).image);
    assertEquals(30, config.dialogueFor(BossPhase.PHASE_TWO).frameCount);
    assertEquals(4.82f, config.dialogueFor(BossPhase.ENRAGED).durationSeconds);
    assertEquals(3, config.texturePaths().length);
  }

  @Test
  void shouldRejectBlankFilename() {
    assertThrows(IllegalArgumentException.class, () -> BossDialogueConfig.load(" "));
  }

  @Test
  void shouldRejectInvalidDialogueEntry() {
    assertThrows(
        IllegalStateException.class,
        () -> BossDialogueConfig.load("test/boss_dialogue_invalid.json"));
  }

  @Test
  void shouldProvideValidDialogueImages() {
    BossDialogueConfig config = BossDialogueConfig.load();

    for (BossPhase phase : BossPhase.values()) {
      BossDialogueConfig.Dialogue dialogue = config.dialogueFor(phase);
      Pixmap image = new Pixmap(Gdx.files.internal(dialogue.image));
      try {
        assertEquals(582 * 5, image.getWidth());
        assertEquals(156 * 6, image.getHeight());
      } finally {
        image.dispose();
      }
    }
  }
}
