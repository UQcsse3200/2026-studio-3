package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the Main Menu and emits player selections as entity events. */
public class MainMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuDisplay.class);
  private static final float Z_INDEX = 2f;

  public static final String START_EVENT = "start";
  public static final String LOAD_EVENT = "load";
  public static final String BESTIARY_EVENT = "bestiary";
  public static final String SETTINGS_EVENT = "settings";
  public static final String EXIT_EVENT = "exit";

  private Stack rootStack;
  private Table menuTable;
  private TextButton newGameButton;
  private TextButton loadGameButton;
  private TextButton bestiaryButton;
  private TextButton settingsButton;
  private TextButton exitButton;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootStack =
        new Stack(); // Using stack instead of table to allow for background and overlay to be added
    // behind the content
    rootStack.setFillParent(true);

    Table background = new Table();
    background.setBackground(skin.newDrawable("white", MenuTheme.deepPlum()));
    rootStack.add(background);

    Color overlayColour = MenuTheme.earthBrown();
    overlayColour.a = 0.18f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable("white", overlayColour));
    rootStack.add(overlay);

    rootStack.add(buildContent());
    stage.addActor(rootStack);
  }

  private Table buildContent() {
    Label titleTop = new Label("THE FALL OF", skin, "title");
    titleTop.setColor(MenuTheme.warmParchment());
    titleTop.setFontScale(1.1f);

    Label titleBottom = new Label("THE PANTHEON", skin, "title");
    titleBottom.setColor(MenuTheme.warmParchment());
    titleBottom.setFontScale(1.45f);

    newGameButton = createButton("New Game", START_EVENT);
    loadGameButton = createButton("Load Game", LOAD_EVENT);
    bestiaryButton = createButton("Bestiary", BESTIARY_EVENT);
    settingsButton = createButton("Settings", SETTINGS_EVENT);
    exitButton = createButton("Exit", EXIT_EVENT);

    menuTable = new Table();
    menuTable.setName("main-menu-buttons");
    menuTable.defaults().width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
    addMenuButton(newGameButton);
    addMenuButton(loadGameButton);
    addMenuButton(bestiaryButton);
    addMenuButton(settingsButton);
    menuTable.add(exitButton);

    Color panelColour = MenuTheme.deepPlum();
    panelColour.a = 0.92f;
    Table panel = new Table();
    panel.setBackground(skin.newDrawable("window", panelColour));
    panel.pad(MenuTheme.PANEL_PADDING);
    panel.add(menuTable);

    Table content = new Table();
    content.setFillParent(true);
    content.center().pad(MenuTheme.SCREEN_PADDING);
    content.add(titleTop).padBottom(4f);
    content.row();
    content.add(titleBottom).padBottom(MenuTheme.TITLE_SPACING);
    content.row();
    content.add(panel);
    return content;
  }

  private void addMenuButton(TextButton button) {
    menuTable.add(button);
    menuTable.row().padTop(MenuTheme.BUTTON_SPACING);
  }

  private TextButton createButton(String text, String eventName) {
    TextButton button = new TextButton(text, MenuTheme.createButtonStyle(skin));
    button.setName(eventName);
    // Triggers an event when the button is pressed
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
}
