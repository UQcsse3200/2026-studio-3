package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLoadingException;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Read-only card library view backed by the current Team 6 card configuration file. */
public class CardLibraryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(CardLibraryDisplay.class);
  private static final float PANEL_WIDTH = 1120f;
  private static final float PANEL_HEIGHT = 680f;
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.96f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";

  private final GdxGame game;

  private Stack rootStack;
  private Table cardList;
  private Image cardImage;
  private Label nameLabel;
  private Label descriptionLabel;
  private Label costLabel;
  private Label typeLabel;
  private Label targetLabel;
  private Label rarityLabel;
  private Label effectsLabel;
  private Label artworkLabel;
  private TextButton.TextButtonStyle buttonStyle;

  public CardLibraryDisplay(GdxGame game) {
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
    addBackground(rootStack);

    Texture buttonFrameTexture = getTexture(MainMenuDisplay.BUTTON_FRAME_TEXTURE);
    buttonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    buttonStyle = MenuTheme.createButtonStyle(skin, buttonFrameTexture);

    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WHITE, PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    addDivider(panel);
    panel.row();
    addCardContent(panel);

    Table wrapper = new Table();
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);
    wrapper.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    rootStack.add(wrapper);
    stage.addActor(rootStack);
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
    Label eyebrow = new Label("CARD ARCHIVE", labelStyle(SMALL, MenuTheme.softCoral()));
    Label title = new Label("Card Library", labelStyle(LARGE, MenuTheme.warmParchment()));
    Label subtitle =
        new Label(
            "Browse the current card definitions and effect order.",
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
            game.setScreen(GdxGame.ScreenType.LIBRARY);
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

  private void addCardContent(Table panel) {
    try {
      List<CardConfig> cards = loadSortedCards();
      addCards(cards, panel);
      if (!cards.isEmpty()) {
        showCard(cards.get(0));
      }
    } catch (CardLoadingException exception) {
      logger.warn("Failed to load cards for library display", exception);
      Label errorLabel =
          new Label("Unable to load card library: " + exception.getMessage(), bodyLabelStyle());
      errorLabel.setWrap(true);
      panel.add(errorLabel).colspan(2).width(900f).padTop(30f).row();
    }
  }

  private List<CardConfig> loadSortedCards() {
    return CardConfigLoader.loadCards().stream()
        .sorted(
            Comparator.comparing((CardConfig card) -> card.type).thenComparing(card -> card.name))
        .toList();
  }

  private void addCards(List<CardConfig> cards, Table panel) {
    cardList = new Table();
    cardList.top();
    cardList.defaults().width(290f).height(58f).padBottom(8f).left();

    for (CardConfig card : cards) {
      TextButton cardButton = new TextButton(formatCardButton(card), buttonStyle);
      cardButton.getLabel().setFontScale(0.75f);
      cardButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              showCard(card);
            }
          });
      cardList.add(cardButton).row();
    }

    ScrollPane scrollPane = new ScrollPane(cardList, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    listPanel.add(new Label("CARDS", labelStyle(SMALL, MenuTheme.softCoral()))).left().expandX();
    listPanel.row();
    listPanel.add(scrollPane).expand().fill().padTop(12f);

    panel.add(listPanel).width(350f).expandY().fillY().padRight(22f);
    panel.add(createDetailPanel()).expand().fill();
  }

  private Table createDetailPanel() {
    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(24f);
    detailPanel.top();

    nameLabel = new Label("", labelStyle(LARGE, MenuTheme.warmParchment()));
    descriptionLabel = new Label("", bodyLabelStyle());
    costLabel = new Label("", labelStyle(DEFAULT, MenuTheme.softCoral()));
    typeLabel = new Label("", bodyLabelStyle());
    targetLabel = new Label("", bodyLabelStyle());
    rarityLabel = new Label("", bodyLabelStyle());
    effectsLabel = new Label("", labelStyle(DEFAULT, MenuTheme.warmParchment()));
    artworkLabel = new Label("", labelStyle(SMALL, MenuTheme.warmParchment()));
    cardImage = new Image();

    nameLabel.setFontScale(1.25f);
    descriptionLabel.setFontScale(1.1f);
    descriptionLabel.setWrap(true);
    effectsLabel.setWrap(true);
    artworkLabel.setWrap(true);
    cardImage.setScaling(Scaling.fit);

    Table artworkBackground = new Table();
    artworkBackground.setBackground(skin.newDrawable(WHITE, new Color(0.035f, 0.03f, 0.04f, 1f)));
    Stack artwork = new Stack();
    artwork.add(artworkBackground);
    artwork.add(cardImage);

    Table meta = new Table();
    meta.defaults().left().padBottom(8f);
    meta.add(costLabel).row();
    meta.add(typeLabel).row();
    meta.add(targetLabel).row();
    meta.add(rarityLabel).row();

    detailPanel.add(nameLabel).left().expandX();
    detailPanel.row();
    detailPanel.add(descriptionLabel).width(650f).left().padTop(8f).padBottom(16f);
    detailPanel.row();
    detailPanel.add(artwork).width(650f).height(230f).padBottom(16f);
    detailPanel.row();
    detailPanel.add(meta).left().expandX();
    detailPanel.row();
    detailPanel.add(effectsLabel).width(650f).left().top().padTop(8f);
    detailPanel.row();
    detailPanel.add(artworkLabel).width(650f).left().top().padTop(10f);
    return detailPanel;
  }

  private String formatCardButton(CardConfig card) {
    return card.cost + "  " + card.name;
  }

  private void showCard(CardConfig card) {
    nameLabel.setText(card.name);
    descriptionLabel.setText(card.description);
    costLabel.setText("Cost: " + card.cost);
    typeLabel.setText("Type: " + card.type);
    targetLabel.setText("Target: " + card.target);
    rarityLabel.setText("Rarity: " + card.rarity);
    effectsLabel.setText("Effects resolve in this order:\n" + formatEffects(card.effects));
    artworkLabel.setText("Artwork: " + card.texturePath);
    setCardImage(card.texturePath);
  }

  private void setCardImage(String texturePath) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (texturePath == null
        || texturePath.isBlank()
        || !resources.containsAsset(texturePath, Texture.class)) {
      cardImage.setDrawable(null);
      cardImage.setVisible(false);
      return;
    }

    Texture texture = resources.getAsset(texturePath, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    cardImage.setDrawable(new TextureRegionDrawable(texture));
    cardImage.setVisible(true);
  }

  private String formatEffects(EffectConfig[] effects) {
    if (effects == null || effects.length == 0) {
      return "None";
    }

    return Arrays.stream(effects).map(this::formatEffect).reduce((a, b) -> a + "\n" + b).orElse("");
  }

  private String formatEffect(EffectConfig effect) {
    String text = "- " + effect.type + " value " + effect.value;
    if (effect.duration > 0) {
      text += " for " + effect.duration + " turns";
    }
    return text;
  }

  private Label.LabelStyle bodyLabelStyle() {
    return labelStyle(DEFAULT, MenuTheme.warmParchment());
  }

  private Label.LabelStyle labelStyle(String baseStyle, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(baseStyle, Label.LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private Texture getTexture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
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
