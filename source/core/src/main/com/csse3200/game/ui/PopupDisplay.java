package com.csse3200.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Generic reusable popup: a dimmed backdrop behind a centred, closable window.
 *
 * <p>Starts empty and hidden. Callers add their own content via {@link #getContentTable()} — a
 * label, a form, widgets built by a {@link
 * com.csse3200.game.components.spritedisplay.clickable.ClickableFactory}, anything — then call
 * {@link #show()} / {@link #hide()} to toggle it.
 *
 * <p>Look: by default the skin's {@code "default"} styles. Pass a style name (e.g. {@code "popup"})
 * to restyle the window and its close button; that name must exist in the skin as both a {@code
 * Window.WindowStyle} and a {@code TextButton.TextButtonStyle}.
 */
public class PopupDisplay extends UIComponent {
  private static final float Z_INDEX = 20f;
  private static final Color BACKDROP_COLOUR = new Color(0f, 0f, 0f, 0.6f); // around the popup

  private final String title;
  private final String styleName;
  private float minWidth = 0f;
  private float minHeight = 0f;

  private Image backdrop;
  private Window window;
  private Table content;

  private Runnable onShow;
  private Runnable onHide;
  private Runnable onWindowClicked;

  private static final float HEADER_HEIGHT = 35f; // title bar height = the window's top padding
  private static final float SIDE_PADDING = 20f;

  /**
   * @param title text in the window's title bar
   * @param styleName skin style used for the window and its close button (see class javadoc)
   */
  public PopupDisplay(String title, String styleName) {
    this.title = title;
    this.styleName = styleName;
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

    window = new Window(title, skin, styleName);
    window.pad(0f); // nothing between the window edge and the header
    window.padTop(HEADER_HEIGHT); // Window sizes the title table to this height
    window.top();

    TextButton closeButton = new TextButton("X", skin, styleName);
    closeButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            hide();
          }
        });

    window.getTitleTable().padLeft(SIDE_PADDING); // keep the title text off the edge
    window.getTitleTable().add(closeButton).size(28f).padRight(SIDE_PADDING).padTop(-4f);

    content = new Table();
    content.pad(0f, SIDE_PADDING, SIDE_PADDING, SIDE_PADDING); // top, left, bottom, right
    window.add(content).expand().fill().padTop(10f);

    // Window unconditionally toFront()s itself on every touch down inside it (baked into its
    // constructor's own captureListener, unrelated to setMovable) — which, for callers with
    // widgets that sit outside this window's actor hierarchy as stage siblings (e.g. a
    // ClickableFactory-driven grid), silently buries those widgets behind the window on the very
    // next click. This listener runs after that capture-phase toFront (touchDown here is a normal,
    // non-capture listener), so onWindowClicked is the hook such callers use to re-assert their own
    // stacking order in response.
    window.addListener(
        new ClickListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (onWindowClicked != null) {
              onWindowClicked.run();
            }
            return false;
          }
        });

    stage.addActor(backdrop);
    stage.addActor(window);
  }

  /** The table callers add their own widgets to. */
  public Table getContentTable() {
    return content;
  }

  /**
   * Registers a callback run once at the end of every {@link #show()}, after the window has been
   * packed and positioned — so its bounds ({@link #getWindowX()} etc.) are accurate. Used by
   * callers with widgets outside the window's actor hierarchy that need to align themselves against
   * it.
   */
  public void setOnShow(Runnable callback) {
    this.onShow = callback;
  }

  /** Registers a callback run once at the end of every {@link #hide()}. */
  public void setOnHide(Runnable callback) {
    this.onHide = callback;
  }

  /**
   * Registers a callback run after every click inside the window (after the window's own
   * z-order-changing listener has already run). See the comment in {@link #addActors()} for why
   * this exists.
   */
  public void setOnWindowClicked(Runnable callback) {
    this.onWindowClicked = callback;
  }

  public float getWindowX() {
    return window.getX();
  }

  public float getWindowY() {
    return window.getY();
  }

  public float getWindowHeight() {
    return window.getHeight();
  }

  /** Shows the popup, centred on the stage and in front of everything else. */
  public void show() {
    window.pack();
    window.setSize(Math.max(window.getWidth(), minWidth), Math.max(window.getHeight(), minHeight));
    window.setPosition(
        (stage.getWidth() - window.getWidth()) / 2f, (stage.getHeight() - window.getHeight()) / 2f);
    backdrop.setVisible(true);
    window.setVisible(true);
    backdrop.toFront();
    window.toFront();
    if (onShow != null) {
      onShow.run();
    }
  }

  public void hide() {
    backdrop.setVisible(false);
    window.setVisible(false);
    if (onHide != null) {
      onHide.run();
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Actors are drawn by the stage; nothing to do per-frame here.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  public void setHeaderColour(Color colour) {
    window.getTitleTable().setBackground(skin.newDrawable("white", colour));
  }
}
