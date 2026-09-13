package com.csse3200.game.components.enemy;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * Drives an enemy's {@link AnimationRenderComponent} from combat events.
 *
 * <p>The enemy loops {@code idle}, flashes {@code hurt} when it takes damage, and plays optional
 * boss animations for casting, defending, and dying. Non-looping action animations return to idle;
 * death holds on its final frame.
 */
public class EnemyAnimationController extends Component {
  private AnimationRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("enemyDamaged", this::onDamaged);
    entity.getEvents().addListener("enemyDefeated", this::onDefeated);
    entity.getEvents().addListener("enemyCast", this::onCast);
    entity.getEvents().addListener("enemyDefend", this::onDefend);
    animator.startAnimation("idle");
  }

  @Override
  public void update() {
    String currentAnimation = animator.getCurrentAnimation();
    if (("hurt".equals(currentAnimation)
            || "cast".equals(currentAnimation)
            || "defend".equals(currentAnimation))
        && animator.isFinished()) {
      animator.startAnimation("idle");
    }
  }

  private void onDamaged(int amount) {
    animator.startAnimation("hurt");
  }

  private void onDefeated() {
    startIfAvailable("death", "hurt");
  }

  private void onCast() {
    startIfAvailable("cast", "idle");
  }

  private void onDefend() {
    startIfAvailable("defend", "idle");
  }

  private void startIfAvailable(String animation, String fallback) {
    animator.startAnimation(animator.hasAnimation(animation) ? animation : fallback);
  }
}
