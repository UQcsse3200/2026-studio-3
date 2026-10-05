package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;

/** Standalone settings-screen host for the reusable {@link SettingsPanel}. */
public class SettingsMenuDisplay extends UIComponent {
  private final GdxGame game;
  private Stack rootStack;
  private SettingsPanel settingsPanel;

  public SettingsMenuDisplay(GdxGame game) {
    this.game = game;
  }

  @Override
  public void create() {
    super.create();
    rootStack = new Stack();
    rootStack.setFillParent(true);

    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null
        && resources.containsAsset(MainMenuDisplay.BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(MainMenuDisplay.BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      Image background = new Image(texture);
      background.setScaling(Scaling.fill);
      rootStack.add(background);
    }

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.62f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable("white", overlayColour));
    rootStack.add(overlay);

    settingsPanel = new SettingsPanel(skin, () -> game.setScreen(GdxGame.ScreenType.MAIN_MENU));
    Table wrapper = new Table();
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);
    wrapper.add(settingsPanel).width(760f);
    rootStack.add(wrapper);

    stage.addActor(rootStack);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public void dispose() {
    if (rootStack != null) {
      rootStack.remove();
      rootStack.clear();
    }
    super.dispose();
  }

  SettingsPanel getSettingsPanel() {
    return settingsPanel;
  }
}
