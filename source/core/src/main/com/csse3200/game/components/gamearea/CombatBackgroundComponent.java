package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RenderComponent;

/**
 * Draws a static background above terrain and behind combatants.
 *
 * <p>The image follows an unrotated orthographic camera and covers its view without stretching.
 * Excess image content is cropped. The caller owns texture loading and disposal.
 */
public class CombatBackgroundComponent extends RenderComponent {
  private static final int BACKGROUND_LAYER = 0;
  private static final float BACKGROUND_Z = 1f;

  private final Texture texture;
  private final OrthographicCamera camera;

  /**
   * @param texture already-loaded background texture
   * @param camera world camera used for combat rendering
   */
  public CombatBackgroundComponent(Texture texture, OrthographicCamera camera) {
    this.texture = texture;
    this.camera = camera;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    float width = camera.viewportWidth * camera.zoom;
    float height = camera.viewportHeight * camera.zoom;

    if (width <= 0f || height <= 0f) return;

    float scale = Math.max(width / texture.getWidth(), height / texture.getHeight());

    float drawWidth = texture.getWidth() * scale;
    float drawHeight = texture.getHeight() * scale;

    float left = camera.position.x - drawWidth / 2f;
    float bottom = camera.position.y - drawHeight / 2f;

    batch.draw(texture, left, bottom, drawWidth, drawHeight);
  }

  @Override
  public int getLayer() {
    return BACKGROUND_LAYER;
  }

  @Override
  public float getZIndex() {
    return BACKGROUND_Z;
  }
}
