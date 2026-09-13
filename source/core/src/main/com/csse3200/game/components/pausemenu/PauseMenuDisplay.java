package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ui component for the in-game pause menu. Shows a full-screen dimmed overlay with Resume,
 * Settings and Return to Main Menu buttons.
 *
 * <p>The component only owns the view and the button wiring. It fires events that a separate
 * actions component (owned by another team member) listens for; it does not change screens or game
 * state itself. Returning to the main menu is guarded by a confirmation dialog so a run is not
 * abandoned by a single click.
 */
public class PauseMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(PauseMenuDisplay.class);
  private static final float Z_INDEX = 2f;

  /** Fired when the player chooses to resume the game. */
  public static final String RESUME_EVENT = "resume";

  /** Fired when the player opens the settings screen from the pause menu. */
  public static final String SETTINGS_EVENT = "settings";

  /** Fired only after the player confirms leaving the current run. */
  public static final String EXIT_TO_MENU_EVENT = "exitToMenu";

  /**
   * Received (not fired by the display) to open the pause menu. Opening is idempotent, so once
   * paused, pressing Escape again does nothing; the menu is only closed via {@link #RESUME_EVENT}.
   */
  public static final String PAUSE_EVENT = "pause";

  private static final float BACKGROUND_OPACITY = 0.7f;

  private Table table;
  private Texture backgroundTexture;
  private Dialog confirmDialog;
  private TextButton resumeButton;
  private TextButton settingsButton;
  private TextButton returnButton;
  private TextButton confirmButton;
  private TextButton cancelButton;

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener(PAUSE_EVENT, this::showMenu);
    entity.getEvents().addListener(RESUME_EVENT, this::hideMenu);
  }

  private void addActors() {
    table = new Table();
    table.setFillParent(true);
    table.setBackground(new TextureRegionDrawable(new TextureRegion(createBackgroundTexture())));
    table.center();

    resumeButton = new TextButton("Resume", skin);
    settingsButton = new TextButton("Settings", skin);
    returnButton = new TextButton("Return to Main Menu", skin);

    resumeButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Resume button clicked");
            entity.getEvents().trigger(RESUME_EVENT);
          }
        });

    settingsButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Settings button clicked");
            entity.getEvents().trigger(SETTINGS_EVENT);
          }
        });

    returnButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Return to main menu clicked, asking for confirmation");
            confirmDialog.show(stage);
          }
        });

    buildConfirmDialog();

    table.add(resumeButton).padBottom(15f);
    table.row();
    table.add(settingsButton).padBottom(15f);
    table.row();
    table.add(returnButton);

    stage.addActor(table);
    table.setVisible(false);
  }

  /** Builds the "leave this run" confirmation dialog. It only fires the exit event on confirm. */
  private void buildConfirmDialog() {
    confirmDialog = new Dialog("", skin);
    confirmDialog.text("Leave this run? Progress may be lost");

    confirmButton = new TextButton("Confirm", skin);
    cancelButton = new TextButton("Cancel", skin);

    confirmButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Leaving run confirmed");
            entity.getEvents().trigger(EXIT_TO_MENU_EVENT);
            confirmDialog.hide(null);
          }
        });

    cancelButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Leaving run cancelled");
            confirmDialog.hide(null);
          }
        });

    confirmDialog.getButtonTable().add(confirmButton).pad(10f);
    confirmDialog.getButtonTable().add(cancelButton).pad(10f);
  }

  /** Creates a 1x1 dimmed texture used as the full-screen overlay background. */
  private Texture createBackgroundTexture() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, BACKGROUND_OPACITY);
    pixmap.fill();
    backgroundTexture = new Texture(pixmap);
    pixmap.dispose();
    return backgroundTexture;
  }

  /**
   * Shows the menu and lifts it above the rest of the screen's UI, so the dimmed background covers
   * everything and only the menu's own buttons stay visible and clickable. Idempotent: showing an
   * already-visible menu is a no-op, which is what stops a second Escape press from closing it.
   */
  private void showMenu() {
    table.setVisible(true);
    table.toFront();
  }

  /** Hides the menu (and any open confirmation dialog). Triggered by Resume. */
  private void hideMenu() {
    if (confirmDialog != null) {
      confirmDialog.hide(null);
    }
    table.setVisible(false);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    if (confirmDialog != null) {
      confirmDialog.remove();
    }
    if (table != null) {
      table.clear();
    }
    if (backgroundTexture != null) {
      backgroundTexture.dispose();
    }
    super.dispose();
  }

  // --- Visible for testing -------------------------------------------------

  TextButton getResumeButton() {
    return resumeButton;
  }

  TextButton getSettingsButton() {
    return settingsButton;
  }

  TextButton getReturnButton() {
    return returnButton;
  }

  TextButton getConfirmButton() {
    return confirmButton;
  }

  TextButton getCancelButton() {
    return cancelButton;
  }

  Dialog getConfirmDialog() {
    return confirmDialog;
  }

  boolean isMenuVisible() {
    return table.isVisible();
  }
}
