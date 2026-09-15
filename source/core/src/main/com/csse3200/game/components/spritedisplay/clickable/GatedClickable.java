package com.csse3200.game.components.spritedisplay.clickable;

/**
 * A plain button — no slide in/out animation — that blocks clicks while the enemy's turn is in
 * progress. Listens to the same "up"/"down" events {@link InOutOnTrigger} hand cards use ("up"
 * when the player's turn begins, "down" the instant the enemy's turn starts, including through the
 * "enemy thinking" pause). Use for standing actions like "End Turn" that must not be actionable
 * mid-enemy-turn.
 */
public class GatedClickable extends Clickable {
  public GatedClickable(ClickableRecord rec) {
    super(rec);
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("up", () -> setInteractable(true));
    entity.getEvents().addListener("down", () -> setInteractable(false));
  }
}
