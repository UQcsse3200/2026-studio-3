package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class CardDisplay extends Displaying {

  private Label text;

  public CardDisplay(DisplayingRecord rec) {
    super(rec);
    this.text = getLabel(); // Get the text from the superclass
  }

  @Override
  public void create() {
    super.create();

    // Cards are now dynamic (see CardService), so we can't hardcode listeners per card
    // trigger. Instead, DragNDrop fires one generic "cardPlayed" event with the card's
    // display text and a description of what it was dropped on, whenever ANY card is
    // successfully dropped — that works for any card without this class needing to know
    // what cards or targets exist.
    entity.getEvents().addListener("cardPlayed", this::onCardPlayed);
  }

  private void onCardPlayed(String cardLabel, String targetLabel) {
    updateLabel(cardLabel + " played (on " + targetLabel + ")!");
  }

  private void updateLabel(String text) {
    this.text.setText(text);
    this.text.setVisible(true);
  }
}
