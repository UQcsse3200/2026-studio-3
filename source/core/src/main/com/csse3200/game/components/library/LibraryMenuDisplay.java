package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the library category chooser. */
public class LibraryMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(LibraryMenuDisplay.class);
  private final GdxGame game;

  private Stack rootStack;
  private Table rootTable;

  public LibraryMenuDisplay(GdxGame game) {
    this.game = game;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);

    Texture backgroundTexture =
        ServiceLocator.getResourceService()
            .getAsset(MainMenuDisplay.BACKGROUND_TEXTURE, Texture.class);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    rootStack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.45f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable("white", overlayColour));
    rootStack.add(overlay);

    Texture buttonFrameTexture =
        ServiceLocator.getResourceService()
            .getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class);
    buttonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.center().pad(MenuTheme.SCREEN_PADDING);

    Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("title", Label.LabelStyle.class));
    titleStyle.fontColor = MenuTheme.warmParchment();
    Label title = new Label("Library", titleStyle);

    TextButton.TextButtonStyle buttonStyle = MenuTheme.createButtonStyle(skin, buttonFrameTexture);
    TextButton cardLibraryButton = new TextButton("Card Library", buttonStyle);
    TextButton enemyLibraryButton = new TextButton("Enemy Library", buttonStyle);
    TextButton backButton = new TextButton("Back", buttonStyle);

    cardLibraryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Card Library button clicked");
            game.setScreen(GdxGame.ScreenType.CARD_LIBRARY);
          }
        });

    enemyLibraryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Enemy Library button clicked");
            game.setScreen(GdxGame.ScreenType.BESTIARY);
          }
        });

    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    rootTable.add(title).padBottom(12f).row();
    rootTable.defaults().width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
    rootTable.add(cardLibraryButton).row();
    rootTable.add(enemyLibraryButton).row();
    rootTable.add(backButton);

    rootStack.add(rootTable);
    stage.addActor(rootStack);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Rendering handled by the stage.
  }

  @Override
  public void update() {
    stage.act(ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  public void dispose() {
    if (rootStack != null) {
      rootStack.remove();
      rootStack.clear();
    }
    super.dispose();
  }
}
