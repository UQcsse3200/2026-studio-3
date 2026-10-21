package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Pixel-stepped wood/brass plaques shared by both merchant scene states. */
final class MerchantShopFrame extends BaseDrawable {
  enum Tone {
    NORMAL,
    FREE,
    PRIMARY,
    DANGER,
    MUTED
  }

  private final Drawable pixel;
  private final Color fill;
  private final Color metal;
  private final Color highlight;

  MerchantShopFrame(Drawable pixel, Color fill) {
    this(pixel, fill, Tone.NORMAL);
  }

  MerchantShopFrame(Drawable pixel, Color fill, Tone tone) {
    this.pixel = pixel;
    this.fill = new Color(fill);
    String metalColor = "90652D";
    String highlightColor = "D9B16B";
    if (tone == Tone.MUTED) {
      metalColor = "70695E";
      highlightColor = "A49D8F";
    } else if (tone == Tone.DANGER) {
      metalColor = "8F3427";
      highlightColor = "ED6250";
    }
    metal = Color.valueOf(metalColor);
    highlight = Color.valueOf(highlightColor);
  }

  static TextButtonStyle buttonStyle(Drawable pixel, TextButtonStyle base, Tone tone) {
    TextButtonStyle style = new TextButtonStyle(base);
    Color fill =
        Color.valueOf(
            switch (tone) {
              case MUTED -> "292725";
              case DANGER -> "2B100E";
              case FREE -> "3B2817";
              case PRIMARY -> "3F1810";
              case NORMAL -> "35170F";
            });
    style.up = new MerchantShopFrame(pixel, fill, tone);
    style.over = new MerchantShopFrame(pixel, new Color(fill).mul(1.35f, 1.35f, 1.35f, 1f), tone);
    style.down = new MerchantShopFrame(pixel, new Color(fill).mul(0.8f, 0.8f, 0.8f, 1f), tone);
    style.disabled = new MerchantShopFrame(pixel, fill, tone);
    Color text =
        Color.valueOf(
            switch (tone) {
              case DANGER -> "FF6255";
              case MUTED -> "A8A39B";
              case FREE, PRIMARY -> "FFE29B";
              case NORMAL -> "F4E4C5";
            });
    style.fontColor = text;
    style.disabledFontColor = new Color(text);
    style.overFontColor = Color.valueOf("FFF1CF");
    style.downFontColor = new Color(text);
    return style;
  }

  @Override
  public void draw(Batch batch, float x, float y, float w, float h) {
    Color previous = new Color(batch.getColor());
    float alpha = previous.a;
    batch.setColor(0.07f, 0.027f, 0.012f, alpha);
    stepped(batch, x, y, w, h, 10);
    batch.setColor(metal.r, metal.g, metal.b, alpha);
    stepped(batch, x + 2, y + 2, w - 4, h - 4, 8);
    batch.setColor(highlight.r, highlight.g, highlight.b, alpha);
    stepped(batch, x + 4, y + 4, w - 8, h - 8, 6);
    batch.setColor(0.17f, 0.085f, 0.025f, alpha);
    stepped(batch, x + 5, y + 5, w - 10, h - 10, 5);
    batch.setColor(metal.r * 0.7f, metal.g * 0.7f, metal.b * 0.7f, alpha);
    stepped(batch, x + 6, y + 6, w - 12, h - 12, 4);
    batch.setColor(fill.r, fill.g, fill.b, fill.a * alpha);
    stepped(batch, x + 8, y + 8, w - 16, h - 16, 3);
    batch.setColor(0.23f, 0.11f, 0.043f, alpha * 0.45f);
    for (int row = 0; row < (int) (h - 16) / 9; row++)
      pixel.draw(batch, x + 12 + row % 3 * 4, y + 8 + row * 9, w - 36, 1);
    batch.setColor(highlight.r, highlight.g, highlight.b, alpha * 0.75f);
    pixel.draw(batch, x + 15, y + h - 3, w - 30, 1);
    pixel.draw(batch, x + 16, y + 10, w - 32, 1);
    for (float cx : new float[] {x + 10, x + w - 10}) {
      for (float cy : new float[] {y + 10, y + h - 10}) {
        batch.setColor(0.16f, 0.075f, 0.02f, alpha);
        diamond(batch, cx, cy, 8);
        batch.setColor(metal.r, metal.g, metal.b, alpha);
        diamond(batch, cx, cy, 6);
        batch.setColor(highlight.r, highlight.g, highlight.b, alpha);
        diamond(batch, cx, cy, 3);
        batch.setColor(0.22f, 0.11f, 0.025f, alpha);
        pixel.draw(batch, cx - 1, cy - 1, 2, 2);
      }
    }
    if (w > 320f) {
      batch.setColor(metal.r, metal.g, metal.b, alpha);
      diamond(batch, x + w / 2, y + h - 6, 6);
      batch.setColor(highlight.r, highlight.g, highlight.b, alpha);
      diamond(batch, x + w / 2, y + h - 6, 3);
    }
    batch.setColor(previous);
  }

  private void diamond(Batch batch, float x, float y, int radius) {
    for (int row = -radius; row < radius; row += 2) {
      int halfWidth = radius - Math.abs(row);
      pixel.draw(batch, x - halfWidth, y + row, Math.max(2f, halfWidth * 2f), 2);
    }
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
