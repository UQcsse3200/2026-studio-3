package com.csse3200.game.components.enemy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyAnimationControllerTest {

  private AnimationRenderComponent animator;
  private Entity enemy;

  @BeforeEach
  void setUp() {
    animator = mock(AnimationRenderComponent.class);
    enemy = new Entity();
    enemy.addComponent(animator);
    enemy.addComponent(new EnemyAnimationController());
    enemy.create();
  }

  @Test
  void shouldStartIdleOnCreate() {
    verify(animator).startAnimation("idle");
  }

  @Test
  void shouldPlayHurtWhenDamaged() {
    enemy.getEvents().trigger("enemyDamaged", 5);

    verify(animator).startAnimation("hurt");
  }

  @Test
  void shouldPlayHurtWhenDefeated() {
    enemy.getEvents().trigger("enemyDefeated");

    verify(animator).startAnimation("hurt");
  }

  @Test
  void shouldReturnToIdleAfterHurtFinishes() {
    when(animator.getCurrentAnimation()).thenReturn("hurt");
    when(animator.isFinished()).thenReturn(true);

    enemy.update();

    verify(animator, times(2)).startAnimation("idle");
  }

  @Test
  void shouldStayOnHurtWhileItIsStillPlaying() {
    when(animator.getCurrentAnimation()).thenReturn("hurt");
    when(animator.isFinished()).thenReturn(false);

    enemy.update();

    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldNotRestartIdleWhileIdleIsPlaying() {
    when(animator.getCurrentAnimation()).thenReturn("idle");
    when(animator.isFinished()).thenReturn(true);

    enemy.update();

    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldTolerateANullCurrentAnimation() {
    when(animator.getCurrentAnimation()).thenReturn(null);

    enemy.update();

    verify(animator, never()).startAnimation("hurt");
  }
}
