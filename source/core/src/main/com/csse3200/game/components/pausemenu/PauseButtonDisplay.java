package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small always-visible "Pause" button shown during gameplay (top-left). Clicking it opens the
 * pause menu by firing {@link PauseMenuDisplay#PAUSE_EVENT} — the same event Escape fires — so the
 * menu is reachable with the mouse, not just the keyboard. It hides itself while the pause menu is
 * open and reappears on resume.
 */
public class PauseButtonDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(PauseButtonDisplay.class);
  private static final float Z_INDEX = 2f;
  private static final float BUTTON_WIDTH = 170f;
  private static final float BUTTON_HEIGHT = 60f;
  // The frame uses the large menu font; scale the label down so short labels fit this smaller
  // button.
  private static final float LABEL_SCALE = 0.6f;
  private static final float DEFAULT_EDGE_PAD = 12f;

  private final int align;
  private final float edgePad;
  private Table table;
  private TextButton pauseButton;

  /** Creates the pause button in the top-right corner. */
  public PauseButtonDisplay() {
    this(Align.topRight, DEFAULT_EDGE_PAD);
  }

  /**
   * @param align which corner to pin the button to (an {@link Align} constant, e.g. {@code
   *     Align.topRight}). Lets each screen place it clear of its own HUD.
   */
  public PauseButtonDisplay(int align) {
    this(align, DEFAULT_EDGE_PAD);
  }

  /**
   * @param align corner to pin to (an {@link Align} constant)
   * @param edgePad inset from the screen edges, for nudging the button clear of nearby HUD
   */
  public PauseButtonDisplay(int align, float edgePad) {
    this.align = align;
    this.edgePad = edgePad;
  }

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener(PauseMenuDisplay.PAUSE_EVENT, this::hide);
    entity.getEvents().addListener(PauseMenuDisplay.RESUME_EVENT, this::show);
  }

  private void addActors() {
    table = new Table();
    table.setFillParent(true);
    if ((align & Align.bottom) != 0) {
      table.bottom();
    } else {
      table.top();
    }
    if ((align & Align.right) != 0) {
      table.right();
    } else {
      table.left();
    }
    // Only the button captures clicks; the rest of the (full-screen) table lets gameplay through.
    table.setTouchable(Touchable.childrenOnly);

    pauseButton = new TextButton("Pause", buttonStyle());
    pauseButton.getLabel().setFontScale(LABEL_SCALE);
    pauseButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("On-screen pause button clicked");
            entity.getEvents().trigger(PauseMenuDisplay.PAUSE_EVENT);
          }
        });

    table.add(pauseButton).width(BUTTON_WIDTH).height(BUTTON_HEIGHT).pad(edgePad);
    stage.addActor(table);
  }

  /**
   * Main-menu styled button when the frame texture is loaded; plain skin otherwise (e.g. tests).
   */
  private TextButtonStyle buttonStyle() {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null
        && resources.containsAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class)) {
      Texture frame = resources.getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class);
      frame.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      return MenuTheme.createButtonStyle(skin, frame);
    }
    return new TextButtonStyle(skin.get(TextButtonStyle.class));
  }

  private void show() {
    table.setVisible(true);
  }

  private void hide() {
    table.setVisible(false);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // drawn by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    if (table != null) {
      table.clear();
    }
    super.dispose();
  }

  // --- Visible for testing -------------------------------------------------

  TextButton getPauseButton() {
    return pauseButton;
  }

  boolean isButtonVisible() {
    return table.isVisible();
  }
}
