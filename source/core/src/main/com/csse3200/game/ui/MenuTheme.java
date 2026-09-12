package com.csse3200.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;

/** Shared palette and basic dimensions for the menus. */
public final class MenuTheme {
  public static final float BUTTON_WIDTH = 360f;
  public static final float BUTTON_HEIGHT = 64f;
  public static final float BUTTON_SPACING = 14f;
  public static final float TITLE_SPACING = 30f;
  public static final float PANEL_PADDING = 24f;
  public static final float SCREEN_PADDING = 40f;

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

  /** Builds a scalable button style from the skin's existing NinePatch button drawable. */
  public static TextButtonStyle createButtonStyle(Skin skin) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.font = skin.getFont("font_large");
    style.up = skin.newDrawable("button", burntRust());
    style.over = skin.newDrawable("button", softCoral());
    style.down = skin.newDrawable("button-pressed", dustyMauve());
    style.disabled = skin.newDrawable("button", mutedBrown());
    style.fontColor = warmParchment();
    style.overFontColor = deepPlum();
    style.downFontColor = warmParchment();
    style.disabledFontColor = earthBrown();
    return style;
  }
}
