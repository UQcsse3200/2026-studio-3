package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ui component for the in-game pause menu. Shows a full-screen dimmed overlay (styled to match
 * the main menu) with Resume, Save & Load, Settings and Return to Main Menu buttons.
 *
 * <p>The component only owns the view and the button wiring. It fires events that separate
 * components listen for, it does not change screens or game state itself. The Save/Load and
 * Settings subviews are shown in place (no screen switch) so the current run is never lost.
 * Returning to the main menu is guarded by a confirmation dialog.
 */
public class PauseMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(PauseMenuDisplay.class);
  private static final float Z_INDEX = 2f;

  /** Fired when the player chooses to resume the game. */
  public static final String RESUME_EVENT = "resume";

  /** Fired when the player opens the save/load panel from the pause menu. */
  public static final String SAVE_LOAD_EVENT = "saveLoad";

  /** Fired when the player opens the settings view from the pause menu. */
  public static final String SETTINGS_EVENT = "settings";

  /** Fired only after the player confirms leaving the current run. */
  public static final String EXIT_TO_MENU_EVENT = "exitToMenu";

  /**
   * Received (not fired by the display) to open the pause menu. Opening is idempotent, so once
   * paused, pressing Escape again does nothing; the menu is only closed via {@link #RESUME_EVENT}.
   */
  public static final String PAUSE_EVENT = "pause";

  private static final float OVERLAY_OPACITY = 0.7f;

  private Table table;
  private Table menuTable;
  private Table settingsTable;
  private Dialog confirmDialog;
  private TextButton resumeButton;
  private TextButton saveLoadButton;
  private TextButton settingsButton;
  private TextButton returnButton;
  private TextButton confirmButton;
  private TextButton cancelButton;
  private TextButton settingsBackButton;

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
    // Deep-plum dim (matches the main menu palette) that leaves the game faintly visible behind.
    Color dim = MenuTheme.deepPlum();
    dim.a = OVERLAY_OPACITY;
    table.setBackground(skin.newDrawable("white", dim));
    // Catch clicks/scroll so they don't fall through to the gameplay UI behind the overlay.
    table.setTouchable(Touchable.enabled);
    table.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            return table.isVisible();
          }

          @Override
          public boolean scrolled(
              InputEvent event, float x, float y, float amountX, float amountY) {
            return table.isVisible();
          }
        });
    table.center();

    resumeButton = menuButton("Resume", RESUME_EVENT);
    saveLoadButton = menuButton("Save & Load", SAVE_LOAD_EVENT);
    settingsButton = menuButton("Settings", SETTINGS_EVENT);
    returnButton = menuButton("Main Menu", null); // opens the confirm dialog instead

    settingsButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            showSettings();
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
    buildMenuTable();

    table.add(menuTable);

    stage.addActor(table);
    table.setVisible(false);
  }

  private void buildMenuTable() {
    menuTable = new Table();
    menuTable.add(titleLabel("Paused")).padBottom(MenuTheme.TITLE_SPACING).row();
    addMenuRow(resumeButton);
    addMenuRow(saveLoadButton);
    addMenuRow(settingsButton);
    menuTable.add(returnButton).width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
  }

  private void addMenuRow(TextButton button) {
    menuTable.add(button).width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
    menuTable.row().padTop(MenuTheme.BUTTON_SPACING);
  }

  private Table buildSettingsTable() {
    Table root = new Table();
    root.add(titleLabel("Settings")).padBottom(25f).row();
    settingsBackButton = menuButton("Back", null);
    settingsBackButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Pause settings back button clicked");
            showPauseButtons();
          }
        });
    root.add(settingsBackButton).width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
    return root;
  }

  /** Builds the "leave this run" confirmation dialog. It only fires the exit event on confirm. */
  private void buildConfirmDialog() {
    Window.WindowStyle windowStyle = new Window.WindowStyle(skin.get(Window.WindowStyle.class));
    Color panel = MenuTheme.deepPlum();
    panel.a = 0.96f;
    windowStyle.background = skin.newDrawable("white", panel);
    confirmDialog = new Dialog("", windowStyle);

    Label.LabelStyle messageStyle =
        new Label.LabelStyle(skin.getFont("font_large"), MenuTheme.warmParchment());
    confirmDialog
        .getContentTable()
        .add(new Label("Leave this run? Progress may be lost", messageStyle))
        .pad(24f);

    confirmButton = menuButton("Confirm", null);
    cancelButton = menuButton("Cancel", null);

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

    confirmDialog
        .getButtonTable()
        .add(confirmButton)
        .width(MenuTheme.BUTTON_WIDTH)
        .height(MenuTheme.BUTTON_HEIGHT)
        .pad(10f);
    confirmDialog
        .getButtonTable()
        .add(cancelButton)
        .width(MenuTheme.BUTTON_WIDTH)
        .height(MenuTheme.BUTTON_HEIGHT)
        .pad(10f);
  }

  /**
   * Creates a button styled like the main menu (pixel-art frame + palette). Falls back to the
   * default skin style when the frame texture isn't loaded (e.g. in unit tests). When {@code
   * eventName} is non-null, clicking the button triggers that event on the entity.
   */
  private TextButton menuButton(String text, String eventName) {
    TextButton button = new TextButton(text, buttonStyle());
    if (eventName != null) {
      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
              logger.debug("{} button clicked", text);
              entity.getEvents().trigger(eventName);
            }
          });
    }
    return button;
  }

  private TextButtonStyle buttonStyle() {
    Texture frame = loadedButtonFrame();
    return frame != null
        ? MenuTheme.createButtonStyle(skin, frame)
        : new TextButtonStyle(skin.get(TextButtonStyle.class));
  }

  private Label titleLabel(String text) {
    return new Label(
        text, new Label.LabelStyle(skin.getFont("font_large"), MenuTheme.warmParchment()));
  }

  /** Returns the main-menu button frame texture if it has been loaded, otherwise null. */
  private Texture loadedButtonFrame() {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null
        && resources.containsAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class)) {
      Texture frame = resources.getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class);
      frame.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      return frame;
    }
    return null;
  }

  /**
   * Shows the menu and lifts it above the rest of the screen's UI, so the dimmed background covers
   * everything and only the menu's own buttons stay visible and clickable. Idempotent: showing an
   * already-visible menu is a no-op, which is what stops a second Escape press from closing it.
   */
  private void showMenu() {
    showPauseButtons();
    table.setVisible(true);
    table.toFront();
  }

  private void showSettings() {
    settingsTable = buildSettingsTable();
    table.clearChildren();
    table.add(settingsTable);
  }

  private void showPauseButtons() {
    table.clearChildren();
    table.add(menuTable);
    settingsTable = null;
  }

  /** Hides the menu (and any open confirmation dialog). Triggered by Resume. */
  private void hideMenu() {
    if (confirmDialog != null) {
      confirmDialog.hide(null);
    }
    showPauseButtons();
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
    super.dispose();
  }

  // --- Visible for testing -------------------------------------------------

  TextButton getResumeButton() {
    return resumeButton;
  }

  TextButton getSaveLoadButton() {
    return saveLoadButton;
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

  boolean isSettingsVisible() {
    return settingsTable != null;
  }

  TextButton getSettingsBackButton() {
    return settingsBackButton;
  }

  Table getRootTable() {
    return table;
  }
}
