package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
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
class EnemyAnimationControllerTest {

  @BeforeEach
  void registerServices() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
  }

  // 造一个只带指定动画区域的假图集，用来控制 hasAnimation() 的结果
  private Entity newEnemyWithAnimations(String... availableRegionNames) {
    TextureAtlas atlas = mock(TextureAtlas.class);
    for (String name : availableRegionNames) {
      Array<AtlasRegion> regions = new Array<>();
      regions.add(mock(AtlasRegion.class));
      when(atlas.findRegions(name)).thenReturn(regions);
    }

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("idle", 0.5f);
    animator.addAnimation("hurt", 0.15f);
    animator.addAnimation("death", 0.3f);

    Entity entity = new Entity();
    entity.addComponent(animator);
    entity.addComponent(new EnemyAnimationController());
    entity.create();
    return entity;
  }

  // 有 death 动画时，死亡应该播 death，而不是旧的占位逻辑（播 hurt）
  @Test
  void shouldPlayDeathWhenDeathAnimationAvailable() {
    Entity enemy = newEnemyWithAnimations("idle", "hurt", "death");
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("enemyDefeated");

    assertEquals("death", animator.getCurrentAnimation());
  }

  // 图集里还没有 death 帧的旧敌人，死亡时应该退回 hurt，而不是报错
  @Test
  void shouldFallBackToHurtWhenNoDeathAnimation() {
    Entity enemy = newEnemyWithAnimations("idle", "hurt");
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("enemyDefeated");

    assertEquals("hurt", animator.getCurrentAnimation());
  }

  @Test
  void shouldPlayHurtWhenDamaged() {
    Entity enemy = newEnemyWithAnimations("idle", "hurt", "death");
    AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);

    enemy.getEvents().trigger("enemyDamaged", 5);

    assertEquals("hurt", animator.getCurrentAnimation());
  }
}
