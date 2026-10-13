package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.battle.BattleActions;
import com.csse3200.game.services.ServiceLocator;

/** Displaying component for displaying the enemy turn message */
public class EnemyTurnDisplay extends Displaying {

  public EnemyTurnDisplay(DisplayingRecord rec) {
    super(rec);
  }

  @Override
  public void create() {
    super.create();
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.font = skin.getFont("button");
    style.fontColor = Color.valueOf("E8C894");

    Texture texture =
        ServiceLocator.getResourceService()
            .getAsset("images/ui/inventory-panel.png", Texture.class);
    style.background = new TextureRegionDrawable(texture);

    label.setStyle(style);
    label.setAlignment(Align.center);
    label.setColor(1f, 1f, 1f, 0f);
    label.setText("Enemy Turn");

    entity.getEvents().addListener(BattleActions.ENEMY_TURN_EVENT, this::showEnemyTurn);
  }

  /** Displays the enemy turn message with a fade-in and fade-out animation. */
  public void showEnemyTurn() {
    label.clearActions();

    label.addAction(
        Actions.sequence(
            Actions.alpha(0f),
            Actions.alpha(1f, 0.3f),
            Actions.delay(0.6f),
            Actions.alpha(0, 0.3f)));
  }

  @Override
  protected void draw(SpriteBatch batch) {
    super.draw(batch);
    float stageWidth = stage.getViewport().getWorldWidth();
    label.setX((stageWidth - label.getWidth()) / 2f);
  }
}
