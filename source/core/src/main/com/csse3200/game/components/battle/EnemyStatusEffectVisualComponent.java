package com.csse3200.game.components.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.Map;
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
  private static final float POISON_CYCLE_SECONDS = 3.6f;
  private static final float SINGLE_SCALE = 1.4f;
  private static final float ORBIT_EFFECT_WIDTH = 0.55f;
  private static final float ORBIT_RADIUS_X = 0.65f;
  private static final float ORBIT_RADIUS_Y = 0.2f;
  private static final double ORBIT_SECONDS = 3.6;
  private static final float PERSISTENT_ALPHA = 0.9f;
  private static GameTime orbitTimeSource;
  private static long orbitFrame;
  private static double orbitTime;
  private static final EffectType[] STATUS_ORDER = {
    EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE
  };
  private final Entity target;
  private final EffectVisualStyle style;
  private final TextureRegion[] frames;
  private final EffectType statusType;
  private float startDelay;
  private float elapsed;
  private float persistentTime;
  private boolean disposed;
  private boolean badgeVisible;
  private final Map<TextureRegion, Rectangle> visibleFrameBounds = new HashMap<>();

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
      persistentTime = (persistentTime + delta) % cycleSeconds();
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
    if (!isPersistentPhase()) return 1000f;
    AnimationRenderComponent renderer = target.getComponent(AnimationRenderComponent.class);
    float enemyZ = renderer == null ? -target.getPosition().y : renderer.getZIndex();
    return enemyZ + 0.02f;
  }

  private boolean isPersistentPhase() {
    return statusType != null
        && (elapsed >= startDelay + style.duration() || elapsed < startDelay && badgeVisible);
  }

  private float cycleSeconds() {
    return statusType == EffectType.POISON ? POISON_CYCLE_SECONDS : STATUS_CYCLE_SECONDS;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (isExpired() || (elapsed < startDelay && !badgeVisible)) {
      return;
    }
    float t = Math.max(0f, Math.min(1f, (elapsed - startDelay) / style.duration()));
    boolean persistent = isPersistentPhase();
    int frame = Math.min(3, (int) (t * 4f));
    float eased = 1f - (1f - t) * (1f - t);
    Vector2 scale = target.getScale();
    float baseSize = Math.min(scale.x, scale.y);
    Vector2 centre = target.getCenterPosition();
    float size = baseSize * (style.startScale() + (style.endScale() - style.startScale()) * eased);
    float x = centre.x + (statusType == null ? scale.x * 0.18f : 0f) - size / 2f;
    float y = centre.y + style.rise() * baseSize * eased - size / 2f;
    if (statusType != null && !persistent) {
      // Keep the application burst independent of the persistent layout.
      size = baseSize * (1.05f + 0.2f * eased);
      x = centre.x - size / 2f;
      y = centre.y - size / 2f;
    }
    // Keep the complete third frame readable; fade only during the dissipation frame.
    float fade = t < 0.75f ? 1f : (1f - t) * 4f;
    float alpha = style.color().a * fade * fade * (3f - 2f * fade);
    float width = size;
    float height = size;
    if (persistent) {
      Rectangle bounds = visibleTargetBounds();
      centre.set(bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f);
      // Replay all four cells instead of keeping the developed frame on screen.
      frame = Math.min(frames.length - 1, (int) (persistentTime / cycleSeconds() * frames.length));
      alpha = PERSISTENT_ALPHA;
      CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
      int count = 0;
      int slot = 0;
      for (EffectType type : STATUS_ORDER) {
        if (stats.hasStatusEffect(type.name())) {
          if (type == statusType) slot = count;
          count++;
        }
      }
      double phase = sharedOrbitTime() / ORBIT_SECONDS;
      if (count == 1) {
        width = bounds.width * SINGLE_SCALE;
        height = bounds.height * SINGLE_SCALE;
      } else {
        double angle = Math.PI * 2d * (phase + slot / (double) count);
        width = bounds.width * ORBIT_EFFECT_WIDTH;
        height = width;
        centre.x += bounds.width * ORBIT_RADIUS_X * (float) Math.cos(angle);
        centre.y += bounds.height * ORBIT_RADIUS_Y * (float) Math.sin(angle);
      }
      x = centre.x - width / 2f;
      y = centre.y - height / 2f;
    }
    float previousColor = batch.getPackedColor();
    try {
      batch.setColor(style.color().r, style.color().g, style.color().b, alpha);
      batch.draw(frames[frame], x, y, width, height);
    } finally {
      batch.setPackedColor(previousColor);
    }
  }

  private static double sharedOrbitTime() {
    GameTime time = ServiceLocator.getTimeSource();
    long frame = Gdx.graphics.getFrameId();
    if (orbitTimeSource != time) {
      orbitTimeSource = time;
      orbitFrame = frame;
      orbitTime = 0d;
    } else if (orbitFrame != frame) {
      // One shared step per frame, independent of component creation and replay; pauses with game
      // time.
      orbitFrame = frame;
      orbitTime = (orbitTime + time.getDeltaTime()) % ORBIT_SECONDS;
    }
    return orbitTime;
  }

  private Rectangle visibleTargetBounds() {
    Vector2 position = target.getPosition();
    Vector2 scale = target.getScale();
    AnimationRenderComponent renderer = target.getComponent(AnimationRenderComponent.class);
    TextureRegion frame = renderer == null ? null : renderer.getRenderedFrame();
    Rectangle bounds =
        frame == null
            ? new Rectangle(0f, 0f, 1f, 1f)
            : visibleFrameBounds.computeIfAbsent(frame, this::readVisibleFrameBounds);
    return new Rectangle(
        position.x + bounds.x * scale.x,
        position.y + bounds.y * scale.y,
        bounds.width * scale.x,
        bounds.height * scale.y);
  }

  private Rectangle readVisibleFrameBounds(TextureRegion frame) {
    if (!(frame.getTexture().getTextureData() instanceof FileTextureData data)) {
      return new Rectangle(0f, 0f, 1f, 1f);
    }
    // Read a separate pixmap so the renderer's uploaded texture and ownership are untouched.
    Pixmap pixels = new Pixmap(data.getFileHandle());
    try {
      int width = frame.getRegionWidth();
      int height = frame.getRegionHeight();
      int originX = Math.round(Math.min(frame.getU(), frame.getU2()) * pixels.getWidth());
      int originY = Math.round(Math.min(frame.getV(), frame.getV2()) * pixels.getHeight());
      int left = width;
      int top = height;
      int right = -1;
      int bottom = -1;

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if ((pixels.getPixel(originX + x, originY + y) & 0xff) == 0) continue;
          left = Math.min(left, x);
          right = Math.max(right, x);
          top = Math.min(top, y);
          bottom = Math.max(bottom, y);
        }
      }
      if (right < left) return new Rectangle(0f, 0f, 1f, 1f);
      return new Rectangle(
          (frame.isFlipX() ? width - right - 1 : left) / (float) width,
          (frame.isFlipY() ? top : height - bottom - 1) / (float) height,
          (right - left + 1) / (float) width,
          (bottom - top + 1) / (float) height);
    } finally {
      pixels.dispose();
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    visibleFrameBounds.clear();
    super.dispose();
  }
}
