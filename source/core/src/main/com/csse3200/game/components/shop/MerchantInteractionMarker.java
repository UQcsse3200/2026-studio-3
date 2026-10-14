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
  private float hoverTime;

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
              hoverTime = 0;
            }
          }
        });
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (hovered) hoverTime += delta;
  }

  @Override
  public void draw(Batch batch, float alpha) {
    Color old = new Color(batch.getColor());
    float x = getX() + 28;
    float y = getY() + 36 + (hovered ? 2 + (float) Math.sin(hoverTime * 5) * 2 : 0);
    if (hovered) {
      batch.setColor(0.95f, 0.79f, 0.35f, alpha * 0.16f);
      diamond(batch, x, y, 23, 4);
    }
    batch.setColor(1f, hovered ? 0.92f : 0.83f, hovered ? 0.66f : 0.5f, alpha);
    diamond(batch, x, y, 18, 2);
    diamond(batch, x, y, 5, 2);
    for (int i = 0; i < 6; i++) {
      pixel.draw(batch, x - 10 + i * 2, y - 26 - i * 2, 2, 2);
      pixel.draw(batch, x + 10 - i * 2, y - 26 - i * 2, 2, 2);
    }
    batch.setColor(old);
  }

  private void diamond(Batch batch, float x, float y, int radius, int stroke) {
    for (int i = 0; i <= radius; i += 2) {
      for (int direction : new int[] {-1, 1}) {
        pixel.draw(batch, x + direction * i, y + radius - i, stroke, stroke);
        pixel.draw(batch, x + direction * i, y - radius + i, stroke, stroke);
      }
    }
  }
}
