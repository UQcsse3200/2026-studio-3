package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;

/** Displays placeholder regions for the enemy and player sides of a battle. */
public class BattleArea extends UIComponent {
  private Table root;

  @Override
  public void create() {
    super.create();
    root = new Table();
    root.setFillParent(true);
    root.pad(30f, 30f, 150f, 30f);
    Table enemyArea = createPlaceholder("ENEMY AREA", new Color(0.65f, 0.18f, 0.18f, 0.85f));
    Table playerArea = createPlaceholder("PLAYER AREA", new Color(0.18f, 0.35f, 0.65f, 0.85f));
    root.add(enemyArea).expand().fill().padBottom(20f);
    root.row();
    root.add(playerArea).expand().fill();
    stage.addActor(root);
  }

  private Table createPlaceholder(String text, Color colour) {
    Table area = new Table();
    area.setBackground(skin.newDrawable("white", colour));
    area.add(new Label(text, skin, "large"));
    return area;
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the Scene2D stage.
  }

  @Override
  public void dispose() {
    root.remove();
    super.dispose();
  }
}
