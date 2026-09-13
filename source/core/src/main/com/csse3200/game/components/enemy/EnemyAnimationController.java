package com.csse3200.game.components.enemy;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * Drives an enemy's {@link AnimationRenderComponent} from combat events.
 *
 * <p>The enemy loops {@code idle}, flashes {@code hurt} when it takes damage, plays {@code death}
 * when defeated (holding its final frame), and falls back to {@code idle} once a {@code hurt} flash
 * has finished playing.
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
    // 有的旧敌人图集还没有 death 帧，这种情况下退回原来的 hurt 表现，避免打印错误日志
    if (animator.hasAnimation("death")) {
      // death 是一次性动画，播完后停在最后一帧，不需要像 hurt 那样切回 idle
      animator.startAnimation("death");
    } else {
      animator.startAnimation("hurt");
    }
  }
}
