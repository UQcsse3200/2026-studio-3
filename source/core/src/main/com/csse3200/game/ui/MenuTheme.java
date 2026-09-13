package com.csse3200.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/** Shared palette and basic dimensions for the menus. */
public final class MenuTheme {
  // Constants for the main menu layout
  public static final float BUTTON_WIDTH = 330f;
  public static final float BUTTON_HEIGHT = 110f;
  public static final float BUTTON_SPACING = 0f;
  public static final float TITLE_SPACING = 6f;
  public static final float SCREEN_PADDING = 20f;

  private MenuTheme() {
    throw new IllegalStateException("Utility class");
  }

  public static Color deepPlum() {
    return Color.valueOf("371E30");
  }

  public static Color earthBrown() {
    return Color.valueOf("594F38");
  }

  public static Color burntRust() {
    return Color.valueOf("A24936");
  }

  public static Color dustyMauve() {
    return Color.valueOf("896279");
  }

  public static Color warmParchment() {
    return Color.valueOf("D8BEA1");
  }

  public static Color softCoral() {
    return Color.valueOf("D87F67");
  }

  public static Color mutedBrown() {
    return Color.valueOf("776258");
  }

  /** Builds interaction states by tinting one reusable pixel-art button frame. */
  public static TextButtonStyle createButtonStyle(Skin skin, Texture buttonFrameTexture) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    TextureRegionDrawable frame = new TextureRegionDrawable(new TextureRegion(buttonFrameTexture));
    style.font = skin.getFont("font_large");
    style.up = frame;
    style.over = frame.tint(softCoral());
    style.down = frame.tint(dustyMauve());
    style.disabled = frame.tint(mutedBrown());
    style.fontColor = warmParchment();
    style.overFontColor = Color.WHITE;
    style.downFontColor = warmParchment();
    style.disabledFontColor = mutedBrown();
    return style;
  }
}
