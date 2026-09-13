package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.ui.UIComponent;
import java.util.List;

/** Displays a card-library style overlay for Team 5 deck UI work. */
public class CardHandDisplay extends UIComponent {
  private static final int CARDS_PER_ROW = 3;
  private static final float CARD_WIDTH = 150f;
  private static final float CARD_HEIGHT = 214f;
  private static final List<DemoCard> DEMO_HAND =
      List.of(
          new DemoCard("Strike", "1", "Deal 6 damage.", new Color(0.74f, 0.18f, 0.16f, 1f)),
          new DemoCard("Defend", "1", "Gain 5 block.", new Color(0.20f, 0.42f, 0.72f, 1f)),
          new DemoCard(
              "Poison Dagger",
              "1",
              "Deal 4 damage.\nApply 3 poison.",
              new Color(0.20f, 0.52f, 0.25f, 1f)),
          new DemoCard(
              "Expose",
              "1",
              "Apply 2 vulnerable\nto all enemies.",
              new Color(0.86f, 0.56f, 0.16f, 1f)),
          new DemoCard("Inner Focus", "2", "Gain 2 strength.", new Color(0.47f, 0.28f, 0.73f, 1f)),
          new DemoCard("Bandage", "1", "Heal 6 health.", new Color(0.78f, 0.32f, 0.48f, 1f)));

  private Table buttonTable;
  private Table libraryOverlay;
  private boolean libraryVisible;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    buttonTable = new Table();
    buttonTable.setFillParent(true);
    buttonTable.top().right();
    buttonTable.padTop(56f).padRight(10f);

    TextButton cardsButton = new TextButton("Cards", skin);
    cardsButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            toggleLibrary();
          }
        });

    buttonTable.add(cardsButton).width(96f).height(40f).right();

    libraryOverlay = createLibraryOverlay();
    libraryOverlay.setVisible(false);

    stage.addActor(buttonTable);
    stage.addActor(libraryOverlay);
  }

  private Table createLibraryOverlay() {
    Table overlay = new Table();
    overlay.setFillParent(true);
    overlay.setTouchable(Touchable.enabled);
    overlay.setBackground(skin.newDrawable("white", new Color(0.02f, 0.02f, 0.02f, 0.76f)));
    overlay.pad(34f);

    Table libraryPanel = new Table();
    libraryPanel.top();
    libraryPanel.defaults().pad(6f);
    libraryPanel.setBackground(skin.newDrawable("white", new Color(0.10f, 0.08f, 0.06f, 0.95f)));
    libraryPanel.pad(18f);

    Table header = new Table();
    Label title = new Label("Card Library", skin, "title");
    title.setColor(Color.WHITE);
    TextButton closeButton = new TextButton("Close", skin);
    closeButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            hideLibrary();
          }
        });

    header.add(title).left().expandX().fillX();
    header.add(closeButton).width(94f).height(38f).right();
    libraryPanel.add(header).expandX().fillX();
    libraryPanel.row();

    Label sectionTitle = new Label("Current Hand", skin, "large");
    sectionTitle.setColor(Color.WHITE);
    libraryPanel.add(sectionTitle).left().expandX().fillX().padTop(6f);
    libraryPanel.row();

    Table cardGrid = new Table();
    cardGrid.defaults().pad(10f);

    for (int i = 0; i < DEMO_HAND.size(); i++) {
      cardGrid.add(createCard(DEMO_HAND.get(i))).width(CARD_WIDTH).height(CARD_HEIGHT);
      if ((i + 1) % CARDS_PER_ROW == 0) {
        cardGrid.row();
      }
    }

    libraryPanel.add(cardGrid).center().padTop(10f);
    overlay.add(libraryPanel).center();
    return overlay;
  }

  private Table createCard(DemoCard card) {
    Table cardTable = new Table();
    cardTable.top();
    cardTable.pad(8f);
    cardTable.setBackground(skin.newDrawable("white", new Color(0.95f, 0.91f, 0.78f, 1f)));

    Label cost = new Label(card.cost, skin, "large");
    cost.setColor(Color.WHITE);
    Table costBadge = new Table();
    costBadge.setBackground(skin.newDrawable("white", card.accent));
    costBadge.add(cost).center();

    Label name = new Label(card.name, skin, "small");
    name.setColor(Color.BLACK);
    name.setWrap(true);

    Label type = new Label("DEMO CARD", skin, "small");
    type.setColor(card.accent);

    Label description = new Label(card.description, skin, "small");
    description.setColor(Color.BLACK);
    description.setWrap(true);

    cardTable.add(costBadge).size(34f).left();
    cardTable.row();
    cardTable.add(name).width(CARD_WIDTH - 18f).padTop(10f);
    cardTable.row();
    cardTable.add(type).width(CARD_WIDTH - 18f).padTop(6f);
    cardTable.row();
    cardTable.add(description).width(CARD_WIDTH - 18f).expandY().top().padTop(14f);

    return cardTable;
  }

  private void toggleLibrary() {
    libraryVisible = !libraryVisible;
    libraryOverlay.setVisible(libraryVisible);
  }

  private void hideLibrary() {
    libraryVisible = false;
    libraryOverlay.setVisible(false);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Stage draws this UI component.
  }

  @Override
  public float getZIndex() {
    return 3f;
  }

  @Override
  public void dispose() {
    if (buttonTable != null) {
      buttonTable.remove();
    }
    if (libraryOverlay != null) {
      libraryOverlay.remove();
    }
    super.dispose();
  }

  private static class DemoCard {
    private final String name;
    private final String cost;
    private final String description;
    private final Color accent;

    private DemoCard(String name, String cost, String description, Color accent) {
      this.name = name;
      this.cost = cost;
      this.description = description;
      this.accent = accent;
    }
  }
}
