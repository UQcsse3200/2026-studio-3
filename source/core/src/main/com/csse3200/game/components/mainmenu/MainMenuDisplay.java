package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.PixelButtonStyles;
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
  public static final String ENTER_TUTORIAL_EVENT = "enterTutorial";
  public static final String LOAD_EVENT = "load";
  public static final String BESTIARY_EVENT = "bestiary";
  public static final String SETTINGS_EVENT = "settings";
  public static final String EXIT_EVENT = "exit";
  public static final String DEMO_EVENT = "demoEvent";
  public static final String DEMO_CAMPFIRE_EVENT = "demoCampfire";

  private Stack rootStack;
  private Table menuTable;
  private TextButton newGameButton;
  private TextButton loadGameButton;
  private TextButton bestiaryButton;
  private TextButton settingsButton;
  private TextButton exitButton;
  private Dialog tutorialChoiceDialog;

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
    background.setScaling(Scaling.stretch);
    rootStack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.22f; // Set the alpha value to 0.22 for 22% opacity
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable("white", overlayColour));
    rootStack.add(overlay);

    Texture buttonFrameTexture = getTexture(BUTTON_FRAME_TEXTURE);
    buttonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    rootStack.add(buildContent(buttonFrameTexture));
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
            if (START_EVENT.equals(eventName)) {
              showTutorialChoice();
            } else {
              entity.getEvents().trigger(eventName);
            }
          }
        });
    button.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1 && !button.isDisabled()) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
          }
        });
    return button;
  }

  /** Shows the choice between the battle tutorial and a normal new run. */
  private void showTutorialChoice() {
    if (tutorialChoiceDialog != null) {
      return;
    }

    tutorialChoiceDialog =
        new Dialog("Battle Tutorial", skin) {
          @Override
          protected void result(Object enterTutorial) {
            if (tutorialChoiceDialog == null) {
              return;
            }

            remove();
            tutorialChoiceDialog = null;
            entity
                .getEvents()
                .trigger(Boolean.TRUE.equals(enterTutorial) ? ENTER_TUTORIAL_EVENT : START_EVENT);
          }
        };

    tutorialChoiceDialog.setName("tutorial-choice");
    tutorialChoiceDialog.setModal(true);
    tutorialChoiceDialog.setMovable(false);
    tutorialChoiceDialog.getContentTable().pad(20f);
    Label prompt = new Label("Would you like to start the battle tutorial?", skin);
    tutorialChoiceDialog.getContentTable().add(prompt);
    tutorialChoiceDialog.getButtonTable().defaults().width(220f).height(60f).pad(10f);

    TextButton enterButton = new TextButton("Enter Tutorial", skin);
    enterButton.setName(ENTER_TUTORIAL_EVENT);
    tutorialChoiceDialog.button(enterButton, Boolean.TRUE);

    TextButton skipButton = new TextButton("Skip Tutorial", skin);
    skipButton.setName("skipTutorial");
    tutorialChoiceDialog.button(skipButton, Boolean.FALSE);

    ResourceService resources = ServiceLocator.getResourceService();
    boolean styledTutorialDialog =
        resources != null
            && resources.containsAsset("images/tutorial_choice_frame.png", Texture.class)
            && resources.containsAsset("images/ancient_temple_choice_button.png", Texture.class);
    if (styledTutorialDialog) {
      Texture frameTexture = getTexture("images/tutorial_choice_frame.png");
      frameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      TextureRegionDrawable frame = new TextureRegionDrawable(new TextureRegion(frameTexture));
      frame.setMinWidth(0f);
      frame.setMinHeight(0f);

      WindowStyle windowStyle = new WindowStyle(tutorialChoiceDialog.getStyle());
      windowStyle.background = frame;
      windowStyle.stageBackground = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.55f));
      tutorialChoiceDialog.setStyle(windowStyle);
      tutorialChoiceDialog.pad(104f, 64f, 64f, 64f);

      Label titleLabel = tutorialChoiceDialog.getTitleLabel();
      LabelStyle titleStyle = new LabelStyle(skin.get("large", LabelStyle.class));
      titleStyle.fontColor = Color.valueOf("EFC26C");
      titleLabel.setStyle(titleStyle);
      titleLabel.setAlignment(Align.center);
      tutorialChoiceDialog.getTitleTable().clear();
      tutorialChoiceDialog.getContentTable().clear();
      tutorialChoiceDialog.getContentTable().pad(0f).top();
      tutorialChoiceDialog.getContentTable().add(titleLabel).growX().height(30f).padBottom(20f);
      tutorialChoiceDialog.getContentTable().row();

      LabelStyle promptStyle = new LabelStyle(skin.get(LabelStyle.class));
      promptStyle.fontColor = Color.valueOf("E8DCC4");
      prompt.setStyle(promptStyle);
      prompt.setWrap(true);
      prompt.setAlignment(Align.center);
      tutorialChoiceDialog.getContentTable().add(prompt).growX();

      Texture buttonTexture = getTexture("images/ancient_temple_choice_button.png");
      enterButton.setStyle(PixelButtonStyles.create(skin, buttonTexture));
      skipButton.setStyle(PixelButtonStyles.create(skin, buttonTexture));
      tutorialChoiceDialog
          .getButtonTable()
          .getCell(enterButton)
          .width(260f)
          .height(60f)
          .pad(0f)
          .space(0f)
          .padRight(18f);
      tutorialChoiceDialog
          .getButtonTable()
          .getCell(skipButton)
          .width(260f)
          .height(60f)
          .pad(0f)
          .space(0f)
          .padLeft(18f);
      tutorialChoiceDialog.getButtonTable().padTop(24f);
    }

    tutorialChoiceDialog.show(stage);
    if (styledTutorialDialog) {
      tutorialChoiceDialog.setSize(800f, 349f);
      tutorialChoiceDialog.setPosition(
          (stage.getWidth() - 800f) / 2f, (stage.getHeight() - 349f) / 2f);
      tutorialChoiceDialog.validate();
    }
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
    if (tutorialChoiceDialog != null) {
      tutorialChoiceDialog.remove();
      tutorialChoiceDialog = null;
    }
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
