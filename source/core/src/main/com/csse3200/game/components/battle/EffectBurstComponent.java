package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Renders an impact burst: several small particles fly outward from the target and fade out. Good
 * for high-impact effects (e.g. Damage) where a single scaling icon feels too flat.
 *
 * <p>Reuses {@link EffectVisualStyle}'s colour, duration and start/end scale (the latter repurposed
 * here as particle size); particle count and spread are fixed constants for a consistent look.
 */
public class EffectBurstComponent extends RenderComponent {
  private static final int PARTICLE_COUNT = 8;
  private static final float SPREAD_FACTOR = 1.4f;
  private static final float FRONT_Z_INDEX = 1000f;
  private static Texture dotTexture;

  private final EffectVisualStyle style;
  private final float baseSize;
  private final float startDelay;
  private final float[] angles = new float[PARTICLE_COUNT];
  private float elapsed;

  public EffectBurstComponent(EffectVisualStyle style, float baseSize, float startDelay) {
    this.style = style;
    this.baseSize = baseSize;
    this.startDelay = Math.max(0f, startDelay);
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      angles[i] = (360f / PARTICLE_COUNT) * i + MathUtils.random(-15f, 15f);
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
    Texture dot = dotTexture();
    float previousColor = batch.getPackedColor();
    batch.setColor(style.color().r, style.color().g, style.color().b, alpha);
    for (float angle : angles) {
      float x = centre.x + MathUtils.cosDeg(angle) * distance - size / 2f;
      float y = centre.y + MathUtils.sinDeg(angle) * distance - size / 2f;
      batch.draw(dot, x, y, size, size);
    }
    batch.setPackedColor(previousColor);
  }

  /** A single white dot, generated once and shared by every burst, tinted per draw. */
  private static Texture dotTexture() {
    if (dotTexture == null) {
      Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
      pixmap.setColor(Color.WHITE);
      pixmap.fillCircle(4, 4, 4);
      dotTexture = new Texture(pixmap);
      pixmap.dispose();
    }
    return dotTexture;
  }
}
