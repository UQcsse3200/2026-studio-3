package com.csse3200.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Generic reusable popup: a dimmed backdrop behind a centred, closable window.
 *
 * <p>Starts empty and hidden. Callers add their own content via {@link #getContentTable()} — a
 * label, a form, widgets built by a {@link
 * com.csse3200.game.components.spritedisplay.clickable.ClickableFactory}, anything — then call
 * {@link #show()} / {@link #hide()} to toggle it.
 */
public class PopupDisplay extends UIComponent {
  private static final float Z_INDEX = 20f;
  private static final Color BACKDROP_COLOUR = new Color(0f, 0f, 0f, 0.6f);

  private final String title;
  private float minWidth = 0f;
  private float minHeight = 0f;

  private Image backdrop;
  private Window window;
  private Table content;

  public PopupDisplay() {
    this("");
  }

  public PopupDisplay(String title) {
    this.title = title;
  }

  /** Sets a floor on the window's size — it will still grow beyond this to fit its content. */
  public void setMinSize(float minWidth, float minHeight) {
    this.minWidth = minWidth;
    this.minHeight = minHeight;
  }

  @Override
  public void create() {
    super.create();
    addActors();
    hide();
  }

  private void addActors() {
    backdrop = new Image(skin.newDrawable("white", BACKDROP_COLOUR));
    backdrop.setFillParent(true);

    window = new Window(title, skin);
    window.pad(20f);
    window.top();

    TextButton closeButton = new TextButton("X", skin);
    closeButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            hide();
          }
        });
    window.getTitleTable().add(closeButton).size(28f).padRight(4f).padTop(-4f);

    content = new Table();
    window.add(content).expand().fill().padTop(10f);

    stage.addActor(backdrop);
    stage.addActor(window);
  }

  /** The table callers add their own widgets to. */
  public Table getContentTable() {
    return content;
  }

  /** Shows the popup, centred on the stage and in front of everything else. */
  public void show() {
    window.pack();
    window.setSize(Math.max(window.getWidth(), minWidth), Math.max(window.getHeight(), minHeight));
    window.setPosition(
        (stage.getWidth() - window.getWidth()) / 2f,
        (stage.getHeight() - window.getHeight()) / 2f);
    backdrop.setVisible(true);
    window.setVisible(true);
    backdrop.toFront();
    window.toFront();
  }

  public void hide() {
    backdrop.setVisible(false);
    window.setVisible(false);
  }

  public boolean isShowing() {
    return window.isVisible();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage; nothing to do per-frame here.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }
}
