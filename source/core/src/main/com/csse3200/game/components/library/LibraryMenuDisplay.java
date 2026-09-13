package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the library category chooser. */
public class LibraryMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(LibraryMenuDisplay.class);
  private final GdxGame game;

  private Table rootTable;
  private Label statusLabel;

  public LibraryMenuDisplay(GdxGame game) {
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

    Label title = new Label("Library", skin, "title");
    TextButton cardLibraryButton = new TextButton("Card Library", skin);
    TextButton enemyLibraryButton = new TextButton("Enemy Library", skin);
    TextButton backButton = new TextButton("Back", skin);
    statusLabel = new Label("", skin);

    cardLibraryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Card Library button clicked");
            game.setScreen(GdxGame.ScreenType.CARD_LIBRARY);
          }
        });

    enemyLibraryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            logger.debug("Enemy Library button clicked before enemy library is ready");
            statusLabel.setText("Enemy Library is waiting for bestiary data.");
          }
        });

    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    rootTable.add(title).padBottom(25f).row();
    rootTable.add(cardLibraryButton).width(260f).padBottom(12f).row();
    rootTable.add(enemyLibraryButton).width(260f).padBottom(12f).row();
    rootTable.add(statusLabel).padBottom(18f).row();
    rootTable.add(backButton).width(160f);

    stage.addActor(rootTable);
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
