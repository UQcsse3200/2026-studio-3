package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class EnemyStatusEffectVisualComponentTest {
  private final EffectVisualRegistry registry = new EffectVisualRegistry();
  private GameTime time;
  private Graphics originalGraphics;
  private Texture texture;
  private SpriteBatch batch;
  private Entity target;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
    originalGraphics = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    EnemyStatusEffectVisuals.registerAll(registry);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(512);
    when(texture.getHeight()).thenReturn(512);
    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(42f);
    stats = new CombatStatsComponent(30, 6);
    target = new Entity().addComponent(stats);
    target.setPosition(2f, 4f);
    target.setScale(2f, 3f);
  }

  @AfterEach
  void restoreGraphics() {
    Gdx.graphics = originalGraphics;
  }

  private EnemyStatusEffectVisualComponent visual(EffectType type, float delay) {
    return new EnemyStatusEffectVisualComponent(texture, registry.lookup(type), target, delay);
  }

  @Test
  void playsEverySheetInRowMajorOrderExactlyOnceAfterCoordinatorDelay() {
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      clearInvocations(batch);
      EnemyStatusEffectVisualComponent visual = visual(type, 0.25f);
      visual.render(batch);
      verifyNoInteractions(batch);
      when(time.getDeltaTime()).thenReturn(0.25f);
      visual.update();
      when(time.getDeltaTime()).thenReturn(0.1875f);
      for (int i = 0; i < 4; i++) {
        visual.render(batch);
        assertFalse(visual.isExpired());
        visual.update();
      }
      assertTrue(visual.isExpired());
      visual.render(batch);
      ArgumentCaptor<TextureRegion> frames = ArgumentCaptor.forClass(TextureRegion.class);
      verify(batch, times(4))
          .draw(frames.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      for (int i = 0; i < 4; i++) {
        TextureRegion frame = frames.getAllValues().get(i);
        assertEquals((i % 2) * 256, frame.getRegionX());
        assertEquals((i / 2) * 256, frame.getRegionY());
        assertEquals(256, frame.getRegionWidth());
        assertEquals(256, frame.getRegionHeight());
      }
      verify(batch, times(4)).setPackedColor(42f);
    }
    verify(texture, times(3))
        .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
  }

  @Test
  void followsOnlyItsTargetAndFitsBetweenHealthAndIntent() {
    EnemyStatusEffectVisualComponent visual = visual(EffectType.POISON, 0f);
    visual.render(batch);
    target.setPosition(5f, 6f);
    visual.render(batch);
    ArgumentCaptor<Float> xs = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> ys = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> sizes = ArgumentCaptor.forClass(Float.class);
    verify(batch, times(2))
        .draw(any(TextureRegion.class), xs.capture(), ys.capture(), sizes.capture(), anyFloat());
    assertEquals(3f, xs.getAllValues().get(1) - xs.getAllValues().get(0), 0.0001f);
    assertEquals(2f, ys.getAllValues().get(1) - ys.getAllValues().get(0), 0.0001f);
    assertTrue(ys.getValue() > target.getPosition().y);
    assertTrue(ys.getValue() + sizes.getValue() < target.getPosition().y + target.getScale().y);
  }

  @Test
  void deathBeforeOrDuringPlaybackStopsDrawingIncludingQueuedVisuals() {
    for (float delay : new float[] {0f, 1f}) {
      stats.setHealth(30);
      EnemyStatusEffectVisualComponent visual = visual(EffectType.FEEBLE, delay);
      stats.setHealth(0);
      assertTrue(visual.isExpired());
      visual.render(batch);
    }
    verifyNoInteractions(batch);
  }

  @Test
  void disposalStopsDrawingWithoutDisposingSharedTextureOrChangingStatus() {
    stats.applyStatusEffect("POISON", 3, 3);
    EnemyStatusEffectVisualComponent visual = visual(EffectType.POISON, 0f);
    new Entity().addComponent(visual).create();
    when(time.getDeltaTime()).thenReturn(1f);
    visual.update();
    assertTrue(visual.isExpired());
    visual.dispose();
    visual.render(batch);
    verifyNoInteractions(batch);
    verify(texture, never()).dispose();
    verify(ServiceLocator.getRenderService()).unregister(visual);
    assertEquals(30, stats.getHealth());
    assertEquals(3, stats.getStatusEffect("POISON").getValue());
    assertEquals(3, stats.getStatusEffect("POISON").getDuration());
  }

  @Test
  void missingTextureExpiresAndUnequalCellsAreRejected() {
    assertTrue(
        new EnemyStatusEffectVisualComponent(null, registry.lookup(EffectType.FEEBLE), target, 0f)
            .isExpired());
    when(texture.getWidth()).thenReturn(511);
    assertThrows(IllegalArgumentException.class, () -> visual(EffectType.FEEBLE, 0f));
  }

  @Test
  void restoresBatchColourEvenWhenDrawingFails() {
    doThrow(new IllegalStateException("draw failed"))
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    assertThrows(
        IllegalStateException.class, () -> visual(EffectType.VULNERABLE, 0f).render(batch));
    verify(batch).setPackedColor(42f);
  }

  //  @Test
  //  void realTallAndCrawlerAssetsUseCenteredSinglesAndUniformForegroundOrbits() {
  //    String[] enemies = {"tomb_guardian", "bone_crawler"};
  //    int[][][] pixels = {
  //      {{136, 34, 369, 477}, {136, 28, 369, 471}},
  //      {{46, 312, 465, 477}, {46, 309, 465, 477}}
  //    };
  //    EffectType[] types = {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE};
  //    for (int enemy = 0; enemy < enemies.length; enemy++) {
  //      TextureAtlas atlas =
  //          new TextureAtlas(Gdx.files.internal("images/enemies/" + enemies[enemy] + ".atlas"));
  //      RenderService service = new RenderService();
  //      ServiceLocator.registerRenderService(service);
  //      try {
  //        AnimationRenderComponent renderer = spy(new AnimationRenderComponent(atlas));
  //        stats = new CombatStatsComponent(30, 6);
  //        target = new Entity().addComponent(stats).addComponent(renderer);
  //        renderer.addAnimation("idle", 0.1f, Animation.PlayMode.LOOP);
  //        renderer.startAnimation("idle");
  //        EnemyStatusEffectVisualComponent[] visuals = new EnemyStatusEffectVisualComponent[3];
  //        for (int i : new int[] {2, 0, 1}) {
  //          stats.applyStatusEffect(types[i].name(), 1, 3);
  //          visuals[i] =
  //              spy(
  //                  new EnemyStatusEffectVisualComponent(
  //                      texture, registry.lookup(types[i]), target, 0f, types[i]));
  //          visuals[i].showPersistent();
  //          service.register(visuals[i]);
  //        }
  //        service.register(renderer);
  //        for (int frame = 0; frame < 2; frame++) {
  //          when(time.getDeltaTime()).thenReturn(0.3f);
  //          for (EnemyStatusEffectVisualComponent visual : visuals) visual.update();
  //          int[] box = pixels[enemy][frame];
  //          for (float[] transform : new float[][] {{2f, 4f, 2f, 3f}, {7f, -2f, 4f, 1.5f}}) {
  //            target.setPosition(transform[0], transform[1]);
  //            target.setScale(transform[2], transform[3]);
  //            when(time.getDeltaTime()).thenReturn(0f);
  //            clearInvocations(renderer, visuals[0], visuals[1], visuals[2]);
  //            service.render(batch);
  //            for (EnemyStatusEffectVisualComponent visual : visuals) {
  //              var order = inOrder(renderer, visual);
  //              order.verify(renderer).render(batch);
  //              order.verify(visual).render(batch);
  //              assertEquals(renderer.getZIndex() + 0.02f, visual.getZIndex(), 0.0001f);
  //            }
  //            float bottom = transform[1] + (511 - box[3]) / 512f * transform[3];
  //            float height = (box[3] - box[1] + 1) / 512f * transform[3];
  //            float left = transform[0] + box[0] / 512f * transform[2];
  //            float width = (box[2] - box[0] + 1) / 512f * transform[2];
  //            for (int mask : new int[] {7, 5, 4, 6, 2, 3, 1}) {
  //              for (int i = 0; i < 3; i++) {
  //                stats.removeStatusEffect(types[i].name());
  //                if ((mask & (1 << i)) != 0) stats.applyStatusEffect(types[i].name(), 1, 3);
  //              }
  //              int slot = 0;
  //              int count = Integer.bitCount(mask);
  //              for (int i = 0; i < 3; i++) {
  //                if ((mask & (1 << i)) == 0) continue;
  //                float[] effect = renderedBounds(visuals[i]);
  //                double angle = 2d * Math.PI * slot++ / count;
  //                float expectedWidth = width * (count == 1 ? 1.4f : 0.55f);
  //                float expectedHeight = count == 1 ? height * 1.4f : expectedWidth;
  //                assertEquals(expectedWidth, effect[2], 0.0001f);
  //                assertEquals(expectedHeight, effect[3], 0.0001f);
  //                assertEquals(
  //                    left + width / 2f + (count == 1 ? 0f : width * 0.65f * Math.cos(angle)),
  //                    effect[0] + effect[2] / 2f,
  //                    0.0001f);
  //                assertEquals(
  //                    bottom + height / 2f + (count == 1 ? 0f : height * 0.2f * Math.sin(angle)),
  //                    effect[1] + effect[3] / 2f,
  //                    0.0001f);
  //                assertEquals(0.9f, firstAlpha(), 0.0001f);
  //              }
  //            }
  //            for (EffectType type : types) {
  //              stats.removeStatusEffect(type.name());
  //              stats.applyStatusEffect(type.name(), 1, 3);
  //            }
  //          }
  //          when(time.getDeltaTime()).thenReturn(0.1f);
  //          renderer.render(mock(SpriteBatch.class));
  //        }
  //        for (EnemyStatusEffectVisualComponent visual : visuals) visual.dispose();
  //        assertEquals(30, stats.getHealth());
  //        assertEquals(3, stats.getStatusEffect("POISON").getDuration());
  //      } finally {
  //        service.dispose();
  //        atlas.dispose();
  //      }
  //    }
  //  }

  @Test
  void orbitUsesSharedFrameTimeAndRedistributesWithoutResetOnReplay() {
    EffectType[] types = {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE};
    EnemyStatusEffectVisualComponent[] visuals = new EnemyStatusEffectVisualComponent[3];
    for (int i = 2; i >= 0; i--) {
      stats.applyStatusEffect(types[i].name(), 1, 3);
      visuals[i] =
          new EnemyStatusEffectVisualComponent(
              texture, registry.lookup(types[i]), target, 0f, types[i]);
      when(time.getDeltaTime()).thenReturn(i * 0.17f);
      visuals[i].update();
      visuals[i].showPersistent();
    }
    for (int step = 0; step <= 4; step++) {
      when(Gdx.graphics.getFrameId()).thenReturn((long) step);
      when(time.getDeltaTime()).thenReturn(0.9f);
      for (int mask : new int[] {7, 5, 4, 6, 2, 3, 1}) {
        for (int i = 0; i < 3; i++) {
          stats.removeStatusEffect(types[i].name());
          if ((mask & (1 << i)) != 0) stats.applyStatusEffect(types[i].name(), 1, 3);
        }
        int slot = 0;
        int count = Integer.bitCount(mask);
        for (int i = 0; i < 3; i++) {
          if ((mask & (1 << i)) == 0) continue;
          double angle = Math.PI * 2d * (step / 4d + slot++ / (double) count);
          float[] effect = renderedBounds(visuals[i]);
          assertEquals(count == 1 ? 2.8f : 1.1f, effect[2], 0.0001f);
          assertEquals(count == 1 ? 4.2f : 1.1f, effect[3], 0.0001f);
          assertEquals(
              3f + (count == 1 ? 0f : 1.3f * Math.cos(angle)), effect[0] + effect[2] / 2f, 0.0001f);
          assertEquals(
              5.5f + (count == 1 ? 0f : 0.6f * Math.sin(angle)),
              effect[1] + effect[3] / 2f,
              0.0001f);
          visuals[i].replay(0.3f);
          assertArrayEquals(effect, renderedBounds(visuals[i]), 0.0001f);
          visuals[i].showPersistent();
          assertArrayEquals(effect, renderedBounds(visuals[i]), 0.0001f);
        }
      }
    }
    stats.applyStatusEffect("FEEBLE", 1, 3);
    float[] paused = renderedBounds(visuals[0]);
    when(time.getDeltaTime()).thenReturn(0f);
    when(Gdx.graphics.getFrameId()).thenReturn(5L);
    assertArrayEquals(paused, renderedBounds(visuals[0]), 0.0001f);
    assertEquals(30, stats.getHealth());
  }

  @Test
  void singleAndCombinedStatusesLoopAllFourFramesWithoutChangingLayoutAlphaOrCombatState() {
    EffectType[] types = {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE};
    for (int mask : new int[] {1, 2, 4, 7}) {
      EnemyStatusEffectVisualComponent[] visuals = new EnemyStatusEffectVisualComponent[3];
      float[][] initial = new float[3][];
      int[] previousFrame = new int[3];
      int[] transitions = new int[3];
      for (int i = 0; i < 3; i++) {
        stats.removeStatusEffect(types[i].name());
        if ((mask & (1 << i)) != 0) stats.applyStatusEffect(types[i].name(), 1, 3);
      }
      for (int i = 0; i < 3; i++) {
        if ((mask & (1 << i)) == 0) continue;
        visuals[i] =
            new EnemyStatusEffectVisualComponent(
                texture, registry.lookup(types[i]), target, 0f, types[i]);
        visuals[i].showPersistent();
        initial[i] = renderedBounds(visuals[i]);
        assertEquals(0.9f, firstAlpha(), 0.0001f);
      }
      when(time.getDeltaTime()).thenReturn(0.1f);
      for (int step = 0; step < 73; step++) {
        for (int i = 0; i < 3; i++) {
          if (visuals[i] == null) continue;
          visuals[i].update();
          assertArrayEquals(initial[i], renderedBounds(visuals[i]), 0.0001f);
          assertEquals(0.9f, firstAlpha(), 0.0001f);
          ArgumentCaptor<TextureRegion> region = ArgumentCaptor.forClass(TextureRegion.class);
          verify(batch).draw(region.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
          int frame =
              region.getValue().getRegionY() / 256 * 2 + region.getValue().getRegionX() / 256;
          if (frame != previousFrame[i]) {
            assertEquals((previousFrame[i] + 1) % 4, frame);
            transitions[i]++;
            previousFrame[i] = frame;
          }
        }
      }
      for (int i = 0; i < 3; i++) {
        if (visuals[i] == null) continue;
        assertTrue(transitions[i] >= 8, types[i] + " must complete at least two four-frame loops");
        assertEquals(1, stats.getStatusEffect(types[i].name()).getValue());
        assertEquals(3, stats.getStatusEffect(types[i].name()).getDuration());
      }
    }
    assertEquals(30, stats.getHealth());
  }

  private float firstAlpha() {
    ArgumentCaptor<Float> alphas = ArgumentCaptor.forClass(Float.class);
    verify(batch, atLeastOnce()).setColor(anyFloat(), anyFloat(), anyFloat(), alphas.capture());
    return alphas.getAllValues().get(0);
  }

  @Test
  void applicationLastFrameFadesSmoothlyOverExtendedDuration() {
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      assertEquals(0.75f, registry.lookup(type).duration());
      EnemyStatusEffectVisualComponent visual = visual(type, 0f);
      when(time.getDeltaTime()).thenReturn(0.5625f);
      visual.update();
      float[] expectedAlpha = {1f, 0.84375f, 0.5f, 0.15625f};
      for (float fraction : expectedAlpha) {
        clearInvocations(batch);
        visual.render(batch);
        assertEquals(registry.lookup(type).color().a * fraction, firstAlpha(), 0.0001f);
        assertFalse(visual.isExpired());
        when(time.getDeltaTime()).thenReturn(0.046875f);
        visual.update();
      }
      assertTrue(visual.isExpired());
      clearInvocations(batch);
      visual.render(batch);
      verifyNoInteractions(batch);
    }
  }

  @Test
  void largeApplicationPlaysFourFramesThenReplaysOnTheSamePersistentComponent() {
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      stats.applyStatusEffect(type.name(), 1, 3);
      EnemyStatusEffectVisualComponent visual =
          new EnemyStatusEffectVisualComponent(texture, registry.lookup(type), target, 0f, type);
      when(time.getDeltaTime()).thenReturn(0.1875f);
      for (int replay = 0; replay < 2; replay++) {
        visual.replay(0f);
        for (int frame = 0; frame < 4; frame++) {
          float[] bounds = renderedBounds(visual);
          ArgumentCaptor<TextureRegion> region = ArgumentCaptor.forClass(TextureRegion.class);
          verify(batch).draw(region.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
          assertEquals(frame % 2 * 256, region.getValue().getRegionX());
          assertEquals(frame / 2 * 256, region.getValue().getRegionY());
          assertEquals(3f, bounds[0] + bounds[2] / 2f, 0.0001f);
          assertEquals(5.5f, bounds[1] + bounds[2] / 2f, 0.0001f);
          assertTrue(bounds[2] >= 2f);
          visual.update();
        }
        float[] hint = renderedBounds(visual);
        assertTrue(hint[2] > 0f && hint[3] > 0f);
        assertTrue(visual.getZIndex() > -target.getPosition().y);
        assertTrue(visual.represents(target, type));
        assertFalse(visual.isExpired());
      }
      assertEquals(3, stats.getStatusEffect(type.name()).getDuration());
      assertEquals(30, stats.getHealth());
      stats.removeStatusEffect(type.name());
      clearInvocations(batch);
      visual.render(batch);
      verifyNoInteractions(batch);
    }
  }

  @Test
  void applicationAndQueuedReplayRemainInFront() {
    stats.applyStatusEffect("POISON", 1, 2);
    stats.applyStatusEffect("FEEBLE", 1, 2);
    EnemyStatusEffectVisualComponent poison =
        new EnemyStatusEffectVisualComponent(
            texture, registry.lookup(EffectType.POISON), target, 0f, EffectType.POISON);
    EnemyStatusEffectVisualComponent feeble =
        new EnemyStatusEffectVisualComponent(
            texture, registry.lookup(EffectType.FEEBLE), target, 0f, EffectType.FEEBLE);
    float[] left = renderedBounds(poison);
    float[] right = renderedBounds(feeble);
    assertEquals(3f, left[0] + left[2] / 2f, 0.0001f);
    assertEquals(5.5f, left[1] + left[2] / 2f, 0.0001f);
    assertEquals(3f, right[0] + right[2] / 2f, 0.0001f);
    assertEquals(5.5f, right[1] + right[2] / 2f, 0.0001f);
    poison.showPersistent();
    poison.replay(0.3f);
    stats.removeStatusEffect("FEEBLE");
    float[] centered = renderedBounds(poison);
    assertEquals(3f, centered[0] + centered[2] / 2f, 0.0001f);
    assertTrue(poison.getZIndex() > -target.getPosition().y);
    assertFalse(poison.isExpired());
  }

  private float[] renderedBounds(EnemyStatusEffectVisualComponent visual) {
    clearInvocations(batch);
    visual.render(batch);
    ArgumentCaptor<Float> xs = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> ys = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> sizes = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> heights = ArgumentCaptor.forClass(Float.class);
    verify(batch, atLeastOnce())
        .draw(
            any(TextureRegion.class),
            xs.capture(),
            ys.capture(),
            sizes.capture(),
            heights.capture());
    return new float[] {
      xs.getAllValues().getFirst(),
      ys.getAllValues().getFirst(),
      sizes.getAllValues().getFirst(),
      heights.getAllValues().getFirst()
    };
  }

  @Test
  void clearedOrDeadStatusesStopDrawingEvenBeforeCoordinatorCleanup() {
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      stats.setHealth(30);
      stats.applyStatusEffect(type.name(), 1, 2);
      EnemyStatusEffectVisualComponent visual =
          new EnemyStatusEffectVisualComponent(texture, registry.lookup(type), target, 0f, type);
      visual.showPersistent();
      stats.removeStatusEffect(type.name());
      assertTrue(visual.isExpired());
      visual.render(batch);
      stats.applyStatusEffect(type.name(), 1, 2);
      stats.setHealth(0);
      assertTrue(visual.isExpired());
      visual.render(batch);
    }
    verifyNoInteractions(batch);
  }

  @Test
  void statusRegistrationLeavesOtherTeamsStylesAloneAndUsesTransparentFourCellAssets()
      throws Exception {
    EffectVisualStyle damage = registry.lookup(EffectType.DAMAGE);
    EnemyStatusEffectVisuals.registerAll(registry);
    assertSame(damage, registry.lookup(EffectType.DAMAGE));
    assertFalse(EnemyStatusEffectVisuals.supports(EffectType.DAMAGE));
    assertFalse(EnemyStatusEffectVisuals.supports(null));
    for (String path : EnemyStatusEffectVisuals.texturePaths()) {
      BufferedImage image = ImageIO.read(new File(path));
      assertTrue(image.getColorModel().hasAlpha());
      assertEquals(512, image.getWidth());
      assertEquals(512, image.getHeight());
      for (int i = 0; i < 4; i++) {
        int visible = 0;
        for (int y = 0; y < 256; y++) {
          for (int x = 0; x < 256; x++) {
            int alpha = image.getRGB(i % 2 * 256 + x, i / 2 * 256 + y) >>> 24;
            if (alpha > 0) visible++;
            if (x == 0 || y == 0 || x == 255 || y == 255) assertEquals(0, alpha);
          }
        }
        assertTrue(visible > 100 && visible < 256 * 256 * 0.8, path + " frame " + i);
      }
    }
    assertTrue(registry.lookup(EffectType.POISON).rise() > 0);
    assertEquals(0f, registry.lookup(EffectType.VULNERABLE).rise());
    assertTrue(registry.lookup(EffectType.FEEBLE).rise() < 0);
  }
}
