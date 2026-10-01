package com.csse3200.game.components.enemy;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Drives an enemy's {@link AnimationRenderComponent} from combat events.
 *
 * <p>The enemy loops {@code idle}, flashes {@code hurt} when it takes damage, and plays optional
 * boss animations for casting, defending, and dying. Non-looping action animations return to idle;
 * death holds on its final frame.
 */
public class EnemyAnimationController extends Component {
  private static final float LUNGE_SECONDS = 0.3f;
  private final EventListener0 attackListener = this::onAttack;
  private final EventListener1<Integer> damagedListener = this::onDamaged;
  private final EventListener0 defeatedListener = this::onDefeated;
  private final EventListener0 castListener = this::onCast;
  private final EventListener0 defendListener = this::onDefend;
  private AnimationRenderComponent animator;
  private Vector2 attackOrigin;
  private float attackElapsed;
  private boolean defeated;
  private boolean disposed;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("enemyDamaged", damagedListener);
    entity.getEvents().addListener("enemyDefeated", defeatedListener);
    entity.getEvents().addListener("enemyCast", castListener);
    entity.getEvents().addListener("enemyDefend", defendListener);
    entity.getEvents().addListener("enemyAttack", attackListener);
    animator.startAnimation("idle");
  }

  @Override
  public void update() {
    if (defeated || disposed) {
      return;
    }
    if (attackOrigin != null) {
      attackElapsed += ServiceLocator.getTimeSource().getDeltaTime();
      if (attackElapsed >= LUNGE_SECONDS) {
        resetAttackMotion();
      } else {
        float progress = attackElapsed / LUNGE_SECONDS;
        float offset =
            (float) Math.sin(Math.PI * progress) * Math.min(0.25f, entity.getScale().x * 0.15f);
        // Battle enemies stand to the right of the player. This is presentation only.
        entity.setPosition(attackOrigin.x - offset, attackOrigin.y);
      }
    }
    String currentAnimation = animator.getCurrentAnimation();
    if (("hurt".equals(currentAnimation)
            || "attack".equals(currentAnimation)
            || "cast".equals(currentAnimation)
            || "defend".equals(currentAnimation))
        && animator.isFinished()) {
      animator.startAnimation("idle");
    }
  }

  private void onDamaged(int amount) {
    if (defeated || disposed) {
      return;
    }
    resetAttackMotion();
    animator.startAnimation("hurt");
  }

  private void onDefeated() {
    if (defeated || disposed) {
      return;
    }
    defeated = true;
    resetAttackMotion();
    startIfAvailable("death", "hurt");
  }

  private void onCast() {
    if (defeated || disposed) {
      return;
    }
    resetAttackMotion();
    startIfAvailable("cast", "idle");
  }

  private void onDefend() {
    if (defeated || disposed) {
      return;
    }
    resetAttackMotion();
    startIfAvailable("defend", "idle");
  }

  private void onAttack() {
    if (defeated || disposed) {
      return;
    }
    resetAttackMotion();
    startIfAvailable("attack", "idle");
    if (!animator.hasAnimation("attack")) {
      attackOrigin = entity.getPosition();
      attackElapsed = 0f;
    }
  }

  private void resetAttackMotion() {
    if (attackOrigin != null) {
      entity.setPosition(attackOrigin);
      attackOrigin = null;
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    resetAttackMotion();
    entity.getEvents().removeListener("enemyAttack", attackListener);
    entity.getEvents().removeListener("enemyDamaged", damagedListener);
    entity.getEvents().removeListener("enemyDefeated", defeatedListener);
    entity.getEvents().removeListener("enemyCast", castListener);
    entity.getEvents().removeListener("enemyDefend", defendListener);
    super.dispose();
  }

  private void startIfAvailable(String animation, String fallback) {
    animator.startAnimation(animator.hasAnimation(animation) ? animation : fallback);
  }
}
