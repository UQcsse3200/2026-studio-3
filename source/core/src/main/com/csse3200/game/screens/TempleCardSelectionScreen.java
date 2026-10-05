package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Allows the player to choose any card as the reward for Blessing of War. */
public class TempleCardSelectionScreen extends ScreenAdapter {
  private final GdxGame game;
  private final RunState runState;
  private final Renderer renderer;
  private final Skin skin;

  private final List<CardConfig> cards;
  private final CardLibrary cardLibrary;
  private final String[] cardTextures;

  private Label nameLabel;
  private Label descriptionLabel;
  private Label costLabel;
  private Label typeLabel;
  private Label rarityLabel;
  private Image cardImage;
  private TextButton claimButton;
  private TextButton selectedCardButton;

  private Texture backgroundTexture;
  private Texture cardChoiceButtonTexture;
  private Texture templeButtonTexture;

  private CardConfig selectedCard;
  private boolean claimed;

  public TempleCardSelectionScreen(GdxGame game) {
    this.game = game;
    this.runState = game.getRunState();

    if (runState == null || !runState.hasPendingEliteTempleReward()) {
      throw new IllegalStateException(
          "TempleCardSelectionScreen opened without a pending Elite temple reward");
    }

    cards =
        CardConfigLoader.loadCards().stream()
            .sorted(Comparator.comparing(card -> card.name))
            .toList();

    cardLibrary = new CardLibrary(cards);
    cardTextures = collectCardTextures(cards);

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));

    loadAssets();
    createUI();
  }

  private void loadAssets() {
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(cardTextures);
    resources.loadAll();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    Entity inputEntity = new Entity();
    inputEntity.addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(inputEntity);

    backgroundTexture = new Texture(Gdx.files.internal("images/blessing_of_war_bg.png"));
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    cardChoiceButtonTexture =
        new Texture(Gdx.files.internal("images/temple_card_choice_button.png"));
    cardChoiceButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    templeButtonTexture =
        new Texture(Gdx.files.internal("images/ancient_temple_choice_button.png"));
    templeButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.stretch);

    Table root = new Table();
    root.setFillParent(true);
    root.pad(35f);

    Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("title", Label.LabelStyle.class));
    titleStyle.fontColor = Color.valueOf("F1B45A");

    Label title = new Label("Blessing of War", titleStyle);
    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = Color.valueOf("E8DCC5");

    Label subtitle =
        new Label(
            "The ancient warrior grants you knowledge of any technique you desire.", bodyStyle);

    Table cardList = createCardList();
    Table detailPanel = createDetailPanel(bodyStyle);

    cardList.setBackground(skin.newDrawable("white", new Color(0.02f, 0.035f, 0.11f, 0.48f)));

    detailPanel.setBackground(skin.newDrawable("white", new Color(0.015f, 0.025f, 0.08f, 0.60f)));

    root.add(title).colspan(2).padBottom(15f);
    root.row();

    root.add(subtitle).colspan(2).padBottom(30f);
    root.row();

    root.add(cardList).width(360f).expandY().fillY().padRight(25f);
    root.add(detailPanel).width(700f).expandY().fillY();

    Stack sceneRoot = new Stack();
    sceneRoot.setFillParent(true);
    sceneRoot.add(background);
    sceneRoot.add(root);

    stage.addActor(sceneRoot);

    if (!cards.isEmpty()) {
      showCard(cards.get(0));
    }
  }

  /**
   * Creates the normal blue-and-gold card selection button style.
   *
   * @return button style for unselected card choices
   */
  private TextButtonStyle createCardChoiceButtonStyle() {
    TextureRegionDrawable normal =
        new TextureRegionDrawable(new TextureRegion(cardChoiceButtonTexture));

    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));

    style.up = normal;

    // Warm highlight while hovering.
    style.over = normal.tint(Color.valueOf("FFF0C8"));

    // Darker blue when pressed.
    style.down = normal.tint(Color.valueOf("7F86A8"));

    style.fontColor = Color.valueOf("F4E7C5");
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.valueOf("F1C879");

    return style;
  }

  /**
   * Creates a highlighted style for the currently selected card.
   *
   * @return selected card button style
   */
  private TextButtonStyle createSelectedCardButtonStyle() {
    TextureRegionDrawable selected =
        new TextureRegionDrawable(new TextureRegion(cardChoiceButtonTexture));

    TextButtonStyle style = new TextButtonStyle(createCardChoiceButtonStyle());

    // Selected: brighter golden-violet highlight.
    style.up = selected.tint(Color.valueOf("FFD98A"));
    style.over = selected.tint(Color.valueOf("FFF0B8"));
    style.down = selected.tint(Color.valueOf("B894D6"));

    style.fontColor = Color.valueOf("FFF4D6");
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;

    return style;
  }

  private TextButtonStyle createTempleButtonStyle() {
    TextureRegionDrawable normal =
        new TextureRegionDrawable(new TextureRegion(templeButtonTexture));

    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));

    style.up = normal;

    // Hover: brighter warm-gold/red tone.
    style.over = normal.tint(Color.valueOf("E6A85C"));

    // Pressed: darker red tone.
    style.down = normal.tint(Color.valueOf("8C493D"));

    style.disabled = normal.tint(Color.valueOf("5E5550"));

    style.fontColor = Color.valueOf("F6E8C8");
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.valueOf("F1C879");
    style.disabledFontColor = Color.valueOf("94877A");

    return style;
  }

  private Table createCardList() {
    Table buttons = new Table();
    buttons.top();
    buttons.defaults().width(320f).height(55f).padBottom(8f);

    TextButton firstButton = null;

    for (CardConfig card : cards) {
      TextButton button = new TextButton(card.name, createCardChoiceButtonStyle());
      button.getLabel().setFontScale(0.9f);

      if (firstButton == null) {
        firstButton = button;
      }

      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (selectedCardButton != null) {
                selectedCardButton.setStyle(createCardChoiceButtonStyle());
              }

              selectedCardButton = button;
              selectedCardButton.setStyle(createSelectedCardButtonStyle());

              showCard(card);
            }
          });

      buttons.add(button);
      buttons.row();
    }

    if (firstButton != null) {
      selectedCardButton = firstButton;
      selectedCardButton.setStyle(createSelectedCardButtonStyle());
    }

    ScrollPane scrollPane = new ScrollPane(buttons, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);

    Table panel = new Table();
    panel.top();

    panel.add(new Label("Choose Any Card", skin, "title")).padBottom(15f);
    panel.row();

    panel.add(scrollPane).expand().fill();

    return panel;
  }

  private Table createDetailPanel(Label.LabelStyle bodyStyle) {
    Table panel = new Table();
    panel.top();
    panel.pad(20f);

    nameLabel = new Label("", skin, "title");

    descriptionLabel = new Label("", bodyStyle);
    descriptionLabel.setWrap(true);

    Label.LabelStyle costStyle = new Label.LabelStyle(bodyStyle);
    costStyle.fontColor = Color.valueOf("F1B45A");

    Label.LabelStyle typeStyle = new Label.LabelStyle(bodyStyle);
    typeStyle.fontColor = Color.valueOf("BFD8FF");

    Label.LabelStyle rarityStyle = new Label.LabelStyle(bodyStyle);
    rarityStyle.fontColor = Color.valueOf("D7B7FF");

    costLabel = new Label("", costStyle);
    typeLabel = new Label("", typeStyle);
    rarityLabel = new Label("", rarityStyle);

    cardImage = new Image();
    cardImage.setScaling(Scaling.fit);

    // Keep the original project button style here.
    claimButton = new TextButton("Claim This Card", createTempleButtonStyle());
    claimButton.getLabel().setFontScale(1.05f);
    claimButton.setDisabled(true);

    claimButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            claimSelectedCard();
          }
        });

    panel.add(nameLabel).padBottom(15f);
    panel.row();

    panel.add(cardImage).width(420f).height(240f).padBottom(15f);
    panel.row();

    panel.add(descriptionLabel).width(600f).padBottom(15f);
    panel.row();

    panel.add(costLabel).padBottom(6f);
    panel.row();

    panel.add(typeLabel).padBottom(6f);
    panel.row();

    panel.add(rarityLabel).padBottom(25f);
    panel.row();

    panel.add(claimButton).width(360f).height(82f);

    return panel;
  }

  private void showCard(CardConfig card) {
    selectedCard = card;

    nameLabel.setText(card.name);
    descriptionLabel.setText(card.description);
    costLabel.setText("Cost: " + card.cost);
    typeLabel.setText("Type: " + card.type);
    rarityLabel.setText("Rarity: " + card.rarity);

    setCardImage(card.texturePath);

    claimButton.setDisabled(false);
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
    cardImage.setDrawable(new TextureRegionDrawable(texture));
    cardImage.setVisible(true);
  }

  private void claimSelectedCard() {
    if (claimed || selectedCard == null) {
      return;
    }

    claimed = true;

    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardLibrary);
    playerDeck.addCard(selectedCard.id);

    runState.clearPendingEliteTempleReward();
    game.setScreen(GdxGame.ScreenType.MAP);
  }

  private static String[] collectCardTextures(List<CardConfig> cards) {
    Set<String> paths = new LinkedHashSet<>();

    for (CardConfig card : cards) {
      if (card.texturePath != null && !card.texturePath.isBlank()) {
        paths.add(card.texturePath);
      }
    }

    return paths.toArray(new String[0]);
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.08f, 0.06f, 0.04f, 1f);
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    if (backgroundTexture != null) {
      backgroundTexture.dispose();
    }

    if (cardChoiceButtonTexture != null) {
      cardChoiceButtonTexture.dispose();
    }

    if (templeButtonTexture != null) {
      templeButtonTexture.dispose();
    }

    skin.dispose();
    renderer.dispose();

    ResourceService resources = ServiceLocator.getResourceService();
    resources.unloadAssets(cardTextures);

    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
