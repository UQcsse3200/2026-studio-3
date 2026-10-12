package com.csse3200.game.components.library;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.story.StoryEntry;
import com.csse3200.game.story.StoryEntryLoader;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Library section where players read the plot and setting at their own pace.
 *
 * <p>Uses the same list-plus-detail layout as {@link ItemLibraryDisplay}. Entries come from {@link
 * StoryEntryLoader}, so text can change without code changes. Long entries scroll; Up/Down (or W/S)
 * moves between entries and Escape returns to the Library menu.
 */
public class StoryLibraryDisplay extends UIComponent {
  static final String ENTRY_LIST_NAME = "story-entry-list";
  static final String EMPTY_TITLE = "Nothing to read yet";
  static final String EMPTY_BODY = "The story entries could not be loaded.";

  private static final float PANEL_WIDTH = 1120f;
  private static final float PANEL_HEIGHT = 680f;
  private static final float LIST_WIDTH = 350f;
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.96f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";

  private final GdxGame game;
  private final List<StoryEntry> entries;
  private final List<TextButton> entryButtons = new ArrayList<>();

  private Stack rootStack;
  private Label titleLabel;
  private Label bodyLabel;
  private ScrollPane bodyScroll;
  private TextButton.TextButtonStyle buttonStyle;
  private int selectedIndex = -1;

  /** Creates the display using {@link StoryEntryLoader#DEFAULT_FILE}. */
  public StoryLibraryDisplay(GdxGame game) {
    this(game, StoryEntryLoader.loadDefault());
  }

  /** Creates the display with explicit entries (used by tests and alternative content). */
  public StoryLibraryDisplay(GdxGame game, List<StoryEntry> entries) {
    this.game = game;
    this.entries = List.copyOf(Objects.requireNonNull(entries, "entries must not be null"));
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);
    addBackground(rootStack);

    buttonStyle = createButtonStyle();

    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WHITE, PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    addDivider(panel);
    panel.row();
    Table detailPanel = createDetailPanel();
    panel.add(createEntryList()).width(LIST_WIDTH).expandY().fillY().padRight(22f);
    panel.add(detailPanel).expand().fill();

    Table wrapper = new Table();
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);
    wrapper.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    rootStack.add(wrapper);

    rootStack.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            return handleKey(keycode);
          }
        });

    stage.addActor(rootStack);
    stage.setKeyboardFocus(rootStack);

    if (entries.isEmpty()) {
      showEmptyState();
    } else {
      selectEntry(0);
    }
  }

  private void addBackground(Stack stack) {
    Texture backgroundTexture = getTexture(MainMenuDisplay.BACKGROUND_TEXTURE);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    stack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.62f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable(WHITE, overlayColour));
    stack.add(overlay);
  }

  private void addHeader(Table panel) {
    Table titleBlock = new Table();
    Label eyebrow = new Label("THE FALL OF THE PANTHEON", labelStyle(SMALL, MenuTheme.softCoral()));
    Label title = new Label("Story", labelStyle(LARGE, MenuTheme.warmParchment()));
    Label subtitle =
        new Label(
            "Read the world, the corruption and the angel's task.",
            labelStyle(SMALL, MenuTheme.warmParchment()));
    eyebrow.setFontScale(1.15f);
    title.setFontScale(1.35f);
    subtitle.setFontScale(1.05f);

    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);

    TextButton backButton = new TextButton("Back", buttonStyle);
    backButton.getLabel().setFontScale(0.85f);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            returnToLibrary();
          }
        });

    panel.add(titleBlock).left().expandX();
    panel.add(backButton).right().width(170f).height(58f);
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, MenuTheme.softCoral()));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(16f);
  }

  private Table createEntryList() {
    Table entryList = new Table();
    entryList.setName(ENTRY_LIST_NAME);
    entryList.top();
    entryList.defaults().width(LIST_WIDTH - 60f).height(76f).padBottom(8f).left();

    ButtonGroup<TextButton> group = new ButtonGroup<>();
    group.setMaxCheckCount(1);
    // Keep one entry selected so clicking the open entry again does not clear the selection.
    group.setMinCheckCount(1);
    for (int i = 0; i < entries.size(); i++) {
      int index = i;
      TextButton entryButton = new TextButton(entries.get(i).title(), buttonStyle);
      entryButton.getLabel().setFontScale(0.6f);
      entryButton.getLabel().setWrap(true);
      entryButton.getLabel().setAlignment(Align.center);
      entryButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (entryButton.isChecked()) {
                showEntry(index);
              }
            }
          });
      group.add(entryButton);
      entryButtons.add(entryButton);
      entryList.add(entryButton).row();
    }

    ScrollPane listScroll = new ScrollPane(entryList, skin);
    listScroll.setFadeScrollBars(false);
    listScroll.setScrollingDisabled(true, false);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    listPanel.add(new Label("ENTRIES", labelStyle(SMALL, MenuTheme.softCoral()))).left().expandX();
    listPanel.row();
    listPanel.add(listScroll).expand().fill().padTop(12f);
    return listPanel;
  }

  private Table createDetailPanel() {
    titleLabel = new Label("", labelStyle(LARGE, MenuTheme.warmParchment()));
    titleLabel.setFontScale(1.25f);
    titleLabel.setWrap(true);

    bodyLabel = new Label("", labelStyle(DEFAULT, MenuTheme.warmParchment()));
    bodyLabel.setFontScale(1.1f);
    bodyLabel.setWrap(true);
    bodyLabel.setAlignment(Align.topLeft);

    Table bodyTable = new Table();
    bodyTable.top().left();
    bodyTable.add(titleLabel).expandX().fillX().left().padRight(12f);
    bodyTable.row();
    bodyTable.add(bodyLabel).expandX().fillX().top().left().padTop(16f).padRight(12f);

    bodyScroll = new ScrollPane(bodyTable, skin);
    bodyScroll.setFadeScrollBars(false);
    bodyScroll.setScrollingDisabled(true, false);

    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(24f);
    detailPanel.top();
    detailPanel.add(bodyScroll).expand().fill();
    return detailPanel;
  }

  /**
   * Selects the entry at {@code index}, checking its list button and showing its text.
   *
   * @param index zero-based entry index; ignored when out of range
   */
  void selectEntry(int index) {
    if (index < 0 || index >= entryButtons.size()) {
      return;
    }
    TextButton button = entryButtons.get(index);
    if (button.isChecked()) {
      showEntry(index);
    } else {
      button.setChecked(true);
    }
  }

  /**
   * Handles keyboard navigation.
   *
   * @param keycode libGDX key code
   * @return whether the key was used
   */
  boolean handleKey(int keycode) {
    switch (keycode) {
      case Input.Keys.UP, Input.Keys.W -> {
        selectEntry(Math.max(0, selectedIndex - 1));
        return !entries.isEmpty();
      }
      case Input.Keys.DOWN, Input.Keys.S -> {
        selectEntry(Math.min(entries.size() - 1, selectedIndex + 1));
        return !entries.isEmpty();
      }
      case Input.Keys.ESCAPE -> {
        returnToLibrary();
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private void showEntry(int index) {
    StoryEntry entry = entries.get(index);
    selectedIndex = index;
    titleLabel.setText(entry.title());
    bodyLabel.setText(entry.bodyText());
    bodyScroll.setScrollY(0f);
    bodyScroll.updateVisualScroll();
  }

  private void showEmptyState() {
    selectedIndex = -1;
    titleLabel.setText(EMPTY_TITLE);
    bodyLabel.setText(EMPTY_BODY);
  }

  private void returnToLibrary() {
    game.setScreen(GdxGame.ScreenType.LIBRARY);
  }

  int getSelectedIndex() {
    return selectedIndex;
  }

  String getDisplayedTitle() {
    return titleLabel.getText().toString();
  }

  String getDisplayedBody() {
    return bodyLabel.getText().toString();
  }

  private Label.LabelStyle labelStyle(String baseStyle, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(baseStyle, Label.LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private Texture getTexture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  private TextButton.TextButtonStyle createButtonStyle() {
    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    style.font = skin.getFont("font_large");
    style.up = buttonDrawable(null);
    style.over = buttonDrawable(new Color(1f, 0.88f, 0.68f, 1f));
    style.down = buttonDrawable(new Color(0.7f, 0.48f, 0.58f, 1f));
    style.checked = buttonDrawable(new Color(1f, 0.72f, 0.34f, 1f));
    style.checkedOver = buttonDrawable(new Color(1f, 0.86f, 0.55f, 1f));
    style.fontColor = MenuTheme.warmParchment();
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.checkedFontColor = Color.WHITE;
    return style;
  }

  private Drawable buttonDrawable(Color tint) {
    Texture texture = getTexture(CardLibraryDisplay.BUTTON_TEXTURE);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
    drawable.setMinWidth(0f);
    drawable.setMinHeight(0f);
    return tint == null ? drawable : drawable.tint(tint);
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
    entryButtons.clear();
    super.dispose();
  }
}
