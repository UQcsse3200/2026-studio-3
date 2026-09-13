package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

/**
 * A centred line on the battle end screen (the "VICTORY" / "DEFEAT" heading, or the "click to
 * continue" hint). Registered as the {@link DisplayingFactory} {@code "endBattle"} variant.
 *
 * <p>The heading text is filled in when the screen fires {@link #RESULT_EVENT}. {@link Displaying}
 * has no button, so any click or key press fires {@link #RETURN_TO_MENU_EVENT} on this component's
 * entity; the screen listens for it and navigates back to the main menu.
 */
public class EndBattleDisplay extends Displaying {
  /** Event the screen fires to fill in the heading text ("VICTORY" / "DEFEAT"). */
  public static final String RESULT_EVENT = "endBattleResult";

  /** Event this component fires when the player clicks or presses a key to leave. */
  public static final String RETURN_TO_MENU_EVENT = "returnToMenu";

  private boolean fired = false;

  public EndBattleDisplay(DisplayingRecord rec) {
    super(rec);
  }

  @Override
  public void create() {
    super.create();
    stage.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            return requestReturn();
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            return requestReturn();
          }
        });
  }

  private boolean requestReturn() {
    if (fired) {
      return false;
    }
    fired = true;
    entity.getEvents().trigger(RETURN_TO_MENU_EVENT);
    return true;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Centre horizontally; use the record's y as an offset down from the top of the screen.
    label.setPosition(
        (Gdx.graphics.getWidth() - label.getPrefWidth()) / 2f, Gdx.graphics.getHeight() - getY());
  }
}
