package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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
  private Texture backgroundTexture;

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

    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.stretch);

    Table root = new Table();
    root.setFillParent(true);
    root.pad(35f);

    Label title = new Label("Blessing of War", skin, "title");

    Label.LabelStyle bodyStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    bodyStyle.fontColor = new Color(0.9f, 0.8f, 0.65f, 1f);

    Label subtitle =
        new Label(
            "The ancient warrior grants you knowledge of any technique you desire.", bodyStyle);

    Table cardList = createCardList();
    Table detailPanel = createDetailPanel(bodyStyle);

    cardList.setBackground(skin.newDrawable("white", new Color(0.05f, 0.03f, 0.10f, 0.72f)));
    detailPanel.setBackground(skin.newDrawable("white", new Color(0.05f, 0.03f, 0.10f, 0.72f)));

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

  private Table createCardList() {
    Table buttons = new Table();
    buttons.top();
    buttons.defaults().width(310f).height(55f).padBottom(8f);

    for (CardConfig card : cards) {
      TextButton button = new TextButton(card.name, skin);

      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              showCard(card);
            }
          });

      buttons.add(button);
      buttons.row();
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

    costLabel = new Label("", bodyStyle);
    typeLabel = new Label("", bodyStyle);
    rarityLabel = new Label("", bodyStyle);

    cardImage = new Image();
    cardImage.setScaling(Scaling.fit);

    claimButton = new TextButton("Claim This Card", skin);
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

    panel.add(claimButton).width(300f).height(75f);

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
