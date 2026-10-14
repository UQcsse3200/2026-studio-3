package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Shared 16:10 coordinates for both camera states and their live controls. */
final class MerchantSceneLayout extends WidgetGroup {
  final Group scene = new Group();

  MerchantSceneLayout() {
    setTouchable(Touchable.childrenOnly);
    scene.setTouchable(Touchable.childrenOnly);
    scene.setSize(1280, 800);
    addActor(scene);
  }

  @Override
  public void layout() {
    float scale = Math.min(getWidth() / 1280f, getHeight() / 800f);
    scene.setScale(scale);
    scene.setPosition((getWidth() - 1280 * scale) / 2, (getHeight() - 800 * scale) / 2);
  }

  static Drawable frame(Drawable pixel, Color fill) {
    return new BaseDrawable() {
      @Override
      public void draw(Batch batch, float x, float y, float width, float height) {
        Color previous = new Color(batch.getColor());
        batch.setColor(fill.r, fill.g, fill.b, fill.a * previous.a);
        pixel.draw(batch, x, y, width, height);
        // Sparse horizontal grain and small broken streaks keep the wood pixel-based.
        for (int row = 10; row < height - 8; row += 9) {
          batch.setColor(fill.r * 1.22f, fill.g * 1.2f, fill.b * 1.12f, previous.a * fill.a);
          pixel.draw(batch, x + 8, y + row, width - 16, 2);
          batch.setColor(fill.r * 0.72f, fill.g * 0.75f, fill.b * 0.8f, previous.a * fill.a);
          for (int col = 18 + row % 17; col < width - 20; col += 63)
            pixel.draw(batch, x + col, y + row - 3, Math.min(25, width - col - 8), 1);
        }
        batch.setColor(0.12f, 0.075f, 0.04f, previous.a);
        border(pixel, batch, x, y, width, height, 7);
        batch.setColor(0.46f, 0.32f, 0.15f, previous.a);
        border(pixel, batch, x + 2, y + 2, width - 4, height - 4, 3);
        batch.setColor(0.69f, 0.51f, 0.26f, previous.a);
        pixel.draw(batch, x + 4, y + height - 4, width - 8, 1);
        pixel.draw(batch, x + 3, y + 4, 1, height - 8);
        for (float cx : new float[] {x + 4, x + width - 14})
          for (float cy : new float[] {y + 4, y + height - 14}) {
            batch.setColor(0.37f, 0.24f, 0.1f, previous.a);
            pixel.draw(batch, cx, cy, 10, 10);
            batch.setColor(0.72f, 0.54f, 0.28f, previous.a);
            pixel.draw(batch, cx + 2, cy + 2, 6, 6);
            batch.setColor(0.9f, 0.72f, 0.43f, previous.a);
            pixel.draw(batch, cx + 3, cy + 5, 3, 2);
            batch.setColor(0.2f, 0.12f, 0.055f, previous.a);
            pixel.draw(batch, cx + 4, cy + 3, 2, 2);
          }
        batch.setColor(previous);
      }
    };
  }

  private static void border(
      Drawable pixel, Batch batch, float x, float y, float w, float h, float thickness) {
    pixel.draw(batch, x, y, w, thickness);
    pixel.draw(batch, x, y + h - thickness, w, thickness);
    pixel.draw(batch, x, y, thickness, h);
    pixel.draw(batch, x + w - thickness, y, thickness, h);
  }
}
