package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyCombatEffectsComponentTest {

  @BeforeEach
  void registerServices() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
  }

  private Entity newEnemy() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    Array<AtlasRegion> regions = new Array<>();
    regions.add(mock(AtlasRegion.class));
    when(atlas.findRegions("test_name")).thenReturn(regions);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("test_name", 0.1f);
    animator.startAnimation("test_name");

    Entity entity = new Entity();
    entity.addComponent(new CombatStatsComponent(20, 6));
    entity.addComponent(animator);
    entity.addComponent(new EnemyCombatEffectsComponent());
    entity.create();
    return entity;
  }

  @Test
  void shouldFlashOnDamage() {
    Entity enemy = newEnemy();
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("enemyDamaged", 5);

    assertEquals(Color.RED, animator.getActiveTint());
    assertTrue(!animator.isTintPersistent());
  }

  @Test
  void shouldFlashWhenArmourIncreases() {
    Entity enemy = newEnemy();
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("updateArmour", 4);

    assertEquals(Color.CYAN, animator.getActiveTint());
  }

  // updateArmour 在护甲减少（比如被伤害吸收）时也会触发，这种情况不应该播放防御闪烁
  @Test
  void shouldNotFlashWhenArmourDecreases() {
    Entity enemy = newEnemy();
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("updateArmour", 4);
    animator.clearTint();

    enemy.getEvents().trigger("updateArmour", 1);

    assertNull(animator.getActiveTint());
  }

  @Test
  void shouldSetPersistentTintOnEnrage() {
    Entity enemy = newEnemy();
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("enemyEnraged");

    assertTrue(animator.isTintPersistent());
    assertTrue(animator.getActiveTint() != null);
  }

  @Test
  void shouldFlashOnAttackIntent() {
    Entity enemy = newEnemy();
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("intentChanged", EnemyIntent.attack(6));

    assertEquals(Color.YELLOW, animator.getActiveTint());
  }

  // 防御意图不应该触发攻击预警的黄色闪烁
  @Test
  void shouldNotFlashOnDefendIntent() {
    Entity enemy = newEnemy();
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("intentChanged", EnemyIntent.defend(4));

    assertNull(animator.getActiveTint());
  }
}
