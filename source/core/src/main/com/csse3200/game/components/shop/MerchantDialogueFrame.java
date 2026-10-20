package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Quiet black dialogue panel with a thin, pixel-cut warm border. */
final class MerchantDialogueFrame extends BaseDrawable {
  private final Drawable pixel;

  MerchantDialogueFrame(Drawable pixel) {
    this.pixel = pixel;
  }

  @Override
  public void draw(Batch batch, float x, float y, float width, float height) {
    Color old = new Color(batch.getColor());
    batch.setColor(0.02f, 0.012f, 0.008f, old.a * 0.9f);
    cutRect(batch, x + 1, y - 1, width, height, 4);
    batch.setColor(0.66f, 0.56f, 0.34f, old.a);
    cutRect(batch, x, y, width, height, 4);
    batch.setColor(0.018f, 0.015f, 0.014f, old.a * 0.97f);
    cutRect(batch, x + 1, y + 1, width - 2, height - 2, 3);
    batch.setColor(old);
  }

  private void cutRect(Batch batch, float x, float y, float width, float height, int cut) {
    pixel.draw(batch, x + cut, y, width - cut * 2, height);
    pixel.draw(batch, x, y + cut, width, height - cut * 2);
    for (int i = 1; i < cut; i++) {
      pixel.draw(batch, x + cut - i, y + i, width - (cut - i) * 2, 1);
      pixel.draw(batch, x + cut - i, y + height - i - 1, width - (cut - i) * 2, 1);
    }
  }
}
