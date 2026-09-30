package com.csse3200.game.components.enemy;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * Turns combat events emitted by an enemy into colour-flash visual feedback.
 *
 * <p>Damage, armour changes, enrage events, and intent changes are translated into tint effects.
 * The actual tint rendering is handled by {@link AnimationRenderComponent}.
 *
 * <p>The {@code updateArmour} event from {@link CombatStatsComponent} is emitted whenever armour
 * increases or decreases. This component therefore tracks the previous armour value and plays the
 * defend flash only when armour has increased.
 */
public class EnemyCombatEffectsComponent extends Component {
  private static final float DAMAGE_FLASH_SECONDS = 0.2f;
  private static final float DEFEND_FLASH_SECONDS = 0.2f;
  private static final float ATTACK_TELEGRAPH_FLASH_SECONDS = 0.3f;

  private static final Color DAMAGE_COLOR = Color.RED;
  private static final Color DEFEND_COLOR = Color.CYAN;
  private static final Color ATTACK_TELEGRAPH_COLOR = Color.YELLOW;
  private static final Color ENRAGE_COLOR = new Color(1f, 0.3f, 0.3f, 1f);
  private static final Color DEFEATED_COLOR = new Color(0.35f, 0.35f, 0.35f, 1f);

  private AnimationRenderComponent animator;
  private int lastArmour;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    lastArmour = stats == null ? 0 : stats.getArmour();

    entity.getEvents().addListener("enemyDamaged", this::onDamaged);
    entity.getEvents().addListener("updateArmour", this::onArmourUpdated);
    entity.getEvents().addListener("enemyEnraged", this::onEnraged);
    entity.getEvents().addListener("intentChanged", this::onIntentChanged);
    entity.getEvents().addListener("enemyDefeated", this::onDefeated);
  }

  private void onDamaged(int amount) {
    animator.flashTint(DAMAGE_COLOR, DAMAGE_FLASH_SECONDS);
  }

  private void onArmourUpdated(int armour) {
    if (armour > lastArmour) {
      animator.flashTint(DEFEND_COLOR, DEFEND_FLASH_SECONDS);
    }
    lastArmour = armour;
  }

  private void onEnraged() {
    animator.setPersistentTint(ENRAGE_COLOR);
  }

  // Flash when an attack intent is selected to warn the player before the attack resolves.

  private void onDefeated() {
    animator.setPersistentTint(DEFEATED_COLOR);
  }

  // 攻击意图刚决定时（还没真正命中）先给一次闪烁，当作"预警"
  private void onIntentChanged(EnemyIntent intent) {
    if (intent.getType() == IntentType.ATTACK) {
      animator.flashTint(ATTACK_TELEGRAPH_COLOR, ATTACK_TELEGRAPH_FLASH_SECONDS);
    }
  }
}
