package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/**
 * Plays an enemy status sheet, then keeps a visual while the specified status is active.
 *
 * <p>The shared coordinator supplies the delay and owns entity disposal via {@link #isExpired()}.
 * This component neither listens for card/status events nor changes combat state. Textures belong
 * to the resource service. Do not also attach a single-image EffectVisualComponent to this entity.
 */
public class EnemyStatusEffectVisualComponent extends RenderComponent {
  private static final float STATUS_CYCLE_SECONDS = 2.4f;
  private final Entity target;
  private final EffectVisualStyle style;
  private final TextureRegion[] frames;
  private final EffectType statusType;
  private float startDelay;
  private float elapsed;
  private float persistentTime;
  private boolean disposed;
  private boolean badgeVisible;

  /**
   * @param texture a 2-by-2 sheet, or null when the resource is unavailable
   * @param style registered status style; duration is the total duration of all four frames
   * @param target the actual enemy affected by the card
   * @param startDelay delay supplied by the shared coordinator, never computed by this component
   */
  public EnemyStatusEffectVisualComponent(
      Texture texture, EffectVisualStyle style, Entity target, float startDelay) {
    this(texture, style, target, startDelay, null);
  }

  /** A null status type preserves the original one-shot constructor's behaviour. */
  public EnemyStatusEffectVisualComponent(
      Texture texture,
      EffectVisualStyle style,
      Entity target,
      float startDelay,
      EffectType statusType) {
    if (statusType != null && !EnemyStatusEffectVisuals.supports(statusType)) {
      throw new IllegalArgumentException("Unsupported enemy status visual: " + statusType);
    }
    this.style = Objects.requireNonNull(style);
    this.target = Objects.requireNonNull(target);
    this.statusType = statusType;
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
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    elapsed += delta;
    if (statusType != null) {
      persistentTime = (persistentTime + delta) % STATUS_CYCLE_SECONDS;
    }
    if (statusType != null && elapsed >= startDelay + style.duration()) {
      badgeVisible = true;
      elapsed = startDelay + style.duration();
    }
  }

  boolean represents(Entity target, EffectType type) {
    return !disposed && this.target == target && statusType == type;
  }

  /** Reuse the marker on reapplication; keep its badge visible during the queued delay. */
  void replay(float delay) {
    startDelay = Math.max(0f, delay);
    elapsed = 0f;
  }

  /** Reconcile an already active status without inventing another application animation. */
  void showPersistent() {
    startDelay = 0f;
    elapsed = style.duration();
    badgeVisible = true;
  }

  /** The owner should remove expired entities outside this component's update iteration. */
  public boolean isExpired() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    return disposed
        || frames.length == 0
        || (stats != null && stats.isDead())
        || (statusType == null
            ? elapsed >= startDelay + style.duration()
            : stats == null || !stats.hasStatusEffect(statusType.name()));
  }

  @Override
  public float getZIndex() {
    return 1000f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (isExpired() || (elapsed < startDelay && !badgeVisible)) {
      return;
    }
    float t = Math.max(0f, Math.min(1f, (elapsed - startDelay) / style.duration()));
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
    float frameBlend = 0f;
    if (statusType != null && (t >= 1f || elapsed < startDelay)) {
      frame = 2;
      float phase = MathUtils.PI2 * persistentTime / STATUS_CYCLE_SECONDS;
      if (statusType == EffectType.POISON) {
        // Blend the two developed mist frames and drift over the body, without looping dissipation.
        frameBlend = (1f - MathUtils.cos(phase)) * 0.5f;
        size = baseSize * (0.66f + 0.045f * MathUtils.sin(phase));
        x = centre.x - size / 2f + baseSize * 0.07f * MathUtils.sin(phase);
        y = centre.y - size * 0.58f + baseSize * 0.09f * MathUtils.sin(phase + 0.8f);
        alpha = style.color().a * 0.65f;
      } else if (statusType == EffectType.VULNERABLE) {
        float pulse = (1f - MathUtils.cos(phase * 2f)) * 0.5f;
        frameBlend = pulse * 0.7f;
        size = baseSize * (0.55f + 0.1f * pulse);
        x = centre.x - baseSize * 0.08f - size / 2f;
        y = centre.y + baseSize * 0.08f - size / 2f;
        alpha = style.color().a * (0.42f + 0.2f * pulse);
      } else {
        // Feeble arrows descend and fade to zero at the loop boundary, avoiding a visible jump.
        float progress = persistentTime / STATUS_CYCLE_SECONDS;
        frameBlend = (1f - MathUtils.cos(phase)) * 0.5f;
        size = baseSize * 0.46f;
        x = centre.x + baseSize * 0.2f - size / 2f;
        y = centre.y + baseSize * (0.24f - 0.4f * progress) - size / 2f;
        alpha = style.color().a * 0.7f * MathUtils.sin(MathUtils.PI * progress);
      }
    }
    float previousColor = batch.getPackedColor();
    try {
      batch.setColor(style.color().r, style.color().g, style.color().b, alpha * (1f - frameBlend));
      batch.draw(frames[frame], x, y, size, size);
      if (frameBlend > 0f) {
        batch.setColor(style.color().r, style.color().g, style.color().b, alpha * frameBlend);
        batch.draw(frames[1], x, y, size, size);
      }
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
