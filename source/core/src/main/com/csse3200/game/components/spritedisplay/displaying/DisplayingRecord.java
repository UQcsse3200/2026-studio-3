// DisplayingRecord.java
package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public record DisplayingRecord(
    CharSequence text,
    String trigger,
    Skin skin,
    String fontName,
    String colour,
    float x,
    float y,
    float width,
    float height,
    float scale,
    String variant) {

  private static final float NO_SIZE = -1;
  private static final float NO_SCALE = 1f;
  private static final String DEFAULT_VARIANT = "Displaying";

  public DisplayingRecord {
    if (variant == null) {
      variant = DEFAULT_VARIANT;
    }
  }

  public static Builder builder(CharSequence text) {
    return new Builder(text);
  }

  public boolean hasSize() {
    return width != NO_SIZE && height != NO_SIZE;
  }

  public static final class Builder {
    private final CharSequence text;
    private String trigger;
    private Skin skin;
    private String fontName;
    private String colour;
    private float x;
    private float y;
    private float width = NO_SIZE;
    private float height = NO_SIZE;
    private float scale = NO_SCALE;
    private String variant = DEFAULT_VARIANT;

    private Builder(CharSequence text) {
      this.text = text;
    }

    public Builder trigger(String trigger) {
      this.trigger = trigger;
      return this;
    }

    public Builder skin(Skin skin) {
      this.skin = skin;
      return this;
    }

    public Builder fontName(String fontName) {
      this.fontName = fontName;
      return this;
    }

    public Builder colour(String colour) {
      this.colour = colour;
      return this;
    }

    public Builder position(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
    }

    public Builder size(float width, float height) {
      this.width = width;
      this.height = height;
      return this;
    }

    public Builder scale(float scale) {
      this.scale = scale;
      return this;
    }

    public Builder variant(String variant) {
      this.variant = variant;
      return this;
    }

    public DisplayingRecord build() {
      return new DisplayingRecord(
          text, trigger, skin, fontName, colour, x, y, width, height, scale, variant);
    }
  }
}
