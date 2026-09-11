package com.csse3200.game.components.enemy;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * Drives an enemy's {@link AnimationRenderComponent} from combat events.
 *
 * <p>The enemy loops {@code idle}, flashes {@code hurt} when it takes damage or is defeated, and
 * falls back to {@code idle} once a non-looping flash has finished playing.
 */
public class EnemyAnimationController extends Component {
  private AnimationRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("enemyDamaged", this::onDamaged);
    entity.getEvents().addListener("enemyDefeated", this::onDefeated);
    animator.startAnimation("idle");
  }

  @Override
  public void update() {
    if ("hurt".equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("idle");
    }
  }

  private void onDamaged(int amount) {
    animator.startAnimation("hurt");
  }

  private void onDefeated() {
    animator.startAnimation("hurt");
  }
}
