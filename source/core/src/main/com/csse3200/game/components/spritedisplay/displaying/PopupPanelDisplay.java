package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.csse3200.game.components.battle.DeckEditorEvents;
import com.csse3200.game.ui.PopupDisplay;

/**
 * A display-only filled panel with a coloured border, anchored to a {@link PopupDisplay}'s window
 * and only visible while that popup is open. Used as the backdrop behind other displays (e.g. the
 * card preview): create it FIRST in the factory's record list so it sits behind them.
 *
 * <ul>
 *   <li>The record's {@code x}/{@code y} are offsets from the window's TOP-LEFT corner (y grows
 *       downwards) and {@code size} is the whole panel, border included.
 *   <li>The record's {@code colour} (hex RRGGBB) is the FILL colour (e.g. {@code
 *       PopupPalette.PANEL_FILL.toString()}). The border is {@link PopupPalette#BORDER}, {@link
 *       #BORDER_WIDTH} px wide.
 * </ul>
 *
 * Register per-screen (it needs the popup): {@code registerInstanceVariant("popupPanel", rec -> {
 * PopupPanelDisplay d = new PopupPanelDisplay(rec); d.addPopup(popup); return d; })}.
 */
public class PopupPanelDisplay extends Displaying {
  private static final float BORDER_WIDTH = 2f;

  private final Image border = new Image();
  private final Image fill = new Image();
  private PopupDisplay popup;

  public PopupPanelDisplay(DisplayingRecord rec) {
    super(rec);
    label.setVisible(false); // the inherited label isn't used
    border.setTouchable(Touchable.disabled);
    fill.setTouchable(Touchable.disabled);
    border.setVisible(false);
    fill.setVisible(false);
  }

  /** The popup whose window this panel is anchored to. Call before the factory creates it. */
  public void addPopup(PopupDisplay popup) {
    this.popup = popup;
  }

  @Override
  public void create() {
    super.create();
    // Displaying already tinted its (unused) label with the record's colour: reuse it as the fill.
    Color fillColour = new Color(label.getColor());
    border.setDrawable(skin.newDrawable("white", PopupPalette.BORDER));
    fill.setDrawable(skin.newDrawable("white", fillColour));
    stage.addActor(border);
    stage.addActor(fill);

    entity.getEvents().addListener(DeckEditorEvents.OPENED, () -> setShown(true));
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, () -> setShown(false));
    entity
        .getEvents()
        .addListener(
            DeckEditorEvents.TO_FRONT,
            () -> {
              border.toFront();
              fill.toFront();
            });
  }

  private void setShown(boolean shown) {
    border.setVisible(shown);
    fill.setVisible(shown);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (popup == null) {
      return;
    }
    float left = popup.getWindowX() + getX();
    float bottom = popup.getWindowY() + popup.getWindowHeight() - getY() - getHeight();
    border.setBounds(left, bottom, getWidth(), getHeight());
    fill.setBounds(
        left + BORDER_WIDTH,
        bottom + BORDER_WIDTH,
        getWidth() - 2 * BORDER_WIDTH,
        getHeight() - 2 * BORDER_WIDTH);
  }

  @Override
  public void dispose() {
    border.remove();
    fill.remove();
    super.dispose();
  }
}
