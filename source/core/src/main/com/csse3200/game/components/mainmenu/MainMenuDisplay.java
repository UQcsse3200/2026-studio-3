package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.debug.DemoEncounterAccess;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the Main Menu and emits player selections as entity events. */
public class MainMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuDisplay.class);
  private static final float Z_INDEX = 2f;
  private static final float TITLE_WIDTH = 510f;
  private static final float TITLE_HEIGHT = 170f;

  public static final String BACKGROUND_TEXTURE = "images/main_menu_background.png";
  public static final String BUTTON_FRAME_TEXTURE = "images/main_menu_button_frame.png";
  public static final String TITLE_LOGO_TEXTURE = "images/main_menu_title_logo.png";

  public static final String START_EVENT = "start";
  public static final String LOAD_EVENT = "load";
  public static final String BESTIARY_EVENT = "bestiary";
  public static final String SETTINGS_EVENT = "settings";
  public static final String EXIT_EVENT = "exit";
  public static final String DEMO_SHOP_EVENT = "demoShop";
  public static final String DEMO_EVENT_EVENT = "demoEvent";

  private Stack rootStack;
  private Table menuTable;
  private TextButton newGameButton;
  private TextButton loadGameButton;
  private TextButton bestiaryButton;
  private TextButton settingsButton;
  private TextButton exitButton;
  private TextButton demoShopButton;
  private TextButton demoEventButton;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);

    Texture backgroundTexture = getTexture(BACKGROUND_TEXTURE);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    rootStack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.22f; // Set the alpha value to 0.22 for 22% opacity
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable("white", overlayColour));
    rootStack.add(overlay);

    Texture buttonFrameTexture = buttonFrameTexture();
    rootStack.add(buildContent(buttonFrameTexture));
    if (DemoEncounterAccess.ENABLED) {
      rootStack.add(buildDemoPanel(buttonFrameTexture));
    }
    stage.addActor(rootStack);
  }

  private Table buildContent(Texture buttonFrameTexture) {
    Texture titleTexture = getTexture(TITLE_LOGO_TEXTURE);
    titleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    // Set the filter to Nearest to avoid blurring the pixel art
    Image titleLogo = new Image(titleTexture);
    titleLogo.setScaling(Scaling.fit);

    // Build the buttons and add them to the menu table
    newGameButton = createButton("New Game", START_EVENT, buttonFrameTexture);
    loadGameButton = createButton("Load Game", LOAD_EVENT, buttonFrameTexture);
    bestiaryButton = createButton("Library", BESTIARY_EVENT, buttonFrameTexture);
    settingsButton = createButton("Settings", SETTINGS_EVENT, buttonFrameTexture);
    exitButton = createButton("Exit", EXIT_EVENT, buttonFrameTexture);

    // Build the menu table and add the buttons to it
    menuTable = new Table();
    menuTable.setName("main-menu-buttons");
    menuTable.defaults().width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
    addMenuButton(newGameButton);
    addMenuButton(loadGameButton);
    addMenuButton(bestiaryButton);
    addMenuButton(settingsButton);
    menuTable.add(exitButton);

    Table content = new Table();
    content.setFillParent(true);
    content.center().pad(MenuTheme.SCREEN_PADDING);
    content
        .add(titleLogo)
        .width(TITLE_WIDTH)
        .height(TITLE_HEIGHT)
        .padBottom(MenuTheme.TITLE_SPACING);
    content.row();
    content.add(menuTable);
    return content;
  }

  /** Adds temporary review shortcuts without changing the layout of the real main menu. */
  private Table buildDemoPanel(Texture buttonFrameTexture) {
    demoShopButton = createButton("Demo Shop", DEMO_SHOP_EVENT, buttonFrameTexture);
    demoEventButton = createButton("Demo Event", DEMO_EVENT_EVENT, buttonFrameTexture);
    demoShopButton.getLabel().setFontScale(0.62f);
    demoEventButton.getLabel().setFontScale(0.62f);

    Table buttons = new Table();
    buttons.defaults().width(220f).height(68f);
    buttons.add(demoShopButton);
    buttons.row();
    buttons.add(demoEventButton).padTop(8f);

    Table panel = new Table();
    panel.setFillParent(true);
    panel.top().right().pad(18f);
    panel.add(buttons);
    return panel;
  }

  private Texture buttonFrameTexture() {
    Texture texture = getTexture(BUTTON_FRAME_TEXTURE);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return texture;
  }

  private void addMenuButton(TextButton button) {
    menuTable.add(button);
    menuTable.row().padTop(MenuTheme.BUTTON_SPACING);
  }

  private TextButton createButton(String text, String eventName, Texture buttonFrameTexture) {
    TextButton button = new TextButton(text, MenuTheme.createButtonStyle(skin, buttonFrameTexture));
    button.setName(eventName);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("{} button clicked", text);
            entity.getEvents().trigger(eventName);
          }
        });
    return button;
  }

  private Texture getTexture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    if (rootStack != null) {
      rootStack.remove();
      rootStack.clear();
    }
    super.dispose();
  }

  Stack getRootStack() {
    return rootStack;
  }

  Table getMenuTable() {
    return menuTable;
  }

  List<TextButton> getMenuButtons() {
    return List.of(newGameButton, loadGameButton, bestiaryButton, settingsButton, exitButton);
  }

  List<TextButton> getDemoButtons() {
    if (!DemoEncounterAccess.ENABLED) {
      return List.of();
    }
    return List.of(demoShopButton, demoEventButton);
  }
}
