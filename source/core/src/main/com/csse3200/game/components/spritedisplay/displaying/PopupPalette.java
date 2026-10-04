package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;

/**
 * The light-brown "panel" look shared by the deck editor's preview panel ({@link
 * PopupPanelDisplay}) and the per-card frames ({@link CardFramesDisplay}), so the two can never
 * drift apart. Change a colour here and both follow.
 *
 * <p>BattleScreen's display records can read these too, e.g. {@code
 * .colour(PopupPalette.PANEL_FILL.toString())} ({@code Color.toString()} is the hex string {@code
 * Displaying} parses).
 */
public final class PopupPalette {
  /** borderlines around panels and card frames. */
  public static final Color BORDER = Color.valueOf("6B4423"); // brown

  private PopupPalette() {}
}
