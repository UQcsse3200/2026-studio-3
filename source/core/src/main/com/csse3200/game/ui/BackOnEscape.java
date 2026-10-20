package com.csse3200.game.ui;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lets Escape press the Back button on the menu screens, so the player can leave the settings, the
 * save screen and the library screens from the keyboard. It looks for a visible button labelled
 * "Back", so the screens don't need to hand their buttons over.
 *
 * <p>Anything on the screen that uses Escape itself, like an open dropdown, gets the key first.
 */
public final class BackOnEscape {
  private static final String BACK = "Back";
  private static final Set<ScreenType> SCREENS =
      EnumSet.of(
          ScreenType.SETTINGS,
          ScreenType.SAVE_LOAD,
          ScreenType.LIBRARY,
          ScreenType.CARD_LIBRARY,
          ScreenType.ITEM_LIBRARY,
          ScreenType.BESTIARY);

  private BackOnEscape() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Called by GdxGame once the new screen has been built, since the key listener goes on its stage.
   * Screens without a Back button are left alone.
   *
   * @param screen the screen that just opened
   */
  public static void attach(ScreenType screen) {
    RenderService render = ServiceLocator.getRenderService();
    if (!SCREENS.contains(screen) || render == null || render.getStage() == null) {
      return;
    }
    Stage stage = render.getStage();
    stage.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            if (keycode != Keys.ESCAPE || event.isHandled()) {
              return false;
            }
            TextButton back = findBack(stage.getRoot());
            if (back == null) {
              return false;
            }
            back.fire(new ChangeEvent());
            return true;
          }
        });
  }

  /** Finds the first visible Back button that can be pressed, or null if there isn't one. */
  private static TextButton findBack(Group group) {
    for (Actor child : group.getChildren()) {
      if (!child.isVisible()) {
        continue;
      }
      if (child instanceof TextButton button
          && !button.isDisabled()
          && BACK.contentEquals(button.getText())) {
        return button;
      }
      if (child instanceof Group inner) {
        TextButton found = findBack(inner);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
