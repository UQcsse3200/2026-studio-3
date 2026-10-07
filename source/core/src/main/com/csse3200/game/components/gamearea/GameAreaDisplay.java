package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.ui.UIComponent;

/** Displays the name of the current game area. */
public class GameAreaDisplay extends UIComponent {
  private static final String BATTLE_TITLE_TEXTURE = "images/ui/battle-title.png";
  private static final int TITLE_CROP_X = 20;
  private static final int TITLE_CROP_Y = 215;
  private static final int TITLE_CROP_WIDTH = 2130;
  private static final int TITLE_CROP_HEIGHT = 270;
  private static final float TITLE_WIDTH = 450f;
  private static final float TITLE_HEIGHT = 57f;
  private String gameAreaName = "";
  private Texture titleTexture;
  private Image title;

  public GameAreaDisplay(String gameAreaName) {
    this.gameAreaName = gameAreaName;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    titleTexture = new Texture(BATTLE_TITLE_TEXTURE);
    titleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegion titleRegion =
        new TextureRegion(
            titleTexture, TITLE_CROP_X, TITLE_CROP_Y, TITLE_CROP_WIDTH, TITLE_CROP_HEIGHT);
    title = new Image(new TextureRegionDrawable(titleRegion));
    title.setSize(TITLE_WIDTH, TITLE_HEIGHT);
    stage.addActor(title);
  }

  @Override
  public void draw(SpriteBatch batch) {
    float stageHeight = stage.getViewport().getWorldHeight();
    float offsetX = 14f;
    float offsetY = 72f;

    title.setPosition(offsetX, stageHeight - offsetY);
  }

  @Override
  public void dispose() {
    super.dispose();
    title.remove();
    titleTexture.dispose();
  }
}
