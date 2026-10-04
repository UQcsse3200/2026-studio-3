package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

public class CardActor extends Table {

  public CardActor(String cardName, String texturePath, Skin skin) {
    this.setBackground(skin.newDrawable("white", new Color(0.82f, 0.71f, 0.55f, 1f)));

    Image artImage = new Image(new Texture(texturePath));

    this.add(artImage).size(80, 80).padTop(8).padLeft(8).padRight(8);

    Label nameLabel = new Label(cardName, skin);
    nameLabel.setAlignment(Align.center);
    nameLabel.setColor(Color.BLACK); // Make sure it's readable on the beige background

    this.row();
    this.add(nameLabel).expandX().fillX().padBottom(8).padTop(4);
  }
}
