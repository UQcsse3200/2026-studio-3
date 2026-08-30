// Clickable.java
package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.components.Component;

public abstract class Clickable extends Component {
  // Shared default skin, only loaded if the record doesn't provide one.
  private static Skin defaultSkin;

  float x;
  float y;
  Button btn;
  Skin btnSkin;
  String trigger;
  float width;
  float height;

  private static Skin getDefaultSkin() {
    if (defaultSkin == null) {
      defaultSkin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    }
    return defaultSkin;
  }

  public Clickable(ClickableRecord record) {
    this.x = record.x();
    this.y = record.y();
    this.width = record.width();
    this.height = record.height();
    this.btnSkin = (record.btnSkin() != null) ? record.btnSkin() : getDefaultSkin();

    String text = record.text();
    String styleName = record.styleName();

    this.btn =
        switch (record.type()) {
          case TEXT ->
              (styleName != null)
                  ? new TextButton(text, btnSkin, styleName)
                  : new TextButton(text, btnSkin);
          case IMAGE ->
              (styleName != null) ? new ImageButton(btnSkin, styleName) : new ImageButton(btnSkin);
          case IMAGE_TEXT ->
              (styleName != null)
                  ? new ImageTextButton(text, btnSkin, styleName)
                  : new ImageTextButton(text, btnSkin);
        };

    init(record.trigger());
  }

  private void init(String trigger) {
    this.trigger = trigger;
    btn.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            onEnter();
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            onExit();
          }
        });
    btn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            onClick();
          }
        });
  }

  protected void onEnter() {
    btn.setColor(1.2f, 1.2f, 1.2f, 1f); // brighten
  }

  protected void onExit() {
    btn.setColor(1f, 1f, 1f, 1f); // reset
  }

  protected void onClick() {
    entity.getEvents().trigger(trigger);
  }

  public Button getBtn() {
    return btn;
  }

  public Skin getBtnSkin() {
    return btnSkin;
  }

  public String getTrigger() {
    return trigger;
  }

  public float getY() {
    return y;
  }

  public float getX() {
    return x;
  }

  public float getWidth() {
    return width;
  }

  public float getHeight() {
    return height;
  }

  public void draw() {
    int screenHeight = Gdx.graphics.getHeight();
    btn.setPosition(this.getX(), screenHeight - this.getY());

    if (this.getWidth() > 0 && this.getHeight() > 0) {
      btn.setSize(this.getWidth(), this.getHeight());
    }
  }
}
