package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** A text-sized, feathered dark field with no panel outline or texture allocation. */
final class EncounterResultShade extends BaseDrawable {
  private final Label label;
  private final Drawable pixel;
  private final float strength;

  EncounterResultShade(Label label, Drawable pixel, float strength) {
    this.label = label;
    this.pixel = pixel;
    this.strength = strength;
  }

  static float opacity(float horizontal, float vertical) {
    float distance = (float) Math.sqrt(Math.pow(horizontal, 4) + vertical * vertical);
    float t = Math.clamp((distance - .28f) / .72f, 0f, 1f);
    return 1f - t * t * (3f - 2f * t);
  }

  @Override
  public void draw(Batch batch, float x, float y, float width, float height) {
    float saved = batch.getPackedColor();
    float alpha = batch.getColor().a;
    float fieldWidth = Math.min(width, label.getGlyphLayout().width + 96f);
    float fieldHeight = Math.max(52f, label.getGlyphLayout().height + 44f);
    float left = x + (width - fieldWidth) / 2f;
    float bottom = y + (height - fieldHeight) / 2f;
    for (int col = 0; col < 96; col++) {
      for (int row = 0; row < 24; row++) {
        float fade = opacity((col + .5f) / 48f - 1f, (row + .5f) / 12f - 1f);
        batch.setColor(0f, 0f, 0f, strength * fade * alpha);
        pixel.draw(
            batch,
            left + col * fieldWidth / 96f,
            bottom + row * fieldHeight / 24f,
            fieldWidth / 96f,
            fieldHeight / 24f);
      }
    }
    batch.setPackedColor(saved);
  }
}
