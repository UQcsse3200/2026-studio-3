package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.BossDialogueConfig.Dialogue;
import com.csse3200.game.components.enemy.EnemyAI.BossAI;
import com.csse3200.game.components.enemy.EnemyAI.BossAI.BossPhase;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIContext;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BossDialogueComponentTest {
  @Test
  void shouldShowOpeningAndBlockCardsUntilClicked() {
    try (Fixture fixture = new Fixture()) {
      fixture.component.create();

      assertTrue(fixture.component.isShowing());
      assertTrue(fixture.controller.isCardPlayBlocked());
      assertEquals(BossPhase.PHASE_ONE, fixture.component.getDisplayedPhase());
      assertEquals("Opening line", fixture.component.getDisplayedText());

      InputEvent click = new InputEvent();
      click.setType(InputEvent.Type.touchDown);
      fixture.component.getOverlay().fire(click);

      assertFalse(fixture.component.isShowing());
      assertFalse(fixture.controller.isCardPlayBlocked());
      assertNull(fixture.component.getDisplayedPhase());
    }
  }

  @Test
  void shouldShowEveryPhaseOnlyOnce() {
    try (Fixture fixture = new Fixture()) {
      fixture.component.create();
      fixture.component.dismiss();

      fixture.bossAI.decide(fixture.context(65));
      assertEquals(BossPhase.PHASE_TWO, fixture.component.getDisplayedPhase());
      assertEquals("Phase two line", fixture.component.getDisplayedText());

      fixture.component.dismiss();
      fixture.bossAI.decide(fixture.context(60));
      assertFalse(fixture.component.isShowing());

      fixture.bossAI.decide(fixture.context(30));
      assertEquals(BossPhase.ENRAGED, fixture.component.getDisplayedPhase());
      assertEquals("Enraged line", fixture.component.getDisplayedText());
    }
  }

  @Test
  void shouldCloseAfterConfiguredDuration() {
    try (Fixture fixture = new Fixture()) {
      fixture.component.create();

      fixture.stage.act(1.1f);
      fixture.stage.act(0f);

      assertFalse(fixture.component.isShowing());
      assertFalse(fixture.controller.isCardPlayBlocked());
    }
  }

  @Test
  void shouldAdvanceThroughSampledFrames() {
    try (Fixture fixture = new Fixture()) {
      fixture.component.create();

      assertEquals(0, fixture.component.getDisplayedFrameIndex());
      fixture.component.advanceAnimation(0.04f);
      assertEquals(1, fixture.component.getDisplayedFrameIndex());
      fixture.component.advanceAnimation(0.95f);
      assertEquals(29, fixture.component.getDisplayedFrameIndex());
    }
  }

  private static final class Fixture implements AutoCloseable {
    private final Stage stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    private final BossAI bossAI = new BossAI();
    private final BattleController controller;
    private final BossDialogueComponent component;

    private Fixture() {
      Entity player = new Entity().addComponent(new CombatStatsComponent(100, 0));
      Entity boss = new Entity().addComponent(new CombatStatsComponent(100, 10));
      controller = new BattleController(player, List.of(boss));

      BossDialogueConfig config = new BossDialogueConfig();
      config.opening = dialogue("Opening line", "opening.png");
      config.phaseTwo = dialogue("Phase two line", "phase-two.png");
      config.enraged = dialogue("Enraged line", "enraged.png");
      component =
          new BossDialogueComponent(
              stage,
              bossAI,
              config,
              controller,
              () -> new Rectangle(850f, 300f, 180f, 220f),
              (ignored, frameCount) -> frames(frameCount));
    }

    private static Drawable[] frames(int frameCount) {
      Drawable[] frames = new Drawable[frameCount];
      for (int i = 0; i < frameCount; i++) {
        frames[i] = new BaseDrawable();
      }
      return frames;
    }

    private static Dialogue dialogue(String text, String image) {
      Dialogue dialogue = new Dialogue();
      dialogue.text = text;
      dialogue.image = image;
      dialogue.frameCount = 30;
      dialogue.durationSeconds = 1f;
      return dialogue;
    }

    private EnemyAIContext context(int bossHealth) {
      return new EnemyAIContext(100, bossHealth, 100, 10, 0, EnemyIntent.unknown(), 1);
    }

    @Override
    public void close() {
      component.dispose();
      stage.dispose();
    }
  }
}
