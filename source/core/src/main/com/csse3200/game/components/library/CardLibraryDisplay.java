package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLoadingException;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Read-only card library view backed by the current Team 6 card configuration file. */
public class CardLibraryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(CardLibraryDisplay.class);

  private final GdxGame game;

  private Table rootTable;
  private Label nameLabel;
  private Label descriptionLabel;
  private Label costLabel;
  private Label typeLabel;
  private Label targetLabel;
  private Label rarityLabel;
  private Label effectsLabel;
  private Label textureLabel;

  public CardLibraryDisplay(GdxGame game) {
    this.game = game;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.pad(35f);

    TextButton backButton = new TextButton("Back", skin);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.LIBRARY);
          }
        });

    rootTable.add(new Label("Card Library", skin, "title")).expandX().left();
    rootTable.add(backButton).width(150f).right().row();

    try {
      List<CardConfig> cards = loadSortedCards();
      addCardContent(cards);
      if (!cards.isEmpty()) {
        showCard(cards.get(0));
      }
    } catch (CardLoadingException exception) {
      logger.warn("Failed to load cards for library display", exception);
      Label errorLabel = new Label("Unable to load card library: " + exception.getMessage(), skin);
      errorLabel.setWrap(true);
      rootTable.add(errorLabel).colspan(2).width(800f).padTop(30f).row();
    }

    stage.addActor(rootTable);
  }

  private List<CardConfig> loadSortedCards() {
    return CardConfigLoader.loadCards().stream()
        .sorted(
            Comparator.comparing((CardConfig card) -> card.type).thenComparing(card -> card.name))
        .toList();
  }

  private void addCardContent(List<CardConfig> cards) {
    Table cardList = new Table();
    cardList.defaults().width(260f).padBottom(8f).left();

    for (CardConfig card : cards) {
      TextButton cardButton = new TextButton(formatCardButton(card), skin);
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

    Table detailTable = makeDetailTable();
    rootTable.add(scrollPane).width(320f).height(430f).padTop(25f).padRight(35f).top();
    rootTable.add(detailTable).expandX().fillX().height(430f).padTop(25f).top().row();
  }

  private Table makeDetailTable() {
    Table detailTable = new Table();
    detailTable.defaults().left().padBottom(8f);

    nameLabel = new Label("", skin, "title");
    descriptionLabel = new Label("", skin);
    descriptionLabel.setWrap(true);
    costLabel = new Label("", skin);
    typeLabel = new Label("", skin);
    targetLabel = new Label("", skin);
    rarityLabel = new Label("", skin);
    effectsLabel = new Label("", skin);
    effectsLabel.setWrap(true);
    textureLabel = new Label("", skin);
    textureLabel.setWrap(true);

    detailTable.add(nameLabel).width(650f).row();
    detailTable.add(descriptionLabel).width(650f).padBottom(18f).row();
    detailTable.add(costLabel).row();
    detailTable.add(typeLabel).row();
    detailTable.add(targetLabel).row();
    detailTable.add(rarityLabel).row();
    detailTable.add(effectsLabel).width(650f).padTop(10f).row();
    detailTable.add(textureLabel).width(650f).padTop(10f).row();

    return detailTable;
  }

  private String formatCardButton(CardConfig card) {
    return card.name + " (" + card.type + ")";
  }

  private void showCard(CardConfig card) {
    nameLabel.setText(card.name);
    descriptionLabel.setText(card.description);
    costLabel.setText("Cost: " + card.cost);
    typeLabel.setText("Type: " + card.type);
    targetLabel.setText("Target: " + card.target);
    rarityLabel.setText("Rarity: " + card.rarity);
    effectsLabel.setText("Effects:\n" + formatEffects(card.effects));
    textureLabel.setText("Texture: " + card.texturePath);
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
    rootTable.remove();
    super.dispose();
  }
}
