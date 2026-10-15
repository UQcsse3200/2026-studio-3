package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class EnemyReleaseComponentTest {
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
  }

  private Entity enemyWithRelease(AnimationRenderComponent animator) {
    EnemyReleaseComponent release = new EnemyReleaseComponent();
    Entity enemy = new Entity().addComponent(animator).addComponent(release);
    enemy.setScale(2f, 4f);
    enemy.create();
    return enemy;
  }

  @Test
  void waitsForTriggerAndFinishesAfterTwoSeconds() {
    when(time.getDeltaTime()).thenReturn(0.5f);
    EnemyReleaseComponent release = new EnemyReleaseComponent();
    Entity enemy = new Entity().addComponent(release);
    enemy.create();

    enemy.update();
    assertFalse(release.isPlaying());
    assertEquals(0f, release.getElapsedTime());

    release.startRelease();
    assertTrue(release.isPlaying());

    for (int i = 0; i < 4; i++) {
      enemy.update();
    }

    assertTrue(release.isFinished());
    assertFalse(release.isPlaying());
  }

  @Test
  void risesFromFlattenedPoseToOriginalHeight() {
    when(time.getDeltaTime()).thenReturn(0.4f, 0.5f, 0.5f);
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.hasAnimation("idle")).thenReturn(true);

    Entity enemy = enemyWithRelease(animator);
    EnemyReleaseComponent release = enemy.getComponent(EnemyReleaseComponent.class);
    release.startRelease();

    enemy.update(); // Rise begins at 75% height.
    assertEquals(3f, enemy.getScale().y, 0.01f);
    verify(animator).startAnimation("idle");

    enemy.update(); // Halfway upright.
    assertEquals(3.5f, enemy.getScale().y, 0.01f);

    enemy.update(); // Back to its captured height.
    assertEquals(4f, enemy.getScale().y, 0.01f);
  }

  @Test
  void keepsPurifiedTintWhenReleaseFinishes() {
    when(time.getDeltaTime()).thenReturn(0.5f);
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.getActiveTint()).thenReturn(new Color(0.35f, 0.35f, 0.35f, 1f));

    Entity enemy = enemyWithRelease(animator);
    EnemyReleaseComponent release = enemy.getComponent(EnemyReleaseComponent.class);
    release.startRelease();

    for (int i = 0; i < 4; i++) {
      enemy.update();
    }

    ArgumentCaptor<Color> tint = ArgumentCaptor.forClass(Color.class);
    verify(animator, atLeastOnce()).setPersistentTint(tint.capture());

    Color finalTint = tint.getAllValues().get(tint.getAllValues().size() - 1);
    assertEquals(1f, finalTint.r, 0.001f);
    assertEquals(0.72f, finalTint.g, 0.001f);
    assertEquals(0.32f, finalTint.b, 0.001f);
    assertEquals(1f, finalTint.a, 0.001f);
  }

  @Test
  void repeatedStartDoesNotRestartRelease() {
    when(time.getDeltaTime()).thenReturn(0.5f);
    EnemyReleaseComponent release = new EnemyReleaseComponent();
    Entity enemy = new Entity().addComponent(release);
    enemy.create();

    release.startRelease();
    enemy.update();
    release.startRelease();

    assertEquals(0.5f, release.getElapsedTime());
  }
}
