package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class CardDisplay extends Displaying {

  private Label label;

  public CardDisplay(DisplayingRecord record) {
    super(record);
    this.label = getLabel(); // Get the label from the superclass
  }

  @Override
  public void create() {
    super.create();

    // Listen for each card trigger
    entity.getEvents().addListener("card1", () -> updateLabel("Card 1 clicked!"));
    entity.getEvents().addListener("card2", () -> updateLabel("Card 2 clicked!"));
    entity.getEvents().addListener("card3", () -> updateLabel("Card 3 clicked!"));
    entity.getEvents().addListener("card4", () -> updateLabel("Card 4 clicked!"));
  }

  private void updateLabel(String text) {
    label.setText(text);
    label.setVisible(true);
  }
}
