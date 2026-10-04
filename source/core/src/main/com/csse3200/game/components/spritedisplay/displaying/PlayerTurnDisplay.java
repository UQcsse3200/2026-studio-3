package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

public class PlayerTurnDisplay extends Displaying {
  private static final float FADE_IN = 0.3f;
  private static final float HOLD = 1.5f;
  private static final float FADE_OUT = 0.6f;

  public PlayerTurnDisplay(DisplayingRecord record) {
    super(record);
  }

  @Override
  public void create() {
    super.create();

    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = Color.valueOf("DBBEA1");
    style.background = skin.newDrawable("white", new Color(0.05f, 0.04f, 0.08f, 0.75f));

    label.setStyle(style);
    label.setAlignment(Align.center);
    label.setColor(1f, 1f, 1f, 0f);

    entity.getEvents().addListener("playerTurnStarted", this::onPlayerTurnStarted);
  }

  private void onPlayerTurnStarted(int turnNumber) {
    label.setText("PLAYER TURN\n\nTurn " + turnNumber);
    label.setFontScale(1.25f);
    label.clearActions();
    label.addAction(
        Actions.sequence(
            Actions.alpha(0f),
            Actions.alpha(1f, FADE_IN),
            Actions.delay(HOLD),
            Actions.alpha(0f, FADE_OUT)));
  }

  @Override
  protected void draw(SpriteBatch batch) {
    super.draw(batch);

    float stageWidth = stage.getViewport().getWorldWidth();
    label.setX((stageWidth - label.getWidth()) / 2f);
  }
}
