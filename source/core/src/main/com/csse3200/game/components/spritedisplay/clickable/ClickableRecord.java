// ClickableRecord.java
package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import java.util.Arrays;
import java.util.Objects;

public record ClickableRecord(
    String text,
    Skin btnSkin,
    float x,
    float y,
    String styleName,
    String trigger,
    ButtonType type,
    float width,
    float height,
    String variant,
    Object[] args,
    String label) {

  public enum ButtonType {
    TEXT,
    IMAGE,
    IMAGE_TEXT
  }

  private static final float NO_SIZE = -1;
  private static final String DEFAULT_VARIANT = "Clickable";
  private static final Object[] NO_ARGS = new Object[0];

  public ClickableRecord {
    if (variant == null) {
      variant = DEFAULT_VARIANT;
    }
    if (args == null) {
      args = NO_ARGS;
    }
    if (label == null) {
      label = trigger;
    }
  }

  public boolean hasSize() {
    return width != NO_SIZE && height != NO_SIZE;
  }

  // Explicit equals/hashCode/toString because the auto-generated record versions compare the
  // Object[] args by reference, not content. A 12-component record deconstruction pattern here
  // would be far less readable than accessor comparison, so java:S6878 is suppressed.
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof ClickableRecord other
        && Float.compare(x, other.x) == 0
        && Float.compare(y, other.y) == 0
        && Float.compare(width, other.width) == 0
        && Float.compare(height, other.height) == 0
        && type == other.type
        && Objects.equals(text, other.text)
        && Objects.equals(btnSkin, other.btnSkin)
        && Objects.equals(styleName, other.styleName)
        && Objects.equals(trigger, other.trigger)
        && Objects.equals(variant, other.variant)
        && Arrays.equals(args, other.args)
        && Objects.equals(label, other.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        text,
        btnSkin,
        x,
        y,
        styleName,
        trigger,
        type,
        width,
        height,
        variant,
        Arrays.hashCode(args),
        label);
  }

  @Override
  public String toString() {
    return "ClickableRecord[text="
        + text
        + ", btnSkin="
        + btnSkin
        + ", x="
        + x
        + ", y="
        + y
        + ", styleName="
        + styleName
        + ", trigger="
        + trigger
        + ", type="
        + type
        + ", width="
        + width
        + ", height="
        + height
        + ", variant="
        + variant
        + ", args="
        + Arrays.toString(args)
        + ", label="
        + label
        + "]";
  }

  public static Builder builder(String trigger) {
    return new Builder(trigger);
  }

  public static final class Builder {
    private String text;
    private Skin btnSkin;
    private float x;
    private float y;
    private String styleName;
    private final String trigger;
    private float width = NO_SIZE;
    private float height = NO_SIZE;
    private String variant = DEFAULT_VARIANT;
    private Object[] args = NO_ARGS;
    private String label;

    private Builder(String trigger) {
      this.trigger = trigger;
    }

    public Builder text(String text) {
      this.text = text;
      return this;
    }

    public Builder skin(Skin btnSkin) {
      this.btnSkin = btnSkin;
      return this;
    }

    public Builder position(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
    }

    public Builder styleName(String styleName) {
      this.styleName = styleName;
      return this;
    }

    public Builder size(float width, float height) {
      this.width = width;
      this.height = height;
      return this;
    }

    public Builder variant(String variant) {
      this.variant = variant;
      return this;
    }

    /** Arguments carried in the drag payload and passed to the fired event, in order. */
    public Builder args(Object... args) {
      this.args = args;
      return this;
    }

    /**
     * Human-readable label for UI feedback (e.g. shown when the card is played). Defaults to
     * trigger.
     */
    public Builder label(String label) {
      this.label = label;
      return this;
    }

    public ClickableRecord build() {
      ButtonType type = inferType(text, btnSkin);
      return new ClickableRecord(
          text, btnSkin, x, y, styleName, trigger, type, width, height, variant, args, label);
    }

    private static ButtonType inferType(String text, Skin btnSkin) {
      if (text == null) {
        return ButtonType.IMAGE;
      }
      if (btnSkin != null) {
        return ButtonType.IMAGE_TEXT;
      }
      return ButtonType.TEXT;
    }
  }
}
