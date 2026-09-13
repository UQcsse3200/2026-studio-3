package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

@ExtendWith(GameExtension.class)
class AnimationRenderComponentTest {
  @Test
  void shouldAddRemoveAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);

    assertTrue(animator.addAnimation("test_name", 0.1f));
    assertTrue(animator.removeAnimation("test_name"));
    assertFalse(animator.removeAnimation("test_name"));
  }

  @Test
  void shouldFailRemoveInvalidAnimation() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    when(atlas.findRegions("test_name")).thenReturn(null);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);

    assertFalse(animator.addAnimation("test_name", 0.1f));
    assertFalse(animator.removeAnimation("test_name"));
  }

  @Test
  void shouldFailDuplicateAddAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);

    assertTrue(animator.addAnimation("test_name", 0.1f));
    assertFalse(animator.addAnimation("test_name", 0.2f));
  }

  @Test
  void shouldHaveAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);

    animator.addAnimation("test_name", 0.1f);
    assertTrue(animator.hasAnimation("test_name"));
    animator.removeAnimation("test_name");
    assertFalse(animator.hasAnimation("test_name"));
  }

  @Test
  void shouldPlayAnimation() {
    int numFrames = 5;
    String animName = "test_name";
    float frameTime = 1f;

    // Mock texture atlas
    TextureAtlas atlas = createMockAtlas(animName, numFrames);
    Array<AtlasRegion> regions = atlas.findRegions(animName);
    SpriteBatch batch = mock(SpriteBatch.class);

    // Mock game time
    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(frameTime);

    // Start animation
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    Entity entity = new Entity();
    animator.setEntity(entity);
    animator.addAnimation(animName, frameTime);
    animator.startAnimation(animName);

    for (int i = 0; i < 5; i++) {
      // Each draw advances 1 frame, check that it matches for each
      animator.draw(batch);
      verify(batch)
          .draw(
              regions.get(i),
              entity.getPosition().x,
              entity.getPosition().y,
              entity.getScale().x,
              entity.getScale().y);
    }
  }

  @Test
  void shouldFinish() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    SpriteBatch batch = mock(SpriteBatch.class);

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(1f);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    Entity entity = new Entity();
    animator.setEntity(entity);
    animator.addAnimation("test_name", 1f);
    assertFalse(animator.isFinished());

    animator.startAnimation("test_name");
    assertFalse(animator.isFinished());

    animator.draw(batch);
    assertTrue(animator.isFinished());
  }

  @Test
  void shouldStopAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("test_name", 1f);
    assertFalse(animator.stopAnimation());

    animator.startAnimation("test_name");
    assertTrue(animator.stopAnimation());
    assertNull(animator.getCurrentAnimation());
  }

  // flashTint 应该在指定时长后自动清除
  @Test
  void shouldExpireFlashTintAfterDuration() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(Color.WHITE.toFloatBits());

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(0.1f);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.setEntity(new Entity());
    animator.addAnimation("test_name", 1f);
    animator.startAnimation("test_name");

    animator.flashTint(Color.RED, 0.2f);
    assertEquals(Color.RED, animator.getActiveTint());

    animator.draw(batch); // 0.1s 消耗掉，还剩 0.1s
    assertEquals(Color.RED, animator.getActiveTint());

    animator.draw(batch); // 再消耗 0.1s，闪烁结束
    assertNull(animator.getActiveTint());
  }

  // 染色期间画图应该临时切颜色，画完立刻还原，不污染批次的全局颜色状态
  @Test
  void shouldRestoreBatchColorAfterFlash() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    SpriteBatch batch = mock(SpriteBatch.class);
    float originalPacked = Color.WHITE.toFloatBits();
    when(batch.getPackedColor()).thenReturn(originalPacked);

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(0.05f);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.setEntity(new Entity());
    animator.addAnimation("test_name", 1f);
    animator.startAnimation("test_name");
    animator.flashTint(Color.RED, 1f);

    animator.draw(batch);

    InOrder order = inOrder(batch);
    order.verify(batch).setColor(Color.RED);
    order
        .verify(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    order.verify(batch).setPackedColor(originalPacked);
  }

  // 持续染色不会因为多次 draw 而自动消失，要手动 clearTint 才行
  @Test
  void shouldKeepPersistentTintUntilCleared() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(Color.WHITE.toFloatBits());

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(100f); // 故意给一个很大的 deltaTime

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.setEntity(new Entity());
    animator.addAnimation("test_name", 1f);
    animator.startAnimation("test_name");

    animator.setPersistentTint(Color.RED);
    assertTrue(animator.isTintPersistent());

    animator.draw(batch);
    animator.draw(batch);
    assertEquals(Color.RED, animator.getActiveTint());

    animator.clearTint();
    assertNull(animator.getActiveTint());
    assertFalse(animator.isTintPersistent());
  }

  static TextureAtlas createMockAtlas(String animationName, int numRegions) {
    TextureAtlas atlas = mock(TextureAtlas.class);
    Array<AtlasRegion> regions = new Array<>(numRegions);
    for (int i = 0; i < numRegions; i++) {
      regions.add(mock(AtlasRegion.class));
    }
    when(atlas.findRegions(animationName)).thenReturn(regions);
    return atlas;
  }
}
