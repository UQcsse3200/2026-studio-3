package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class EnemyStatusEffectVisualComponentTest {
  private final EffectVisualRegistry registry = new EffectVisualRegistry();
  private GameTime time;
  private Texture texture;
  private SpriteBatch batch;
  private Entity target;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
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
      when(time.getDeltaTime()).thenReturn(0.125f);
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

  @Test
  void vulnerablePulsesAndFeebleDescendsOverTheBodyInsteadOfBecomingCornerIcons() {
    for (EffectType type : new EffectType[] {EffectType.VULNERABLE, EffectType.FEEBLE}) {
      clearInvocations(batch);
      target.setPosition(2f, 4f);
      stats.applyStatusEffect(type.name(), 1, 2);
      EnemyStatusEffectVisualComponent visual =
          new EnemyStatusEffectVisualComponent(texture, registry.lookup(type), target, 0f, type);
      visual.showPersistent();
      visual.render(batch);
      when(time.getDeltaTime()).thenReturn(0.3f);
      visual.update();
      visual.render(batch);
      when(time.getDeltaTime()).thenReturn(0.5f);
      visual.update();
      visual.render(batch);
      target.setPosition(5f, 6f);
      visual.render(batch);

      ArgumentCaptor<TextureRegion> frames = ArgumentCaptor.forClass(TextureRegion.class);
      ArgumentCaptor<Float> xs = ArgumentCaptor.forClass(Float.class);
      ArgumentCaptor<Float> ys = ArgumentCaptor.forClass(Float.class);
      ArgumentCaptor<Float> sizes = ArgumentCaptor.forClass(Float.class);
      ArgumentCaptor<Float> alphas = ArgumentCaptor.forClass(Float.class);
      verify(batch, times(7))
          .draw(frames.capture(), xs.capture(), ys.capture(), sizes.capture(), anyFloat());
      verify(batch, times(7)).setColor(anyFloat(), anyFloat(), anyFloat(), alphas.capture());
      assertEquals(0, frames.getAllValues().get(1).getRegionX());
      assertEquals(256, frames.getAllValues().get(1).getRegionY());
      assertEquals(256, frames.getAllValues().get(2).getRegionX());
      assertEquals(0, frames.getAllValues().get(2).getRegionY());
      if (type == EffectType.VULNERABLE) {
        assertNotEquals(sizes.getAllValues().get(1), sizes.getAllValues().get(3));
      } else {
        assertTrue(ys.getAllValues().get(3) < ys.getAllValues().get(1));
      }
      assertNotEquals(alphas.getAllValues().get(1), alphas.getAllValues().get(3));
      for (int i = 0; i < 5; i++) {
        assertTrue(xs.getAllValues().get(i) >= 2f);
        assertTrue(xs.getAllValues().get(i) + sizes.getAllValues().get(i) <= 4f);
        assertTrue(ys.getAllValues().get(i) >= 4f);
        assertTrue(ys.getAllValues().get(i) + sizes.getAllValues().get(i) <= 7f);
      }
      assertEquals(3f, xs.getAllValues().get(5) - xs.getAllValues().get(3), 0.0001f);
      assertEquals(2f, ys.getAllValues().get(5) - ys.getAllValues().get(3), 0.0001f);
      assertFalse(visual.isExpired());
      assertEquals(30, stats.getHealth());
      assertEquals(2, stats.getStatusEffect(type.name()).getDuration());
    }
  }

  @Test
  void persistentPoisonKeepsMovingAndBlendingOverTheBodyWithoutTickingStatus() {
    stats.applyStatusEffect("POISON", 3, 2);
    EnemyStatusEffectVisualComponent visual =
        new EnemyStatusEffectVisualComponent(
            texture, registry.lookup(EffectType.POISON), target, 0f, EffectType.POISON);
    visual.showPersistent();
    visual.render(batch);
    when(time.getDeltaTime()).thenReturn(0.6f);
    visual.update();
    visual.render(batch);
    when(time.getDeltaTime()).thenReturn(1.2f);
    visual.update();
    visual.render(batch);
    target.setPosition(5f, 6f);
    visual.render(batch);

    ArgumentCaptor<TextureRegion> frames = ArgumentCaptor.forClass(TextureRegion.class);
    ArgumentCaptor<Float> xs = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> ys = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> sizes = ArgumentCaptor.forClass(Float.class);
    verify(batch, times(7))
        .draw(frames.capture(), xs.capture(), ys.capture(), sizes.capture(), anyFloat());
    assertEquals(0, frames.getAllValues().get(1).getRegionX());
    assertEquals(256, frames.getAllValues().get(1).getRegionY());
    assertEquals(256, frames.getAllValues().get(2).getRegionX());
    assertEquals(0, frames.getAllValues().get(2).getRegionY());
    assertNotEquals(xs.getAllValues().get(1), xs.getAllValues().get(3));
    assertNotEquals(ys.getAllValues().get(1), ys.getAllValues().get(3));
    assertNotEquals(sizes.getAllValues().get(1), sizes.getAllValues().get(3));
    for (int i = 0; i < 5; i++) {
      assertTrue(xs.getAllValues().get(i) >= 2f);
      assertTrue(xs.getAllValues().get(i) + sizes.getAllValues().get(i) <= 4f);
      assertTrue(ys.getAllValues().get(i) >= 4f);
      assertTrue(ys.getAllValues().get(i) + sizes.getAllValues().get(i) <= 7f);
    }
    assertEquals(3f, xs.getAllValues().get(5) - xs.getAllValues().get(3), 0.0001f);
    assertEquals(2f, ys.getAllValues().get(5) - ys.getAllValues().get(3), 0.0001f);
    assertFalse(visual.isExpired());
    assertEquals(30, stats.getHealth());
    assertEquals(3, stats.getStatusEffect("POISON").getValue());
    assertEquals(2, stats.getStatusEffect("POISON").getDuration());
    stats.removeStatusEffect("POISON");
    clearInvocations(batch);
    visual.render(batch);
    assertTrue(visual.isExpired());
    verifyNoInteractions(batch);
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
