package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/**
 * A plain text button drawn in the gothic bronze frame used by the battle menu buttons (see {@link
 * BattleMenuSkins#frameStyle}), without an icon. Select it from JSON with {@code "variant":
 * "framed"}; the button keeps its own skin's font.
 */
public class Framed extends Clickable {

  public Framed(ClickableRecord rec) {
    super(rec);
    if (btn instanceof TextButton textButton) {
      textButton.setStyle(BattleMenuSkins.frameStyle(textButton.getStyle().font));
    }
  }
}
