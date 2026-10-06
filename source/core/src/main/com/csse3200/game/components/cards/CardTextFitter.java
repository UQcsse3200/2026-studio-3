package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

/** Fits one whole text block uniformly into its allocated card placeholder. */
final class CardTextFitter {
  private CardTextFitter() {}

  static void fit(Label label, float maximumScale) {
    if (label.getWidth() <= 0f || label.getHeight() <= 0f) {
      return;
    }

    BitmapFont font = label.getStyle().font;
    float originalScaleX = font.getScaleX();
    float originalScaleY = font.getScaleY();
    float scale = maximumScale;
    GlyphLayout layout = new GlyphLayout();
    try {
      if (!fits(label, font, layout, maximumScale)) {
        float lower = 0f;
        float upper = maximumScale;
        for (int i = 0; i < 12; i++) {
          float candidate = (lower + upper) / 2f;
          if (fits(label, font, layout, candidate)) {
            lower = candidate;
          } else {
            upper = candidate;
          }
        }
        scale = lower;
      }
    } finally {
      // Skin fonts are shared by other cards and screens. Only this label keeps the fitted scale.
      font.getData().setScale(originalScaleX, originalScaleY);
    }

    if (Math.abs(label.getFontScaleX() - scale) > 0.0001f
        || Math.abs(label.getFontScaleY() - scale) > 0.0001f) {
      label.setFontScale(scale);
    }
  }

  private static boolean fits(Label label, BitmapFont font, GlyphLayout layout, float scale) {
    font.getData().setScale(scale);
    layout.setText(
        font, label.getText(), Color.WHITE, label.getWidth(), Align.center, label.getWrap());
    return layout.width <= label.getWidth()
        && layout.height - font.getDescent() * 2f <= label.getHeight();
  }
}
