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

/**
 * Renders a slim blade shape that slashes from an offset starting point, through the target, to an
 * offset end point, fading out as it goes. Good for piercing/penetrating effects where the motion
 * itself — a blade cutting straight through — should read clearly, without a separate impact flash.
 *
 * <p>Reuses {@link EffectVisualStyle}'s colour and duration; startScale is repurposed as the
 * blade's length multiplier. Direction and reach are constructor parameters, not hardcoded, so
 * whoever registers an effect using this renderer can tune them without editing this file.
 */
public class EffectProjectileComponent extends RenderComponent {
  private static final float FRONT_Z_INDEX = 1000f;
  private static Texture bladeTexture;

  private final EffectVisualStyle style;
  private final float baseSize;
  private final float startDelay;
  private final float travelAngleDeg;
  private final float reach;

  private float elapsed;

  /**
   * @param style colour and timing; startScale is the blade's length multiplier
   * @param baseSize size, in world units, the slash's reach and length are relative to
   * @param startDelay seconds to wait before this visual starts (set by the coordinator)
   * @param travelAngleDeg direction of travel in degrees (0 = +x axis, counter-clockwise); e.g. 225
   *     for top-right to bottom-left
   * @param reach how far past the target, on each side, the slash's start/end points sit, in
   *     multiples of baseSize
   */
  public EffectProjectileComponent(
      EffectVisualStyle style,
      float baseSize,
      float startDelay,
      float travelAngleDeg,
      float reach) {
    this.style = style;
    this.baseSize = baseSize;
    this.startDelay = Math.max(0f, startDelay);
    this.travelAngleDeg = travelAngleDeg;
    this.reach = reach;
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
    Vector2 target = entity.getPosition();

    Vector2 start =
        new Vector2(
            target.x - MathUtils.cosDeg(travelAngleDeg) * baseSize * reach,
            target.y - MathUtils.sinDeg(travelAngleDeg) * baseSize * reach);
    Vector2 end =
        new Vector2(
            target.x + MathUtils.cosDeg(travelAngleDeg) * baseSize * reach,
            target.y + MathUtils.sinDeg(travelAngleDeg) * baseSize * reach);

    Vector2 position = start.cpy().lerp(end, t);
    float length = baseSize * style.startScale();
    float width = length * 0.16f;
    float alpha = 1f - t * t;

    TextureRegion blade = new TextureRegion(bladeTexture());
    float previousColor = batch.getPackedColor();
    batch.setColor(style.color().r, style.color().g, style.color().b, alpha);
    batch.draw(
        blade,
        position.x - length / 2f,
        position.y - width / 2f,
        length / 2f,
        width / 2f,
        length,
        width,
        1f,
        1f,
        travelAngleDeg);
    batch.setPackedColor(previousColor);
  }

  /** A slim blade shape, pointing along +x, generated once and tinted per draw. */
  private static Texture bladeTexture() {
    if (bladeTexture == null) {
      int w = 64;
      int h = 12;
      Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
      pixmap.setColor(Color.WHITE);
      pixmap.fillTriangle(w - 1, h / 2, (int) (w * 0.6f), 0, (int) (w * 0.6f), h - 1);
      pixmap.fillTriangle(0, h / 2, (int) (w * 0.6f), 0, (int) (w * 0.6f), h - 1);
      bladeTexture = new Texture(pixmap);
      pixmap.dispose();
    }
    return bladeTexture;
  }
}
