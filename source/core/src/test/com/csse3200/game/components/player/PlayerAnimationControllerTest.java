package com.csse3200.game.components.player;

import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerAnimationControllerTest {
  private AnimationRenderComponent animator;
  private Entity player;
  private CombatStatsComponent stats;
  private EventHandler events;

  @BeforeEach
  void setUp() {
    animator = mock(AnimationRenderComponent.class);
    stats = new CombatStatsComponent(30, 6);
    player = spy(new Entity());
    events = spy(player.getEvents());
    doReturn(events).when(player).getEvents();
    player.addComponent(stats);
    player.addComponent(animator);
    player.addComponent(new PlayerAnimationController());
    player.create();
  }

  @Test
  void shouldStartIdleOnCreate() {
    verify(animator).startAnimation("idle");
  }

  @Test
  void shouldPlayHurtWhenDamaged() {
    player.getEvents().trigger("updateHealth", 20, 30);
    verify(animator).startAnimation("hurt");
  }

  @Test
  void healthNotDecreaseShouldNotTriggerHurtAnimation() {
    player.getEvents().trigger("updateHealth", 35, 20);
    verify(animator, never()).startAnimation("hurt");
  }

  @Test
  void shouldPlayAttackAnimationWhenAttacking() {
    player.getEvents().trigger("playerAttack");
    verify(animator).startAnimation("attack");
  }

  @Test
  void shouldReturnToIdleAfterHurtFinishes() {
    when(animator.getCurrentAnimation()).thenReturn("hurt");
    when(animator.isFinished()).thenReturn(true);

    player.update();

    verify(animator, times(2)).startAnimation("idle");
  }
}
