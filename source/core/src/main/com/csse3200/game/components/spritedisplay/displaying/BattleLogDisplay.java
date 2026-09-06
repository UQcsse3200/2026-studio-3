package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

/**
 * A short, self-dismissing line describing the most recent battle action, so the player sees "you
 * did X" between their turn and the enemy's, and "the enemy did Y" before their next turn.
 *
 * <p>Registered as the {@link DisplayingFactory} {@code "battleLog"} variant. The record's {@code
 * trigger} ("battleLog") is wired up by {@link Displaying}; this class only adds the fade animation
 * and the win/lose lines.
 */
public class BattleLogDisplay extends Displaying {
  private static final float FADE_IN = 0.15f;
  private static final float HOLD = 2.5f;
  private static final float FADE_OUT = 0.4f;

  public BattleLogDisplay(DisplayingRecord rec) {
    super(rec);
  }

  @Override
  public void create() {
    super.create();
    label.getColor().a = 0f;
    entity.getEvents().addListener("battleWon", () -> onTrigger("VICTORY!"));
    entity.getEvents().addListener("battleLost", () -> onTrigger("DEFEAT..."));
  }

  @Override
  public void onTrigger(Object payload) {
    if (payload == null) {
      return;
    }
    label.setText(String.valueOf(payload));
    label.clearActions();
    label.addAction(
        Actions.sequence(
            Actions.alpha(1f, FADE_IN), Actions.delay(HOLD), Actions.alpha(0f, FADE_OUT)));
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Keep the line centred horizontally near the top of the screen.
    label.setPosition(
        (Gdx.graphics.getWidth() - label.getPrefWidth()) / 2f, Gdx.graphics.getHeight() - getY());
  }
}
