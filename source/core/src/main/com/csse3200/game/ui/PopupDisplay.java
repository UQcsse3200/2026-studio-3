package com.csse3200.game.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.services.ServiceLocator;

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
  private final String styleName;
  private float minWidth = 0f;
  private float minHeight = 0f;
  private String backgroundTexturePath;
  private Color backgroundColour;
  private float paddingTop = 20f;
  private float paddingLeft = 20f;
  private float paddingBottom = 20f;
  private float paddingRight = 20f;
  private boolean defaultCloseButtonVisible = true;
  private Color titleColour;
  private String titleFontName;

  private Image backdrop;
  private Window window;
  private Table content;
  private TextButton closeButton;

  private Runnable onShow;
  private Runnable onHide;
  private Runnable onWindowClicked;

  public PopupDisplay() {
    this("");
  }

  public PopupDisplay(String title) {
    this(title, "default");
  }

  /** Creates a popup using matching Window and TextButton styles from the shared skin. */
  public PopupDisplay(String title, String styleName) {
    this.title = title;
    this.styleName = styleName;
  }

  /** Sets a floor on the window's size — it will still grow beyond this to fit its content. */
  public void setMinSize(float minWidth, float minHeight) {
    this.minWidth = minWidth;
    this.minHeight = minHeight;
  }

  /**
   * Overrides this popup's window background colour, letting callers match their content's visual
   * theme instead of using the skin's default window style.
   *
   * @param colour solid background colour for the window
   */
  public void setBackgroundColour(Color colour) {
    backgroundColour = colour.cpy();
    backgroundTexturePath = null;
    if (window != null) {
      applyBackgroundColour();
    }
  }

  /** Uses a loaded texture as this popup's complete window background. */
  public void setBackgroundTexture(String texturePath) {
    backgroundTexturePath = texturePath;
    backgroundColour = null;
    if (window != null) {
      applyBackgroundTexture();
    }
  }

  private void applyBackgroundTexture() {
    Texture texture =
        ServiceLocator.getResourceService().getAsset(backgroundTexturePath, Texture.class);
    TextureRegionDrawable background = new TextureRegionDrawable(texture);
    background.setMinWidth(0f);
    background.setMinHeight(0f);
    window.setBackground(background);
  }

  private void applyBackgroundColour() {
    window.setBackground(skin.newDrawable("white", backgroundColour));
  }

  /** Sets the inset between the window frame and its title/content. */
  public void setPadding(float top, float left, float bottom, float right) {
    paddingTop = top;
    paddingLeft = left;
    paddingBottom = bottom;
    paddingRight = right;
    if (window != null) {
      window.pad(top, left, bottom, right);
    }
  }

  /** Shows or hides the generic title-bar close button. */
  public void setDefaultCloseButtonVisible(boolean visible) {
    defaultCloseButtonVisible = visible;
    if (closeButton != null) {
      closeButton.setVisible(visible);
    }
  }

  /** Applies a colour and skin font to the popup title without changing other popups. */
  public void setTitleStyle(Color colour, String fontName) {
    titleColour = colour.cpy();
    titleFontName = fontName;
    if (window != null) {
      applyTitleStyle();
    }
  }

  private void applyTitleStyle() {
    LabelStyle style = new LabelStyle(window.getTitleLabel().getStyle());
    style.font = skin.getFont(titleFontName);
    style.fontColor = titleColour;
    window.getTitleLabel().setStyle(style);
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
    window.pad(20f);
    window.top();

    closeButton = new TextButton("X", skin, styleName);
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

    applyPendingConfiguration();

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

    window.addListener(
        new com.badlogic.gdx.scenes.scene2d.InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
              hide();
              return true;
            }
            return false;
          }
        });

    stage.addActor(backdrop);
    stage.addActor(window);
  }

  private void applyPendingConfiguration() {
    window.pad(paddingTop, paddingLeft, paddingBottom, paddingRight);
    closeButton.setVisible(defaultCloseButtonVisible);

    if (backgroundTexturePath != null) {
      applyBackgroundTexture();
    } else if (backgroundColour != null) {
      applyBackgroundColour();
    }

    if (titleColour != null && titleFontName != null) {
      applyTitleStyle();
    }
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

  public float getWindowWidth() {
    return window.getWidth();
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
    stage.setKeyboardFocus(window);
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

  public boolean isShowing() {
    return window != null && window.isVisible();
  }

  public void setHeaderColour(Color colour) {
    if (window != null) {
      window.getTitleTable().setBackground(skin.newDrawable("white", colour));
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
}
