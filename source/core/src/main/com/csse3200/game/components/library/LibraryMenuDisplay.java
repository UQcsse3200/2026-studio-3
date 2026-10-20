package com.csse3200.game.components.library;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the library category chooser. */
public class LibraryMenuDisplay extends UIComponent {
  public static final String CARD_BUTTON_TEXTURE = "images/library_menu_card_button_frame.png";
  public static final String ENEMY_BUTTON_TEXTURE = "images/library_menu_enemy_button_frame.png";
  public static final String ITEM_BUTTON_TEXTURE = "images/library_menu_item_button_frame.png";
  private static final Logger logger = LoggerFactory.getLogger(LibraryMenuDisplay.class);
  private final GdxGame game;

  private Stack rootStack;
  private Table rootTable;
  private final List<TextButton> menuButtons = new ArrayList<>();
  private final List<TextButton.TextButtonStyle> originalStyles = new ArrayList<>();
  private InputListener keyboardListener;
  private int selectedIndex = -1;

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
    background.setScaling(Scaling.stretch);
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
    Texture cardButtonFrameTexture =
        ServiceLocator.getResourceService().getAsset(CARD_BUTTON_TEXTURE, Texture.class);
    cardButtonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Texture enemyButtonFrameTexture =
        ServiceLocator.getResourceService().getAsset(ENEMY_BUTTON_TEXTURE, Texture.class);
    enemyButtonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Texture itemButtonFrameTexture =
        ServiceLocator.getResourceService().getAsset(ITEM_BUTTON_TEXTURE, Texture.class);
    itemButtonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.center().pad(MenuTheme.SCREEN_PADDING);

    Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("title", Label.LabelStyle.class));
    titleStyle.fontColor = MenuTheme.warmParchment();
    Label title = new Label("Library", titleStyle);

    TextButton.TextButtonStyle backButtonStyle =
        MenuTheme.createButtonStyle(skin, buttonFrameTexture);
    TextButton.TextButtonStyle cardButtonStyle =
        MenuTheme.createButtonStyle(skin, cardButtonFrameTexture);
    TextButton.TextButtonStyle enemyButtonStyle =
        MenuTheme.createButtonStyle(skin, enemyButtonFrameTexture);
    TextButton.TextButtonStyle itemButtonStyle =
        MenuTheme.createButtonStyle(skin, itemButtonFrameTexture);
    TextButton cardLibraryButton = new TextButton("Card Library", cardButtonStyle);
    TextButton enemyLibraryButton = new TextButton("Enemy Library", enemyButtonStyle);
    TextButton itemLibraryButton = new TextButton("Item Library", itemButtonStyle);
    TextButton storyButton = new TextButton("Story", backButtonStyle);
    TextButton backButton = new TextButton("Back", backButtonStyle);

    cardLibraryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Card Library button clicked");
            game.setScreen(GdxGame.ScreenType.CARD_LIBRARY);
          }
        });

    cardLibraryButton.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
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

    itemLibraryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Item Library button clicked");
            game.setScreen(GdxGame.ScreenType.ITEM_LIBRARY);
          }
        });

    enemyLibraryButton.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
          }
        });

    itemLibraryButton.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
          }
        });

    storyButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Story button clicked");
            game.setScreen(GdxGame.ScreenType.STORY_LIBRARY);
          }
        });

    storyButton.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
          }
        });

    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    backButton.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer == -1) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
          }
        });

    rootTable.add(title).padBottom(12f).row();
    rootTable.defaults().width(MenuTheme.BUTTON_WIDTH).height(MenuTheme.BUTTON_HEIGHT);
    rootTable.add(cardLibraryButton).row();
    rootTable.add(enemyLibraryButton).row();
    rootTable.add(itemLibraryButton).row();
    rootTable.add(storyButton).row();
    rootTable.add(backButton);

    menuButtons.addAll(
        List.of(cardLibraryButton, enemyLibraryButton, itemLibraryButton, storyButton, backButton));
    for (int i = 0; i < menuButtons.size(); i++) {
      int index = i;
      TextButton button = menuButtons.get(i);
      originalStyles.add(button.getStyle());
      button.addListener(
          new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
              if (pointer == -1) selectButton(index);
            }
          });
    }
    keyboardListener =
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            return handleKey(keycode);
          }
        };
    rootStack.addListener(keyboardListener);
    rootStack.add(rootTable);
    stage.addActor(rootStack);
    stage.setKeyboardFocus(rootStack);
  }

  private void selectButton(int index) {
    if (index == selectedIndex) return;
    if (selectedIndex >= 0) {
      menuButtons.get(selectedIndex).setStyle(originalStyles.get(selectedIndex));
    }
    selectedIndex = index;
    TextButton.TextButtonStyle original = originalStyles.get(index);
    if (original.over != null) {
      TextButton.TextButtonStyle selected = new TextButton.TextButtonStyle(original);
      selected.up = original.over;
      if (original.overFontColor != null) selected.fontColor = original.overFontColor;
      menuButtons.get(index).setStyle(selected);
    }
  }

  boolean handleKey(int keycode) {
    if (menuButtons.isEmpty()) return false;
    switch (keycode) {
      case Input.Keys.UP, Input.Keys.W -> {
        selectButton(Math.max(0, selectedIndex - 1));
        return true;
      }
      case Input.Keys.DOWN, Input.Keys.S -> {
        selectButton(Math.min(menuButtons.size() - 1, selectedIndex + 1));
        return true;
      }
      case Input.Keys.ENTER, Input.Keys.SPACE -> {
        int index = selectedIndex;
        if (index < 0) return false;
        activateButton(index);
        return true;
      }
      case Input.Keys.ESCAPE -> {
        activateButton(menuButtons.size() - 1);
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private void activateButton(int index) {
    // Reuse the existing ChangeListener without changing the button's checked state.
    menuButtons.get(index).fire(new ChangeListener.ChangeEvent());
  }

  int getSelectedIndex() {
    return selectedIndex;
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
      Actor focus = stage.getKeyboardFocus();
      if (focus != null && focus.isDescendantOf(rootStack)) stage.setKeyboardFocus(null);
      if (keyboardListener != null) rootStack.removeListener(keyboardListener);
      for (int i = 0; i < menuButtons.size(); i++) {
        menuButtons.get(i).setStyle(originalStyles.get(i));
        menuButtons.get(i).clearListeners();
      }
      rootStack.remove();
      rootStack.clear();
      rootStack = null;
    }
    menuButtons.clear();
    originalStyles.clear();
    selectedIndex = -1;
    keyboardListener = null;
    super.dispose();
  }
}
