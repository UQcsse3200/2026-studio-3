package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;

/** Shared visual construction for the card and story libraries. */
final class LibraryUiHelper {
  private static final String WHITE = "white";

  private LibraryUiHelper() {}

  /** Adds the library background and its tinted overlay. */
  static void addBackground(Skin skin, Stack stack) {
    Texture backgroundTexture = getTexture(MainMenuDisplay.BACKGROUND_TEXTURE);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    stack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.62f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable(WHITE, overlayColour));
    stack.add(overlay);
  }

  /** Adds the shared divider to the library panel. */
  static void addDivider(Skin skin, Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, MenuTheme.softCoral()));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(16f);
  }

  /** Copies a skin label style and applies the requested colour. */
  static Label.LabelStyle labelStyle(Skin skin, String baseStyle, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(baseStyle, Label.LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private static Texture getTexture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  /** Creates the shared library button style and state drawables. */
  static TextButton.TextButtonStyle createButtonStyle(Skin skin) {
    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    style.font = skin.getFont("font_large");
    style.up = buttonDrawable(null);
    style.over = buttonDrawable(new Color(1f, 0.88f, 0.68f, 1f));
    style.down = buttonDrawable(new Color(0.7f, 0.48f, 0.58f, 1f));
    style.checked = buttonDrawable(new Color(1f, 0.72f, 0.34f, 1f));
    style.checkedOver = buttonDrawable(new Color(1f, 0.86f, 0.55f, 1f));
    style.fontColor = MenuTheme.warmParchment();
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.checkedFontColor = Color.WHITE;
    return style;
  }

  private static Drawable buttonDrawable(Color tint) {
    Texture texture = getTexture(CardLibraryDisplay.BUTTON_TEXTURE);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
    drawable.setMinWidth(0f);
    drawable.setMinHeight(0f);
    return tint == null ? drawable : drawable.tint(tint);
  }

  /** Adds the eyebrow, title and subtitle with shared alignment and spacing. */
  static void populateTitleBlock(Table titleBlock, Label eyebrow, Label title, Label subtitle) {
    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);
  }
}
