package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

/** A non-interactive arrow and target outline drawn over the battle stage. */
final class AimArrowActor extends Actor {
  private final Texture pixel;
  private final Vector2 start = new Vector2();
  private final Vector2 end = new Vector2();
  private Rectangle targetBounds;

  AimArrowActor() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    pixel = new Texture(pixmap);
    pixmap.dispose();
    setTouchable(Touchable.disabled);
    setVisible(false);
  }

  void show(Vector2 source, Vector2 destination, Rectangle selectedBounds) {
    start.set(source);
    end.set(destination);
    targetBounds = selectedBounds == null ? null : new Rectangle(selectedBounds);
    setVisible(true);
    toFront();
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    Color previous = batch.getColor().cpy();
    batch.setColor(targetBounds == null ? Color.WHITE : Color.CYAN);
    line(batch, start.x, start.y, end.x, end.y, 5f);

    Vector2 direction = new Vector2(end).sub(start);
    if (direction.len2() > 0.001f) {
      direction.nor();
      float baseX = end.x - direction.x * 22f;
      float baseY = end.y - direction.y * 22f;
      line(batch, end.x, end.y, baseX - direction.y * 10f, baseY + direction.x * 10f, 5f);
      line(batch, end.x, end.y, baseX + direction.y * 10f, baseY - direction.x * 10f, 5f);
    }

    if (targetBounds != null) {
      float x = targetBounds.x;
      float y = targetBounds.y;
      float right = x + targetBounds.width;
      float top = y + targetBounds.height;
      line(batch, x, y, right, y, 3f);
      line(batch, right, y, right, top, 3f);
      line(batch, right, top, x, top, 3f);
      line(batch, x, top, x, y, 3f);
    }
    batch.setColor(previous);
  }

  private void line(Batch batch, float x1, float y1, float x2, float y2, float thickness) {
    float dx = x2 - x1;
    float dy = y2 - y1;
    float length = (float) Math.sqrt(dx * dx + dy * dy);
    float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
    batch.draw(
        pixel,
        x1,
        y1 - thickness / 2f,
        0f,
        thickness / 2f,
        length,
        thickness,
        1f,
        1f,
        angle,
        0,
        0,
        1,
        1,
        false,
        false);
  }

  void dispose() {
    remove();
    pixel.dispose();
  }
}
