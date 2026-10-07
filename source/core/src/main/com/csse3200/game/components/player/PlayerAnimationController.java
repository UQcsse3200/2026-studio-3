package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

public class PlayerAnimationController extends Component {
  private AnimationRenderComponent animator;
  private int lastKnownHealth;

  @Override
  public void create() {
    super.create();
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    animator = entity.getComponent(AnimationRenderComponent.class);
    lastKnownHealth = stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::onHealthUpdate);
    entity.getEvents().addListener("playerAttack", this::onAttack);
    animator.startAnimation("idle");
  }

  private void onHealthUpdate(int health, int maxHealth) {
    if (health < lastKnownHealth) {
      animator.startAnimation("hurt");
    }
    lastKnownHealth = health;
  }

  private void onAttack() {
    animator.startAnimation("attack");
  }

  @Override
  public void update() {
    String currentAnimation = animator.getCurrentAnimation();
    boolean animationFinished = animator.isFinished();
    if (("hurt".equals(currentAnimation) || "attack".equals(currentAnimation))
        && animationFinished) {
      animator.startAnimation("idle");
    }
  }
}
