package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

/**
 * A centred line on the battle end screen (the "VICTORY" / "DEFEAT" heading, or the "click to
 * continue" hint). Registered as the {@link DisplayingFactory} {@code "endBattle"} variant.
 *
 * <p>The heading text is filled in when the screen fires {@link #RESULT_EVENT}. {@link Displaying}
 * has no button. A defeat may be dismissed with any click or key press; a victory is dismissed by
 * the reward component only after one reward has been claimed.
 */
public class EndBattleDisplay extends Displaying {
  /** Event the screen fires to fill in the heading text ("VICTORY" / "DEFEAT"). */
  public static final String RESULT_EVENT = "endBattleResult";

  /** Event this component fires when the player clicks or presses a key to leave. */
  public static final String RETURN_TO_MENU_EVENT = "returnToMenu";

  private boolean fired = false;
  private boolean clickToReturnEnabled = true;
  private InputListener returnInputListener;

  public EndBattleDisplay(DisplayingRecord rec) {
    super(rec);
  }

  /**
   * Controls whether clicking/pressing a key anywhere on this screen triggers {@link
   * #RETURN_TO_MENU_EVENT}. Disabled on victory screens that show a reward the player must pick
   * first — otherwise an accidental click before choosing a reward returns to the menu without the
   * reward ever being claimed.
   */
  public void setClickToReturnEnabled(boolean enabled) {
    this.clickToReturnEnabled = enabled;
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(RESULT_EVENT, this::configureForResult);
    // EndBattle.json contains both a heading and a continue hint. Only the hint owns the stage
    // listener, otherwise one input would dispatch the return event twice.
    if (getTrigger() == null) {
      returnInputListener =
          new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
              return requestReturn();
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
              return requestReturn();
            }
          };
      stage.addListener(returnInputListener);
    }
  }

  private boolean requestReturn() {
    if (fired || !clickToReturnEnabled) {
      return false;
    }
    fired = true;
    entity.getEvents().trigger(RETURN_TO_MENU_EVENT);
    return true;
  }

  private void configureForResult(Object result) {
    clickToReturnEnabled = "DEFEAT".equals(String.valueOf(result));
    if (label.getText().toString().startsWith("Click anywhere")) {
      label.setVisible(clickToReturnEnabled);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Centre horizontally; use the record's y as an offset down from the top of the screen.
    label.setPosition(
        (label.getStage().getViewport().getWorldWidth() - label.getPrefWidth()) / 2f,
        label.getStage().getViewport().getWorldHeight() - getY());
  }

  public void setVisible(boolean visible) {
    label.setVisible(visible);
  }

  @Override
  public void dispose() {
    if (returnInputListener != null) {
      stage.removeListener(returnInputListener);
      returnInputListener = null;
    }
    super.dispose();
  }
}
