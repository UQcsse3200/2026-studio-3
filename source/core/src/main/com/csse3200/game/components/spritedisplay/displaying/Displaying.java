// Displaying.java
package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public abstract class Displaying extends UIComponent {

  private static Skin defaultSkin;

  private final float x;
  private final float y;
  private final float width;
  private final float height;
  private final String trigger;
  protected Label label;

  protected Displaying(DisplayingRecord rec) {
    this.x = rec.x();
    this.y = rec.y();
    this.width = rec.width();
    this.height = rec.height();
    this.trigger = rec.trigger();

    Skin labelSkin = (rec.skin() != null) ? rec.skin() : getDefaultSkin();

    this.label =
        (rec.fontName() != null)
            ? new Label(rec.text(), labelSkin, rec.fontName())
            : new Label(rec.text(), labelSkin);

    if (rec.colour() != null) {
      label.setColor(Color.valueOf(rec.colour()));
    }

    label.setFontScale(rec.scale());
  }

  private static Skin getDefaultSkin() {
    if (defaultSkin == null) {
      defaultSkin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    }
    return defaultSkin;
  }

  @Override
  public void create() {
    super.create();

    // Add label to the stage so it appears on screen
    Stage stage = ServiceLocator.getRenderService().getStage();
    if (stage != null) {
      stage.addActor(label);
    }

    // Listen for events on THIS entity
    if (trigger != null) {
      entity.getEvents().addListener(trigger, this::onTrigger);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Position and size the label every frame
    int screenHeight = Gdx.graphics.getHeight();
    label.setPosition(x, screenHeight - y);

    if (width > 0 && height > 0) {
      label.setSize(width, height);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    label.remove();
  }

  // To be overridden by subclasses (e.g., HealthDisplay)
  public void onTrigger(Object payload) {
    // Default behavior: just set the label text to the payload string
    if (payload != null) {
      label.setText(String.valueOf(payload));
    }
  }

  public Label getLabel() {
    return label;
  }

  public String getTrigger() {
    return trigger;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getWidth() {
    return width;
  }

  public float getHeight() {
    return height;
  }
}
