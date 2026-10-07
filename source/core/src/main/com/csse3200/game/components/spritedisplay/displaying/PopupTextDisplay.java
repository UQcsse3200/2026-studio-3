package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.battle.DeckEditorEvents;
import com.csse3200.game.ui.PopupDisplay;

/**
 * A text {@link Displaying} that is anchored to a {@link PopupDisplay}'s window instead of the
 * screen, and is only visible while that popup is open.
 *
 * <p>Differences from the default {@code Displaying} variant:
 *
 * <ul>
 *   <li>The record's {@code x}/{@code y} are offsets from the window's TOP-LEFT corner (y grows
 *       downwards), not screen coordinates — so the text follows the window if it moves/resizes.
 *   <li>The record's {@code size} is the text box; text wraps inside it, aligned top-left.
 *   <li>Hidden until {@link DeckEditorEvents#OPENED}, hidden again on {@link
 *       DeckEditorEvents#CLOSED}.
 * </ul>
 *
 * Text is set the usual way: the entity fires the record's {@code trigger} with a String payload.
 * Register it per-screen (it needs the popup): {@code displays.registerInstanceVariant("popupText",
 * rec -> new PopupTextDisplay(rec, popup))}.
 */
public class PopupTextDisplay extends Displaying {
  private PopupDisplay popup;

  public PopupTextDisplay(DisplayingRecord rec) {
    super(rec);
    label.setWrap(true);
    label.setAlignment(Align.topLeft);
    label.setVisible(false);
  }

  public void addPopup(PopupDisplay popup) {
    this.popup = popup;
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(DeckEditorEvents.OPENED, () -> label.setVisible(true));
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, () -> label.setVisible(false));
    entity.getEvents().addListener(DeckEditorEvents.TO_FRONT, () -> label.toFront());
  }

  @Override
  protected void draw(SpriteBatch batch) {
    float width = getWidth() > 0 ? getWidth() : label.getPrefWidth();
    float height = getHeight() > 0 ? getHeight() : label.getPrefHeight();
    label.setSize(width, height);
    label.setPosition(
        popup.getWindowX() + getX(),
        popup.getWindowY() + popup.getWindowHeight() - getY() - height);
  }
}
