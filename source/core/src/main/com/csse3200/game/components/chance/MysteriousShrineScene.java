package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.services.ServiceLocator;

/** Static shrine artwork. Keeps the existing choice callback timing without visual effects. */
final class MysteriousShrineScene extends Group {
  static final String BACKGROUND_TEXTURE = "images/chance/mysterious_shrine_scene_static_v4.png";
  static final String FLAME_TEXTURE = "images/chance/mysterious_shrine_flame_v1.png";
  static final float RESPONSE_SECONDS = .9f;
  private final Image background;
  private float response = -1;
  private boolean cancelled;
  private Runnable onRelease;

  MysteriousShrineScene(Skin skin) {
    setName("mysterious-shrine-scene");
    setTouchable(Touchable.disabled);
    var resources = ServiceLocator.getResourceService();
    if (resources != null && resources.containsAsset(BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      background = new Image(texture);
    } else {
      background = new Image(skin.newDrawable("white", new Color(.07f, .08f, .12f, 1)));
    }
    // Tone down only the static artwork; event text and buttons keep their original brightness.
    background.setColor(.84f, .84f, .84f, 1);
    addActor(background);
  }

  @Override
  protected void sizeChanged() {
    super.sizeChanged();
    if (background != null) background.setBounds(0, 0, getWidth(), getHeight());
  }

  void setOfferingHovered(boolean hovered) {
    // Static artwork deliberately does not react to button hover.
  }

  boolean isResponding() {
    return response >= 0;
  }

  boolean playResponse(Runnable callback) {
    if (isResponding() || cancelled) return false;
    response = 0;
    onRelease = callback;
    return true;
  }

  void leave() {
    // Preserve the existing leave callback; the artwork remains unchanged.
  }

  void cancel() {
    cancelled = true;
    onRelease = null;
    response = -1;
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (isResponding()) {
      response += delta;
      if (response >= RESPONSE_SECONDS) {
        Runnable callback = onRelease;
        onRelease = null;
        response = -1;
        if (callback != null && !cancelled) callback.run();
      }
    }
  }
}
