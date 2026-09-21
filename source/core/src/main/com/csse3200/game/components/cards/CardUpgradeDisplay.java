package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.csse3200.game.ui.UIComponent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardUpgradeDisplay extends UIComponent {
  private static final int CARDS_PER_ROW = 3;
  private static final float CARD_WIDTH = 150f;
  private static final float CARD_HEIGHT = 260f;
  private static final Color CARD_FACE = new Color(0.95f, 0.91f, 0.78f, 1f);
  private static final Color ACCENT = new Color(0.74f, 0.18f, 0.16f, 1f);
  private static final Color UPGRADE = new Color(0.18f, 0.48f, 0.29f, 1f);
  private static final Color SELECTED_FACE = new Color(0.86f, 0.95f, 0.82f, 1f);
  private static final Color DISABLED_FACE = new Color(0.62f, 0.60f, 0.55f, 1f);
  private static final Color SCRIM = new Color(0.02f, 0.02f, 0.02f, 0.76f);
  private static final Color PANEL = new Color(0.10f, 0.08f, 0.06f, 0.95f);
  private final Map<String, Table> tilesByInstanceId = new LinkedHashMap<>();
  private final CardUpgradeCommitter committer;
  private Table buttonTable;
  private Table libraryOverlay;
  private TextButton confirmButton;
  private boolean libraryVisible;
  private final CardUpgradeSelection selection;

  /**
   * A constructor for Card upgrade selection
   *
   * @param selection receive the list of the player's selection
   * @param committer receive card upgrade commit
   */
  public CardUpgradeDisplay(CardUpgradeSelection selection, CardUpgradeCommitter committer) {
    this.selection = selection;
    this.committer = committer;
  }

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

    TextButton cardsButton = new TextButton("Upgrade", skin);
    cardsButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            toggleLibrary();
          }
        });
    cardsButton.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.stop();
            return true;
          }
        });
    buttonTable.add(cardsButton).width(96f).height(40f).right();

    libraryOverlay = createLibraryOverlay();
    libraryOverlay.setVisible(false);
    libraryOverlay.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.stop();
            return true;
          }
        });
    stage.addActor(buttonTable);
    stage.addActor(libraryOverlay);
    refresh();
  }

  private Table createLibraryOverlay() {
    Table overlay = new Table();
    overlay.setFillParent(true);
    overlay.setTouchable(Touchable.enabled);
    overlay.setBackground(skin.newDrawable("white", SCRIM));
    overlay.pad(34f);

    Table libraryPanel = new Table();
    libraryPanel.top();
    libraryPanel.defaults().pad(6f);
    libraryPanel.setBackground(skin.newDrawable("white", PANEL));
    libraryPanel.pad(18f);

    Table header = new Table();
    Label title = new Label("Upgrade a Card", skin, "title");
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

    Label sectionTitle = new Label("Select a Card to Upgrade", skin, "large");
    sectionTitle.setColor(Color.WHITE);
    libraryPanel.add(sectionTitle).left().expandX().fillX().padTop(6f);
    libraryPanel.row();

    Table cardGrid = new Table();
    cardGrid.defaults().pad(10f);

    List<CardUpgradeSelection.UpgradeOption> options = selection.getCardUpgradeOption();
    for (int i = 0; i < options.size(); i++) {
      CardUpgradeSelection.UpgradeOption option = options.get(i);
      Table tile = createTile(option);
      tilesByInstanceId.put(option.instance().instanceId(), tile);
      cardGrid.add(tile).width(CARD_WIDTH).height(CARD_HEIGHT);
      if ((i + 1) % CARDS_PER_ROW == 0) {
        cardGrid.row();
      }
    }

    confirmButton = new TextButton("Upgrade (0)", skin);
    confirmButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!selection.canConfirm()) {
              return;
            }
            committer.commitUpgrades(selection.getSelectedInstanceIds());
            selection.reset();
            hideLibrary();
          }
        });
    libraryPanel.add(cardGrid).center().padTop(10f);
    libraryPanel.row();
    libraryPanel.add(confirmButton).width(140f).height(42f).right().padTop(14f);
    overlay.add(libraryPanel).center();
    return overlay;
  }

  private void toggleLibrary() {
    libraryVisible = !libraryVisible;
    libraryOverlay.setVisible(libraryVisible);
    if (libraryVisible) {
      libraryOverlay.toFront();
    }
  }

  private void hideLibrary() {
    libraryVisible = false;
    libraryOverlay.setVisible(false);
    refresh();
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

  private Table createTile(CardUpgradeSelection.UpgradeOption option) {
    Table tile = new Table();
    tile.top();
    tile.pad(8f);
    tile.setBackground(skin.newDrawable("white", CARD_FACE));
    tile.setTouchable(Touchable.enabled);
    tile.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            selection.toggle(option.instance().instanceId());
            refresh();
          }
        });
    Label cost = new Label(String.valueOf(option.current().cost()), skin, "large");
    cost.setColor(Color.WHITE);
    Table costBadge = new Table();
    costBadge.setBackground(skin.newDrawable("white", ACCENT));
    costBadge.add(cost).center();

    Label name = new Label(option.current().name(), skin, "small");
    name.setColor(Color.BLACK);
    name.setWrap(true);

    Label upgradedName = new Label("-> " + option.preview().name(), skin, "small");
    upgradedName.setColor(UPGRADE);

    Label current = new Label(option.current().description(), skin, "small");
    current.setColor(Color.BLACK);
    current.setWrap(true);

    Label preview = new Label(option.preview().description(), skin, "small");
    preview.setColor(UPGRADE);
    preview.setWrap(true);

    tile.add(costBadge).size(34f).left();
    tile.row();
    tile.add(name).width(CARD_WIDTH - 18f).padTop(8f);
    tile.row();
    tile.add(upgradedName).width(CARD_WIDTH - 18f).padTop(4f);
    tile.row();
    tile.add(current).width(CARD_WIDTH - 18f).padTop(10f);
    tile.row();
    tile.add(preview).width(CARD_WIDTH - 18f).expandY().top().padTop(6f);

    return tile;
  }

  private void refresh() {
    for (Map.Entry<String, Table> entry : tilesByInstanceId.entrySet()) {
      String instanceId = entry.getKey();
      Color face;
      if (selection.isSelected(instanceId)) {
        face = SELECTED_FACE;
      } else if (!selection.canSelect(instanceId)) {
        face = DISABLED_FACE;
      } else {
        face = CARD_FACE;
      }
      entry.getValue().setBackground(skin.newDrawable("white", face));
    }
    confirmButton.setText("Upgrade (" + selection.getSelectedInstanceIds().size() + ")");
    confirmButton.setDisabled(!selection.canConfirm());
  }
}
