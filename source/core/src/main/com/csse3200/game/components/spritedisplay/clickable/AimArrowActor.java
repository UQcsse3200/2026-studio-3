package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Non-interactive curved card aim and outlines of the actual selectable target bounds. */
final class AimArrowActor extends Actor {
  private static final Color IDLE = new Color(0.94f, 0.88f, 0.72f, 1f);
  private static final Color SELECTED = new Color(0.96f, 0.32f, 0.24f, 1f);
  private static final int CURVE_SEGMENTS = 32;
  private final Texture pixel;
  private final Vector2 start = new Vector2();
  private final Vector2 end = new Vector2();
  private final Vector2 control1 = new Vector2();
  private final Vector2 control2 = new Vector2();
  private final Vector2 bodyEnd = new Vector2();
  private final Vector2 from = new Vector2();
  private final Vector2 to = new Vector2();
  private final Vector2 direction = new Vector2();
  private final Map<String, Rectangle> targetBounds = new LinkedHashMap<>();
  private final float[] triangleVertices = new float[20];
  private Set<String> highlighted = Set.of();
  private Rectangle battlefield;
  private boolean drawArrow;
  private boolean valid;

  AimArrowActor() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    pixel = new Texture(pixmap);
    pixmap.dispose();
    setTouchable(Touchable.disabled);
    setVisible(false);
  }

  void show(
      Vector2 source,
      Vector2 destination,
      Map<String, Rectangle> bounds,
      Set<String> highlightedTargets,
      boolean showArrow,
      Rectangle battlefieldBounds,
      boolean validDrop) {
    start.set(source);
    end.set(destination);
    targetBounds.clear();
    bounds.forEach((id, box) -> targetBounds.put(id, new Rectangle(box)));
    highlighted = Set.copyOf(highlightedTargets);
    drawArrow = showArrow;
    battlefield = battlefieldBounds == null ? null : new Rectangle(battlefieldBounds);
    valid = validDrop;
    setVisible(true);
    toFront();
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    Color previous = batch.getColor().cpy();
    float alpha = parentAlpha * getColor().a;
    if (battlefield != null) {
      tint(batch, IDLE, alpha * (valid ? 0.5f : 0.2f));
      corners(batch, battlefield, 1.5f, 22f);
    }
    targetBounds.forEach(
        (id, box) -> {
          boolean selected = highlighted.contains(id);
          tint(batch, IDLE, alpha * (selected ? 0.65f : 0.22f));
          outline(batch, box, 1f);
          if (selected) {
            tint(batch, SELECTED, alpha * 0.95f);
            corners(batch, box, 2f, 14f);
          }
        });
    if (drawArrow && start.dst2(end) > 1f) drawCurve(batch, alpha);
    batch.setColor(previous);
  }

  private void drawCurve(Batch batch, float alpha) {
    float rise = Math.min(180f, Math.max(45f, start.dst(end) * 0.4f));
    control1.set(start.x, start.y + rise);
    control2.set(end.x - (end.x - start.x) * 0.15f, end.y + rise * 0.35f);
    direction.set(end).sub(control2).nor();
    float headLength = Math.min(25f, start.dst(end) * 0.3f);
    bodyEnd.set(end).mulAdd(direction, -headLength * 0.8f);
    // Two passes give the tapered, segmented curve a dark edge on bright backgrounds.
    for (int pass = 0; pass < 2; pass++) {
      if (pass == 0) batch.setColor(0.12f, 0.08f, 0.06f, alpha * 0.7f);
      else tint(batch, valid ? SELECTED : IDLE, alpha);
      for (int i = 0; i < CURVE_SEGMENTS; i++) {
        float t = i / (float) CURVE_SEGMENTS;
        curvePoint(from, t);
        curvePoint(to, (i + 0.82f) / CURVE_SEGMENTS);
        line(batch, from.x, from.y, to.x, to.y, 2.5f + t * 4f + (pass == 0 ? 3f : 0f));
      }
      float length = headLength + (pass == 0 ? 3f : 0f);
      float halfWidth = length * 0.48f;
      float baseX = end.x - direction.x * length;
      float baseY = end.y - direction.y * length;
      triangle(
          batch,
          end.x,
          end.y,
          baseX - direction.y * halfWidth,
          baseY + direction.x * halfWidth,
          baseX + direction.y * halfWidth,
          baseY - direction.x * halfWidth);
    }
  }

  private void curvePoint(Vector2 out, float t) {
    float u = 1f - t;
    out.set(
        u * u * u * start.x
            + 3f * u * u * t * control1.x
            + 3f * u * t * t * control2.x
            + t * t * t * bodyEnd.x,
        u * u * u * start.y
            + 3f * u * u * t * control1.y
            + 3f * u * t * t * control2.y
            + t * t * t * bodyEnd.y);
  }

  private void triangle(Batch batch, float x1, float y1, float x2, float y2, float x3, float y3) {
    float colour = batch.getColor().toFloatBits();
    vertex(0, x1, y1, colour);
    vertex(5, x2, y2, colour);
    vertex(10, x3, y3, colour);
    vertex(15, x3, y3, colour);
    batch.draw(pixel, triangleVertices, 0, triangleVertices.length);
  }

  private void vertex(int offset, float x, float y, float colour) {
    triangleVertices[offset] = x;
    triangleVertices[offset + 1] = y;
    triangleVertices[offset + 2] = colour;
    triangleVertices[offset + 3] = 0.5f;
    triangleVertices[offset + 4] = 0.5f;
  }

  private void tint(Batch batch, Color colour, float alpha) {
    batch.setColor(colour.r, colour.g, colour.b, alpha);
  }

  private void outline(Batch batch, Rectangle box, float thickness) {
    float right = box.x + box.width;
    float top = box.y + box.height;
    line(batch, box.x, box.y, right, box.y, thickness);
    line(batch, right, box.y, right, top, thickness);
    line(batch, right, top, box.x, top, thickness);
    line(batch, box.x, top, box.x, box.y, thickness);
  }

  private void corners(Batch batch, Rectangle box, float thickness, float length) {
    float size = Math.min(length, Math.min(box.width, box.height) / 3f);
    for (int xSide = 0; xSide < 2; xSide++) {
      for (int ySide = 0; ySide < 2; ySide++) {
        float x = box.x + xSide * box.width;
        float y = box.y + ySide * box.height;
        line(batch, x, y, x + (xSide == 0 ? size : -size), y, thickness);
        line(batch, x, y, x, y + (ySide == 0 ? size : -size), thickness);
      }
    }
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
