package com.csse3200.game.components.enemy;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * Turns an enemy's own combat events into colour-flash visual feedback.
 *
 * <p>把敌人自身广播的战斗事件（受伤、护甲变化、激怒、意图变化）转成染色闪烁反馈。目前项目里还没有 战斗场景，所以这里只保证"事件 -&gt; 染色状态"的正确性；真正的染色渲染由 {@link
 * AnimationRenderComponent} 完成，等战斗场景搭好后就能直接看到效果。
 *
 * <p>{@link CombatStatsComponent} 的 {@code updateArmor} 事件在护甲增加或减少时都会触发，
 * 所以这里自己记录上一次的护甲值，只有在护甲真的增加时才播放"防御"闪烁。
 */
public class EnemyCombatEffectsComponent extends Component {
  private static final float DAMAGE_FLASH_SECONDS = 0.2f;
  private static final float DEFEND_FLASH_SECONDS = 0.2f;
  private static final float ATTACK_TELEGRAPH_FLASH_SECONDS = 0.3f;

  private static final Color DAMAGE_COLOR = Color.RED;
  private static final Color DEFEND_COLOR = Color.CYAN;
  private static final Color ATTACK_TELEGRAPH_COLOR = Color.YELLOW;
  private static final Color ENRAGE_COLOR = new Color(1f, 0.3f, 0.3f, 1f);

  private AnimationRenderComponent animator;
  private int lastArmor;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    lastArmor = stats == null ? 0 : stats.getArmor();

    entity.getEvents().addListener("enemyDamaged", this::onDamaged);
    entity.getEvents().addListener("updateArmor", this::onArmorUpdated);
    entity.getEvents().addListener("enemyEnraged", this::onEnraged);
    entity.getEvents().addListener("intentChanged", this::onIntentChanged);
  }

  private void onDamaged(int amount) {
    animator.flashTint(DAMAGE_COLOR, DAMAGE_FLASH_SECONDS);
  }

  private void onArmorUpdated(int armor) {
    if (armor > lastArmor) {
      animator.flashTint(DEFEND_COLOR, DEFEND_FLASH_SECONDS);
    }
    lastArmor = armor;
  }

  private void onEnraged() {
    animator.setPersistentTint(ENRAGE_COLOR);
  }

  // 攻击意图刚决定时（还没真正命中）先给一次闪烁，当作"预警"
  private void onIntentChanged(EnemyIntent intent) {
    if (intent.getType() == IntentType.ATTACK) {
      animator.flashTint(ATTACK_TELEGRAPH_COLOR, ATTACK_TELEGRAPH_FLASH_SECONDS);
    }
  }
}
