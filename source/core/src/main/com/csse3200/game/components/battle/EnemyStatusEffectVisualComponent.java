package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/**
 * Plays an enemy status sheet once, in row-major order, following the target while it moves.
 *
 * <p>The shared coordinator supplies the delay and owns entity disposal via {@link #isExpired()}.
 * This component neither listens for card/status events nor changes combat state. Textures belong
 * to the resource service. Do not also attach a single-image EffectVisualComponent to this entity.
 */
public class EnemyStatusEffectVisualComponent extends RenderComponent {
  private final Entity target;
  private final EffectVisualStyle style;
  private final TextureRegion[] frames;
  private final float startDelay;
  private float elapsed;
  private boolean disposed;

  /**
   * @param texture a 2-by-2 sheet, or null when the resource is unavailable
   * @param style registered status style; duration is the total duration of all four frames
   * @param target the actual enemy affected by the card
   * @param startDelay delay supplied by the shared coordinator, never computed by this component
   */
  public EnemyStatusEffectVisualComponent(
      Texture texture, EffectVisualStyle style, Entity target, float startDelay) {
    this.style = Objects.requireNonNull(style);
    this.target = Objects.requireNonNull(target);
    this.startDelay = Math.max(0f, startDelay);
    if (texture == null) {
      frames = new TextureRegion[0];
    } else {
      if (texture.getWidth() < 2
          || texture.getHeight() < 2
          || texture.getWidth() % 2 != 0
          || texture.getHeight() % 2 != 0) {
        throw new IllegalArgumentException("Status texture must have four equal cells (2 by 2)");
      }
      texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
      TextureRegion[][] cells =
          TextureRegion.split(texture, texture.getWidth() / 2, texture.getHeight() / 2);
      frames = new TextureRegion[] {cells[0][0], cells[0][1], cells[1][0], cells[1][1]};
    }
  }

  @Override
  public void update() {
    elapsed += ServiceLocator.getTimeSource().getDeltaTime();
  }

  /** The owner should remove expired entities outside this component's update iteration. */
  public boolean isExpired() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    return disposed
        || frames.length == 0
        || (stats != null && stats.isDead())
        || elapsed >= startDelay + style.duration();
  }

  @Override
  public float getZIndex() {
    return 1000f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (isExpired() || elapsed < startDelay) {
      return;
    }
    float t = (elapsed - startDelay) / style.duration();
    int frame = Math.min(3, (int) (t * 4f));
    float eased = 1f - (1f - t) * (1f - t);
    Vector2 scale = target.getScale();
    // Fit inside the body height, away from the intent above and the health display below.
    float baseSize = Math.min(scale.x, scale.y);
    float size = baseSize * (style.startScale() + (style.endScale() - style.startScale()) * eased);
    Vector2 centre = target.getCenterPosition();
    float x = centre.x + scale.x * 0.18f - size / 2f;
    float y = centre.y + style.rise() * baseSize * eased - size / 2f;
    // Keep the complete third frame readable; fade only during the dissipation frame.
    float alpha = style.color().a * (t < 0.75f ? 1f : (1f - t) * 4f);
    float previousColor = batch.getPackedColor();
    try {
      batch.setColor(style.color().r, style.color().g, style.color().b, alpha);
      batch.draw(frames[frame], x, y, size, size);
    } finally {
      batch.setPackedColor(previousColor);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    super.dispose();
  }
}
