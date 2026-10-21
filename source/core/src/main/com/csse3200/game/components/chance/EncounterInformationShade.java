package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** A texture-free, input-free shade behind the existing encounter information layout. */
final class EncounterInformationShade extends BaseDrawable {
  private static final float[] OPACITY_STOPS = {.88f, .70f, .56f, .44f, .32f, .18f, 0f};
  private final Table information;
  private final Drawable pixel;

  EncounterInformationShade(Table information, Drawable pixel) {
    this.information = information;
    this.pixel = pixel;
  }

  static float opacity(float fraction) {
    float t = Math.clamp(fraction, 0f, 1f);
    // Seven connected stops keep the outer fade gradual rather than dropping off early.
    int last = OPACITY_STOPS.length - 1;
    int segment = Math.min(last - 1, (int) (t * last));
    float u = t * last - segment;
    float start = OPACITY_STOPS[segment];
    float end = OPACITY_STOPS[segment + 1];
    float startSlope =
        segment == 0 ? 0f : (OPACITY_STOPS[segment + 1] - OPACITY_STOPS[segment - 1]) / 2f;
    float endSlope =
        segment == last - 1 ? 0f : (OPACITY_STOPS[segment + 2] - OPACITY_STOPS[segment]) / 2f;
    float u2 = u * u;
    float u3 = u2 * u;
    return ((2f * u3 - 3f * u2 + 1f) * start
        + (u3 - 2f * u2 + u) * startSlope
        + (-2f * u3 + 3f * u2) * end
        + (u3 - u2) * endSlope);
  }

  private static float smooth(float value) {
    float t = Math.clamp(value, 0f, 1f);
    return t * t * (3f - 2f * t);
  }

  static float spatialOpacity(float horizontal, float fromTop, float height, float topFade) {
    float down = Math.max(0f, (fromTop - topFade) / Math.max(1f, height - topFade));
    // The cross term tilts the soft ellipse towards the bottom right. Vertical
    // falloff is faster than horizontal, so the lower-left cannot remain a dark slab.
    float distance =
        (float)
            Math.sqrt(
                .85f * horizontal * horizontal + 1.45f * down * down + .40f * horizontal * down);
    float upperFeather = smooth(fromTop / topFade);
    float rightFeather = 1f - smooth((horizontal - .82f) / .18f);
    // Keep a broad dark core behind the text; reserve most fading for its perimeter.
    float outerDistance = Math.max(0f, (distance - .20f) / .80f);
    return opacity(outerDistance * outerDistance) * upperFeather * rightFeather;
  }

  @Override
  public void draw(Batch batch, float x, float y, float width, float height) {
    float saved = batch.getPackedColor();
    float parentAlpha = batch.getColor().a;
    float shadeWidth = width * .56f;
    float shadeHeight = Math.min(height * .65f, information.getPrefHeight() + 160f);
    float topFade = Math.clamp(information.getPadTop(), 24f, 56f);
    float bottom = y + height - shadeHeight;
    // Sample a two-dimensional field, not a horizontal ramp multiplied by a box mask.
    for (int column = 0; column < 96; column++) {
      float horizontal = (column + .5f) / 96f;
      for (int row = 0; row < 48; row++) {
        float low = shadeHeight * row / 48f;
        float high = shadeHeight * (row + 1) / 48f;
        float fromTop = shadeHeight - (low + high) / 2f;
        float alpha = spatialOpacity(horizontal, fromTop, shadeHeight, topFade);
        batch.setColor(0f, 0f, 0f, alpha * parentAlpha);
        pixel.draw(
            batch, x + shadeWidth * column / 96f, bottom + low, shadeWidth / 96f, high - low);
      }
    }
    batch.setPackedColor(saved);
  }
}
