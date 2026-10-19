package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.ui.UIComponent;

/** The themed defeat screen, with one explicit return-to-menu action. */
public class DefeatDisplay extends UIComponent {
  private static final String SCENE_TEXTURE = "images/ui/victory-background-approved.png";
  private static final String DEFEAT_TEXTURE = "images/ui/defeat-screen-mythic-small-title.png";
  private static final String INTACT_TEXTURE = "images/ui/defeat-screen-mythic-intact.png";
  private static final float REFERENCE_WIDTH = 1672f;
  private static final float REFERENCE_HEIGHT = 941f;
  private static final float LAYOUT_SCALE = 800f / REFERENCE_HEIGHT;
  private static final float BREAK_START = 1.11f;

  private Table root;
  private Actor backdrop;
  private Actor returnButton;
  private boolean returnQueued;
  private boolean returnRequested;

  @Override
  public void create() {
    super.create();

    Texture sceneTexture =
        ServiceLocator.getResourceService().getAsset(SCENE_TEXTURE, Texture.class);
    Texture defeatTexture =
        ServiceLocator.getResourceService().getAsset(DEFEAT_TEXTURE, Texture.class);
    Texture intactTexture =
        ServiceLocator.getResourceService().getAsset(INTACT_TEXTURE, Texture.class);
    sceneTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    defeatTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    intactTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    Image backgroundImage = new Image(sceneTexture);
    backgroundImage.setScaling(Scaling.fill);
    backgroundImage.setFillParent(true);
    backgroundImage.setTouchable(Touchable.disabled);
    backdrop = backgroundImage;
    stage.addActor(backgroundImage);

    float imageWidth = REFERENCE_WIDTH * LAYOUT_SCALE;
    float imageHeight = REFERENCE_HEIGHT * LAYOUT_SCALE;
    Group composition = new Group();
    composition.setName("defeatComposition");
    composition.setSize(imageWidth, imageHeight);

    Image artwork = new Image(defeatTexture);
    artwork.setSize(imageWidth, imageHeight);
    artwork.setTouchable(Touchable.disabled);
    composition.addActor(artwork);

    Image intactArtwork = new Image(intactTexture);
    intactArtwork.setName("intactWingFrame");
    intactArtwork.setSize(imageWidth, imageHeight);
    intactArtwork.setTouchable(Touchable.disabled);
    composition.addActor(intactArtwork);

    addFallingFragments(composition);
    composition.addAction(
        Actions.sequence(
            Actions.delay(0.85f),
            Actions.moveBy(-1f * LAYOUT_SCALE, 0f, 0.08f, Interpolation.sineOut),
            Actions.moveBy(2f * LAYOUT_SCALE, 0f, 0.10f, Interpolation.sineOut),
            Actions.moveBy(-1f * LAYOUT_SCALE, 0f, 0.08f, Interpolation.sineOut),
            Actions.run(
                () -> {
                  intactArtwork.setVisible(false);
                  AudioService.playSound(SoundId.ARMOUR_BREAK, 0.32f);
                }),
            Actions.delay(0.06f),
            Actions.moveBy(-4f * LAYOUT_SCALE, -2f * LAYOUT_SCALE, 0.05f),
            Actions.moveBy(7f * LAYOUT_SCALE, 3f * LAYOUT_SCALE, 0.07f),
            Actions.moveBy(-5f * LAYOUT_SCALE, -2f * LAYOUT_SCALE, 0.08f),
            Actions.moveBy(3f * LAYOUT_SCALE, 2f * LAYOUT_SCALE, 0.10f),
            Actions.moveBy(-2f * LAYOUT_SCALE, -1f * LAYOUT_SCALE, 0.12f),
            Actions.moveBy(1f * LAYOUT_SCALE, 0f, 0.14f, Interpolation.sineOut)));

    // The illustration contains the button artwork. Keep its interactive bounds at the same
    // fixed reference coordinates so viewport changes never move the hit target away from it.
    float buttonX = 595f * LAYOUT_SCALE;
    float buttonY = (REFERENCE_HEIGHT - 730f) * LAYOUT_SCALE;
    float buttonWidth = 482f * LAYOUT_SCALE;
    float buttonHeight = 130f * LAYOUT_SCALE;

    Image hover = new Image(skin.newDrawable("white", Color.WHITE));
    hover.setName("returnButtonHighlight");
    hover.setBounds(
        buttonX + 28f * LAYOUT_SCALE,
        buttonY + 22f * LAYOUT_SCALE,
        buttonWidth - 56f * LAYOUT_SCALE,
        buttonHeight - 44f * LAYOUT_SCALE);
    hover.setColor(0.9f, 0.68f, 0.34f, 0f);
    hover.setTouchable(Touchable.disabled);
    composition.addActor(hover);

    returnButton = new Actor();
    returnButton.setName("returnToMenu");
    returnButton.setBounds(buttonX, buttonY, buttonWidth, buttonHeight);
    returnButton.addListener(
        new ClickListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (!returnQueued) {
              hover.clearActions();
              hover.addAction(Actions.alpha(0.08f, 0.12f));
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (!returnQueued) {
              hover.clearActions();
              hover.addAction(Actions.alpha(0f, 0.12f));
            }
          }

          @Override
          public void clicked(InputEvent event, float x, float y) {
            queueReturn(hover);
          }
        });
    returnButton.addListener(
        new com.badlogic.gdx.scenes.scene2d.InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
              queueReturn(hover);
              return true;
            }
            return false;
          }
        });
    composition.addActor(returnButton);

    root = new Table();
    root.setFillParent(true);
    root.add(composition).size(imageWidth, imageHeight);
    stage.addActor(root);
    stage.setKeyboardFocus(returnButton);
  }

  private void addFallingFragments(Group composition) {
    float[][] fragments = {{993f, 805f, 5f}, {1012f, 796f, 4f}, {1026f, 784f, 3f}};
    for (int index = 0; index < fragments.length; index++) {
      float[] fragment = fragments[index];
      Image chip = new Image(skin.newDrawable("white", new Color(0.66f, 0.42f, 0.25f, 1f)));
      chip.setName("wingFragment" + index);
      chip.setBounds(
          fragment[0] * LAYOUT_SCALE,
          fragment[1] * LAYOUT_SCALE,
          fragment[2] * LAYOUT_SCALE,
          fragment[2] * LAYOUT_SCALE);
      chip.getColor().a = 0f;
      chip.setTouchable(Touchable.disabled);
      composition.addActor(chip);
      chip.addAction(
          Actions.sequence(
              Actions.delay(BREAK_START),
              Actions.alpha(1f),
              Actions.parallel(
                  Actions.moveBy(
                      (index - 1) * 14f * LAYOUT_SCALE,
                      -(46f + index * 17f) * LAYOUT_SCALE,
                      0.66f,
                      Interpolation.pow2In),
                  Actions.fadeOut(0.66f)),
              Actions.removeActor()));
    }
  }

  private void queueReturn(Image hover) {
    if (returnQueued || returnRequested) {
      return;
    }
    returnQueued = true;
    hover.clearActions();
    hover.addAction(Actions.sequence(Actions.alpha(0.18f, 0.04f), Actions.alpha(0f, 0.12f)));
    returnButton.addAction(
        Actions.sequence(Actions.delay(0.16f), Actions.run(this::requestReturn)));
  }

  private void requestReturn() {
    if (returnRequested) {
      return;
    }
    returnRequested = true;
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The stage draws the artwork and button.
  }

  @Override
  public void dispose() {
    if (backdrop != null) {
      backdrop.remove();
      backdrop = null;
    }
    if (root != null) {
      root.remove();
      root = null;
    }
    super.dispose();
  }
}
