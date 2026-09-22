package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Renders one short-lived effect visual: it grows, optionally drifts upward, and fades out over its
 * style's duration, centred on its own entity's position.
 *
 * <p>This component never disposes its own entity — whoever spawns the visual entity should check
 * {@link #isExpired()} each frame and dispose the entity once true. Disposing an entity from inside
 * its own component's update() would corrupt the list of components currently being iterated.
 */
public class EffectVisualComponent extends RenderComponent {
  // A high z-index makes sure the visual draws on top of every character sprite.
  private static final float FRONT_Z_INDEX = 1000f;

  private final Texture texture;
  private final EffectVisualStyle style;
  private final float baseSize;
  private float elapsed;

  /**
   * @param texture texture to draw, or null to skip drawing (the lifetime still elapses)
   * @param style the look and timing of this visual
   * @param baseSize size, in world units, that the style's start/end scales are relative to
   */
  public EffectVisualComponent(Texture texture, EffectVisualStyle style, float baseSize) {
    this.texture = texture;
    this.style = style;
    this.baseSize = baseSize;
  }

  /**
   * @return true once this visual has played for its full duration and can be removed
   */
  public boolean isExpired() {
    return elapsed >= style.duration();
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
    if (texture == null) {
      return;
    }

    float t = Math.min(elapsed / style.duration(), 1f);
    // Ease-out: fast growth at first, slowing near the end. Feels punchier than a linear grow.
    float eased = 1f - (1f - t) * (1f - t);

    float size = baseSize * lerp(style.startScale(), style.endScale(), eased);
    float alpha = 1f - t * t;

    Vector2 centre = entity.getPosition();
    float drawY = centre.y + style.rise() * baseSize * eased;

    // SpriteBatch colour is global state — restore it after drawing, or every sprite drawn after
    // this one this frame would inherit the tint.
    float previousColor = batch.getPackedColor();
    batch.setColor(style.color().r, style.color().g, style.color().b, alpha);
    batch.draw(texture, centre.x - size / 2f, drawY - size / 2f, size, size);
    batch.setPackedColor(previousColor);
  }

  private static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
  }
}
