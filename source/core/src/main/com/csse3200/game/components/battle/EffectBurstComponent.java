package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders an impact burst: several small particles fly outward from the target and fade out.
 *
 * <p>Two independent choices, set per registration: {@code hasGravity} (an even outward scatter vs
 * one that curves downward, reading as falling debris) and {@code shape} (a round dot vs a jagged
 * triangular shard, with the shard also spinning as it flies). Existing registrations are
 * unaffected by new combinations — e.g. Damage keeps using {@code DOT} with no gravity.
 *
 * <p>Reuses {@link EffectVisualStyle}'s colour, duration and start/end scale (the latter repurposed
 * here as particle size); particle count and spread are fixed constants for a consistent look.
 */
public class EffectBurstComponent extends RenderComponent {
  /** Particle appearance: a round dot, or a jagged triangular shard that spins as it flies. */
  public enum ParticleShape {
    DOT,
    SHARD
  }

  private static final int PARTICLE_COUNT = 8;
  private static final float SPREAD_FACTOR = 1.4f;
  private static final float GRAVITY_DROP_FACTOR = 1.1f;
  private static final float FRONT_Z_INDEX = 1000f;
  private static final Map<ParticleShape, Texture> textures = new EnumMap<>(ParticleShape.class);

  private final EffectVisualStyle style;
  private final float baseSize;
  private final float startDelay;
  private final boolean hasGravity;
  private final ParticleShape shape;
  private final float[] angles = new float[PARTICLE_COUNT];
  private final float[] spinDeg = new float[PARTICLE_COUNT];
  private float elapsed;

  /**
   * @param style colour, duration and particle size
   * @param baseSize size, in world units, the burst's spread and particle size are relative to
   * @param startDelay seconds to wait before this visual starts (set by the coordinator)
   * @param hasGravity false for an even outward scatter; true for particles that curve downward as
   *     they fly out, reading as falling debris
   * @param shape DOT for a round particle (e.g. Damage), SHARD for a spinning jagged fragment (e.g.
   *     Sunder's broken armour)
   */
  public EffectBurstComponent(
      EffectVisualStyle style,
      float baseSize,
      float startDelay,
      boolean hasGravity,
      ParticleShape shape) {
    this.style = style;
    this.baseSize = baseSize;
    this.startDelay = Math.max(0f, startDelay);
    this.hasGravity = hasGravity;
    this.shape = shape;
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      angles[i] = (360f / PARTICLE_COUNT) * i + MathUtils.random(-15f, 15f);
      spinDeg[i] = MathUtils.random(-180f, 180f);
    }
  }

  public boolean isExpired() {
    return elapsed >= startDelay + style.duration();
  }

  @Override
  public void update() {
    elapsed += ServiceLocator.getTimeSource().getDeltaTime();
  }

  @Override
  public float getZIndex() {
    return FRONT_Z_INDEX;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (elapsed < startDelay) {
      return;
    }
    float t = Math.min((elapsed - startDelay) / style.duration(), 1f);
    float distance = baseSize * SPREAD_FACTOR * t;
    float size = baseSize * MathUtils.lerp(style.startScale(), style.endScale(), t) * 0.3f;
    float alpha = 1f - t * t;

    Vector2 centre = entity.getPosition();
    TextureRegion particle = new TextureRegion(textureFor(shape));
    float previousColor = batch.getPackedColor();
    batch.setColor(style.color().r, style.color().g, style.color().b, alpha);
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      float angle = angles[i];
      float x = centre.x + MathUtils.cosDeg(angle) * distance - size / 2f;
      float y = centre.y + MathUtils.sinDeg(angle) * distance - size / 2f;
      if (hasGravity) {
        // Accelerating downward drop, on top of the outward scatter, so particles arc down like
        // falling debris instead of flying out evenly in every direction.
        y -= baseSize * GRAVITY_DROP_FACTOR * t * t;
      }
      float rotation = shape == ParticleShape.SHARD ? spinDeg[i] * t : 0f;
      batch.draw(particle, x, y, size / 2f, size / 2f, size, size, 1f, 1f, rotation);
    }
    batch.setPackedColor(previousColor);
  }

  /** Round dot or jagged shard, generated once per shape and shared across every burst. */
  private static Texture textureFor(ParticleShape shape) {
    return textures.computeIfAbsent(
        shape, s -> s == ParticleShape.SHARD ? buildShardTexture() : buildDotTexture());
  }

  private static Texture buildDotTexture() {
    Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fillCircle(4, 4, 4);
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }

  private static Texture buildShardTexture() {
    int w = 16;
    int h = 16;
    Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    // An irregular triangle, off-centre, so it reads as a broken fragment rather than a neat shape.
    pixmap.fillTriangle(1, h - 2, w - 2, h - 5, 6, 1);
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }
}
