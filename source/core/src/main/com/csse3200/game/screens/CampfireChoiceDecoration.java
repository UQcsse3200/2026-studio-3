package com.csse3200.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

/** Non-interactive scene ornaments; all choice callbacks remain owned by CampfireScreen. */
final class CampfireChoiceDecoration extends Group {
  private final Drawable pixel;
  private final boolean heading;
  private float elapsed;
  private boolean hovered;

  CampfireChoiceDecoration(Skin skin, boolean heading) {
    this.heading = heading;
    pixel = skin.getDrawable("white");
    setTouchable(Touchable.disabled);
    setSize(280, heading ? 160 : 310);
    if (heading) {
      text(skin, "Campfire", 0, 53, 280, 60, 2f, new Color(1f, .87f, .48f, 1));
      text(skin, "Choose your next step", -60, 24, 400, 30, .8f, new Color(.92f, .79f, .59f, 1));
    }
  }

  void optionText(Skin skin, String title) {
    text(skin, title, 0, 151, 280, 28, .85f, new Color(1f, .86f, .57f, 1));
  }

  private void text(
      Skin skin,
      String text,
      float x,
      float y,
      float width,
      float height,
      float scale,
      Color color) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    if (heading) style.fontColor = Color.WHITE;
    Label label = new Label(text, style);
    label.setFontScale(scale);
    label.setAlignment(Align.center);
    label.setWrap(true);
    label.setColor(color);
    label.setBounds(x, y, width, height);
    label.setTouchable(Touchable.disabled);
    addActor(label);
  }

  void setHovered(boolean hovered) {
    this.hovered = hovered;
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    elapsed += delta;
    for (Actor child : getChildren()) child.getColor().a = heading || hovered ? 1f : .90f;
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    super.draw(batch, parentAlpha);
    Color previous = new Color(batch.getColor());
    float cx = getX() + 140, cy = getY() + (heading ? 130 : 230);
    float alpha = parentAlpha * getColor().a;
    if (heading) {
      batch.setColor(1f, .76f, .36f, alpha * .8f);
      dot(batch, cx - 175, cy - 6, 140, 1);
      dot(batch, cx + 35, cy - 6, 140, 1);
      star(batch, cx, cy, 12);
      diamond(batch, cx, cy, 19);
      diamond(batch, cx - 178, cy - 6, 3);
      diamond(batch, cx + 178, cy - 6, 3);
      dot(batch, cx - 145, getY() + 54, 290, 1);
      diamond(batch, cx - 165, getY() + 54, 8);
      diamond(batch, cx + 165, getY() + 54, 8);
      dot(batch, cx - 155, getY() + 14, 130, 1);
      dot(batch, cx + 25, getY() + 14, 130, 1);
      star(batch, cx, getY() + 14, 5);
      diamond(batch, cx, getY() + 14, 7);
    } else {
      float strength = hovered ? 1f : .73f + .05f * (float) Math.sin(elapsed * 1.7f);
      for (int i = 0; i < 180; i++) {
        double angle = Math.PI * 2 * i / 180;
        float x = cx + Math.round(50 * (float) Math.cos(angle));
        float y = cy + Math.round(50 * (float) Math.sin(angle));
        batch.setColor(1f, .62f, .17f, alpha * strength * .12f);
        dot(batch, x - 2, y - 2, 5, 5);
        batch.setColor(1f, .79f, .39f, alpha * strength);
        dot(batch, x, y, 2, 2);
        if (i % 2 == 0) {
          float innerX = cx + Math.round(44 * (float) Math.cos(angle));
          float innerY = cy + Math.round(44 * (float) Math.sin(angle));
          dot(batch, innerX, innerY, 1, 1);
        }
      }
      batch.setColor(1f, .85f, .49f, alpha * strength);
      for (int i = 0; i < 8; i++) {
        double angle = Math.PI * i / 4;
        star(
            batch,
            cx + Math.round(54 * (float) Math.cos(angle)),
            cy + Math.round(54 * (float) Math.sin(angle)),
            i % 2 == 0 ? 5 : 3);
      }
      for (int i = 0; i < (hovered ? 8 : 5); i++) {
        double angle = i * 2.4;
        batch.setColor(1f, .81f, .39f, alpha * (.35f + .35f * (float) Math.sin(elapsed * 2 + i)));
        star(
            batch,
            cx + Math.round(64 * (float) Math.cos(angle)),
            cy + Math.round(60 * (float) Math.sin(angle)),
            2);
      }
      batch.setColor(1f, .76f, .36f, alpha * .7f);
      dot(batch, cx - 42, getY() + 147, 84, 1);
      diamond(batch, cx, getY() + 141, 3);
    }
    batch.setColor(previous);
  }

  private void star(Batch batch, float x, float y, int radius) {
    dot(batch, x - radius, y, radius * 2 + 1, 1);
    dot(batch, x, y - radius, 1, radius * 2 + 1);
    dot(batch, x - 1, y - 1, 3, 3);
  }

  private void diamond(Batch batch, float x, float y, int radius) {
    for (int i = 0; i <= radius; i++) {
      dot(batch, x - radius + i, y + i, 1, 1);
      dot(batch, x + radius - i, y + i, 1, 1);
      dot(batch, x - radius + i, y - i, 1, 1);
      dot(batch, x + radius - i, y - i, 1, 1);
    }
  }

  private void dot(Batch batch, float x, float y, float width, float height) {
    pixel.draw(batch, Math.round(x), Math.round(y), width, height);
  }
}
