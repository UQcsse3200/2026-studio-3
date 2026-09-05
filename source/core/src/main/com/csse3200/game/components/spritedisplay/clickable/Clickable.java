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
  Object[] args;
  String label;

  private static Skin getDefaultSkin() {
    if (defaultSkin == null) {
      defaultSkin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    }
    return defaultSkin;
  }

  protected Clickable(ClickableRecord rec) {
    this.x = rec.x();
    this.y = rec.y();
    this.width = rec.width();
    this.height = rec.height();
    this.btnSkin = (rec.btnSkin() != null) ? rec.btnSkin() : getDefaultSkin();
    this.args = rec.args();
    this.label = rec.label();

    String text = rec.text();
    String styleName = rec.styleName();

    this.btn =
        switch (rec.type()) {
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

    init(rec.trigger());
  }

  protected void init(String trigger) {
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

  /**
   * Default click behaviour: fire this clickable's configured trigger with its configured args on
   * its entity's event handler. Mirrors EnemyDropTargetComponent.fireTrigger's arity handling (0-3
   * args) so every Clickable variant gets working click dispatch for free, without each subclass
   * (InOutOnTrigger, DragNDrop, etc.) needing to know or hardcode what the trigger/args mean.
   * Subclasses may still override this if they need extra behaviour beyond firing the trigger.
   */
  protected void onClick() {
    switch (args.length) {
      case 0 -> entity.getEvents().trigger(trigger);
      case 1 -> entity.getEvents().trigger(trigger, args[0]);
      case 2 -> entity.getEvents().trigger(trigger, args[0], args[1]);
      case 3 -> entity.getEvents().trigger(trigger, args[0], args[1], args[2]);
      default ->
          Gdx.app.error(
              "Clickable",
              "Trigger '" + trigger + "' has " + args.length + " args; only 0-3 are supported.");
    }
  }

  /**
   * Detach this clickable's widget from the stage and release anything it registered elsewhere.
   * Called by {@link ClickableFactory} when a widget is removed at runtime (e.g. a played card
   * leaving the hand) or the factory is disposed. Subclasses that register drag sources or
   * listeners override this to also undo those.
   */
  public void remove() {
    btn.remove();
  }

  /**
   * Put this widget straight into its visible resting state, skipping any intro animation. Called
   * by {@link ClickableFactory} when a widget is added to an already-live UI (e.g. a card drawn to
   * replace one that was just played). Default: nothing, since the base widget is always drawn at
   * its position anyway.
   */
  public void showNow() {
    // no-op by default
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

  /**
   * Arguments this clickable was configured with (e.g. a card's damage amount). May be empty, never
   * null.
   */
  public Object[] getArgs() {
    return args;
  }

  /** Human-readable label for UI feedback. Defaults to the trigger name if not set. */
  public String getLabel() {
    return label;
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
