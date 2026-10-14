package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

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
  private static final Color BATTLE_LOG_FONT_COLOUR =
      new Color(0.8588235294117647f, 0.7450980392156863f, 0.6313725490196078f, 1);
  private static final Color BATTLE_LOG_BACKGROUND_COLOUR =
      new Color(0.105f, 0.070f, 0.065f, 0.98f);

  public BattleLogDisplay(DisplayingRecord rec) {
    super(rec);
  }

  @Override
  public void create() {
    super.create();

    // creating a backdrop for the text so it can be seen against backgrounds of all colours
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.background = skin.newDrawable("color", BATTLE_LOG_BACKGROUND_COLOUR);
    label.setStyle(style);

    label.getStyle().fontColor = BATTLE_LOG_FONT_COLOUR;
    label.setColor(BATTLE_LOG_FONT_COLOUR);
    label.getColor().a = 0f; // setting the alpha to 0 so it's invisible but still there
    label.setAlignment(Align.center);

    entity.getEvents().addListener("battleWon", () -> onTrigger("VICTORY!"));
    entity.getEvents().addListener("battleLost", () -> onTrigger("DEFEAT..."));
  }

  @Override
  public void onTrigger(Object payload) {
    if (payload == null) {
      return;
    }

    if (label.getStage() == null) {
      return;
    }
    float maxWidth = label.getStage().getViewport().getWorldWidth() / 1.75f;

    label.setText(String.valueOf(payload));
    label.setWrap(true);
    label.setWidth(maxWidth);
    label.pack();
    label.setWidth(maxWidth);
    label.setAlignment(Align.center);

    label.clearActions();
    label.addAction(
        Actions.sequence(
            Actions.alpha(1f, FADE_IN), Actions.delay(HOLD), Actions.alpha(0f, FADE_OUT)));
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Keep the line centred horizontally near the top of the screen.
    label.setPosition(
        (label.getStage().getViewport().getWorldWidth() - label.getWidth()) / 2f,
        label.getStage().getViewport().getWorldHeight() - getY());
  }
}
