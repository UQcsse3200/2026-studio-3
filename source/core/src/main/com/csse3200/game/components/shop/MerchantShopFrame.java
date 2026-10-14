package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Pixel-stepped corner wood/brass plaques, shared by formal-shop controls only. */
final class MerchantShopFrame extends BaseDrawable {
  private final Drawable pixel;
  private final Color fill;

  MerchantShopFrame(Drawable pixel, Color fill) {
    this.pixel = pixel;
    this.fill = new Color(fill);
  }

  @Override
  public void draw(Batch batch, float x, float y, float w, float h) {
    Color previous = new Color(batch.getColor());
    float alpha = previous.a;
    batch.setColor(0.07f, 0.027f, 0.012f, alpha);
    stepped(batch, x, y, w, h, 8);
    batch.setColor(0.46f, 0.29f, 0.12f, alpha);
    stepped(batch, x + 2, y + 2, w - 4, h - 4, 6);
    batch.setColor(fill.r, fill.g, fill.b, fill.a * alpha);
    stepped(batch, x + 4, y + 4, w - 8, h - 8, 4);
    batch.setColor(0.23f, 0.11f, 0.043f, alpha * 0.45f);
    for (int row = 0; row < (int) (h - 16) / 9; row++)
      pixel.draw(batch, x + 12 + row % 3 * 4, y + 8 + row * 9, w - 36, 1);
    batch.setColor(0.72f, 0.5f, 0.23f, alpha);
    pixel.draw(batch, x + 10, y + h - 3, w - 20, 1);
    for (float cx : new float[] {x + 8, x + w - 11}) {
      for (float cy : new float[] {y + 8, y + h - 11}) {
        batch.setColor(0.25f, 0.12f, 0.04f, alpha);
        pixel.draw(batch, cx - 1, cy - 1, 5, 5);
        batch.setColor(0.76f, 0.57f, 0.29f, alpha);
        pixel.draw(batch, cx, cy, 3, 3);
      }
    }
    batch.setColor(previous);
  }

  private void stepped(Batch batch, float x, float y, float w, float h, int radius) {
    // Two-pixel stair steps preserve actual transparent corners, not a square underlay.
    pixel.draw(batch, x, y + radius, w, h - radius * 2);
    for (int row = 0; row < radius; row += 2) {
      int inset = radius - row;
      pixel.draw(batch, x + inset, y + row, w - inset * 2, 2);
      pixel.draw(batch, x + inset, y + h - row - 2, w - inset * 2, 2);
    }
  }
}
