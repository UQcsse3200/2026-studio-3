package com.csse3200.game.cards;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;

/**
 * Immutable definition of a single card: what it's called, what event it fires when played (and
 * with what arguments), and how it should look as a draggable button.
 *
 * <p>A Card doesn't know how to render or drag itself — {@link #toClickableRecord(float, float,
 * Skin)} builds a {@link ClickableRecord} (variant "drag") that the existing Clickable/DragNDrop
 * machinery already knows how to handle. This keeps card *data* (loaded from configs/cards.json via
 * {@link CardService}) separate from the generic UI plumbing.
 *
 * <p>The Skin itself isn't resolved here — CardService caches Skin instances per skinFile/
 * skinAtlas pair (the same way ClickableFactory does) and passes the resolved Skin in, so repeated
 * calls don't reload the same texture atlas from disk.
 *
 * @param id unique identifier, e.g. "strike". Used for lookups, not shown to the player.
 * @param name display name, e.g. "Strike". Shown as the label in UI feedback ("Strike played").
 * @param description flavour/rules text, not currently rendered anywhere but kept for later UI.
 * @param trigger the event name fired on the drop target when this card is played, e.g. "damage".
 * @param args arguments passed to that event, in order, e.g. {10} for "10 damage". 0-3 supported.
 * @param skinFile path to the button style JSON, e.g. "sprites/cards/cardExample.json".
 * @param skinAtlas path to the matching texture atlas.
 * @param styleName the ImageButtonStyle name within skinFile, e.g. "cardStyle".
 * @param width on-screen width in pixels.
 * @param height on-screen height in pixels.
 */
public record Card(
    String id,
    String name,
    String description,
    String trigger,
    Object[] args,
    String skinFile,
    String skinAtlas,
    String styleName,
    float width,
    float height) {

  /** Builds a draggable ClickableRecord for this card at the given screen position. */
  public ClickableRecord toClickableRecord(float x, float y, Skin resolvedSkin) {
    return ClickableRecord.builder(trigger)
        .label(name)
        .args(args)
        .variant("drag")
        .position(x, y)
        .size(width, height)
        .skin(resolvedSkin)
        .styleName(styleName)
        .build();
  }
}
