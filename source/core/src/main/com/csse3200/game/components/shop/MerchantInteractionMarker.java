package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Independent rune hit target; hovering the merchant does not activate this actor. */
final class MerchantInteractionMarker extends Actor {
  private final Drawable pixel;
  private boolean hovered;
  private float animationTime;

  MerchantInteractionMarker(Drawable pixel, Runnable approach) {
    this.pixel = pixel;
    setName("merchant-approach");
    addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            approach.run();
          }

          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1) hovered = true;
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            super.exit(event, x, y, pointer, toActor);
            if (pointer == -1) {
              hovered = false;
            }
          }
        });
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    animationTime += delta;
  }

  @Override
  public void draw(Batch batch, float alpha) {
    Color old = new Color(batch.getColor());
    float pulse = 0.5f + 0.5f * (float) Math.sin(animationTime * 2f);
    float x = getX() + getWidth() / 2f;
    // Animate only the painted symbol; the transparent click target remains fixed.
    float y = getY() + 58 + (float) Math.sin(animationTime * 1.8f) * 1.5f + (hovered ? 2f : 0f);
    batch.setColor(1f, 0.65f, 0.16f, alpha * (hovered ? 0.20f : 0.08f) * (0.7f + pulse * 0.3f));
    diamond(batch, x, y, 29, 7);
    batch.setColor(0.19f, 0.08f, 0.015f, alpha * 0.8f);
    diamond(batch, x, y, 26, 6);
    batch.setColor(
        1f, hovered ? 0.91f : 0.79f, hovered ? 0.57f : 0.32f, alpha * (0.85f + pulse * 0.15f));
    diamond(batch, x, y, 24, hovered ? 5 : 4);
    for (int row = -5; row <= 5; row++) {
      int halfWidth = 5 - Math.abs(row);
      pixel.draw(batch, x - halfWidth - .5f, y + row - .5f, halfWidth * 2f + 1f, 1);
    }
    for (int i = 0; i < 6; i++) {
      int offset = 15 - i * 3;
      pixel.draw(batch, x - offset - 2, y - 34 - i * 3, 4, 4);
      if (offset != 0) pixel.draw(batch, x + offset - 2, y - 34 - i * 3, 4, 4);
    }
    batch.setColor(1f, 0.94f, 0.7f, alpha * (0.65f + pulse * 0.35f));
    pixel.draw(batch, x - 2, y + 24, 4, 4);
    pixel.draw(batch, x - 1, y - 1, 2, 2);
    batch.setColor(old);
  }

  private void diamond(Batch batch, float x, float y, int radius, int stroke) {
    for (int row = -radius; row <= radius; row++) {
      int halfWidth = radius - Math.abs(row);
      int width = halfWidth * 2 + 1;
      if (width <= stroke * 2) {
        pixel.draw(batch, x - width / 2f, y + row - .5f, (float) width, 1);
      } else {
        pixel.draw(batch, x - width / 2f, y + row - .5f, (float) stroke, 1);
        pixel.draw(batch, x + width / 2f - stroke, y + row - .5f, (float) stroke, 1);
      }
    }
  }
}
