package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Scenic artwork and independently animated pixel layers; contains no reward or player logic. */
final class WishingFountainScene extends Group {
  static final String BACKGROUND_TEXTURE = "images/chance/wishing_fountain_scene_v1.png";
  static final float WISH_DURATION = 1.65f;

  enum WishState {
    IDLE,
    WISH_CHARGING,
    WISH_RELEASE,
    REWARD,
    COMPLETE
  }

  private final Image background;
  private final WaterAnimation water;
  private final CrossGlow cross;
  private final WishEffect wish;

  WishingFountainScene(Skin skin) {
    setTouchable(Touchable.disabled);
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && resources.containsAsset(BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      background = new Image(texture);
    } else {
      background = new Image(skin.newDrawable("white", new Color(0.22f, 0.27f, 0.31f, 1f)));
    }
    background.setScaling(Scaling.fill);
    addActor(background);
    TextureRegion pixel = skin.getRegion("white");
    // A feathered local shade, not a rectangular event panel.
    addActor(
        new Actor() {
          @Override
          public void draw(Batch batch, float parentAlpha) {
            Color original = new Color(batch.getColor());
            float width = Math.min(470f, getParent().getWidth());
            float top = getParent().getHeight();
            for (int row = 0; row < 12; row++) {
              for (int column = 0; column < 18; column++) {
                float fade = (1f - column / 18f) * (1f - row / 12f);
                batch.setColor(0.025f, 0.035f, 0.045f, fade * 0.52f * parentAlpha);
                batch.draw(pixel, column * width / 18f, top - (row + 1) * 24f, width / 18f, 24f);
              }
            }
            batch.setColor(original);
          }
        });
    water = new WaterAnimation(pixel);
    cross = new CrossGlow(pixel);
    wish = new WishEffect(pixel, cross, water);
    addActor(water);
    addActor(cross);
    addActor(wish);
  }

  boolean playWishAnimation(Runnable onFinished) {
    return wish.play(onFinished);
  }

  boolean isPlayingWish() {
    return wish.isPlaying();
  }

  WishState getWishState() {
    return wish.state;
  }

  void complete() {
    wish.cancel();
    wish.state = WishState.COMPLETE;
  }

  void cancel() {
    wish.cancel();
    wish.state = WishState.COMPLETE;
  }

  @Override
  protected void sizeChanged() {
    if (background == null) {
      return;
    }
    background.setBounds(0, 0, getWidth(), getHeight());
    // Match Scaling.fill exactly, so animated layers remain on the artwork when resized.
    float scale = Math.max(getWidth() / 1280f, getHeight() / 800f);
    float width = 1280f * scale;
    float height = 800f * scale;
    for (PixelLayer layer : new PixelLayer[] {water, cross, wish}) {
      layer.setBounds((getWidth() - width) / 2f, (getHeight() - height) / 2f, width, height);
    }
  }

  private abstract static class PixelLayer extends Actor {
    final TextureRegion pixel;
    float clock;

    PixelLayer(TextureRegion pixel) {
      this.pixel = pixel;
      setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
      super.act(delta);
      clock += delta;
    }

    void rect(Batch batch, float x, float y, float width, float height, Color colour, float alpha) {
      batch.setColor(colour.r, colour.g, colour.b, alpha);
      batch.draw(
          pixel,
          getX() + Math.round(x) * getWidth() / 1280f,
          getY() + Math.round(y) * getHeight() / 800f,
          width * getWidth() / 1280f,
          height * getHeight() / 800f);
    }
  }

  /** Continuous downward highlights, splash and elliptical basin ripples. */
  private static final class WaterAnimation extends PixelLayer {
    static final Color WATER = new Color(0.47f, 0.74f, 0.85f, 1);
    static final Color FOAM = new Color(0.81f, 0.94f, 0.96f, 1);
    float reflection;

    WaterAnimation(TextureRegion pixel) {
      super(pixel);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      Color original = new Color(batch.getColor());
      stream(batch, 640, 438, 357, 12, parentAlpha);
      stream(batch, 640, 350, 223, 17, parentAlpha);
      int frame = (int) (clock * 8f) % 5;
      for (int i = 0; i < 26; i++) {
        float angle = i * 2.39996f;
        float radius = 38f + (i % 7) * 28f;
        float x = 640 + (float) Math.cos(angle + frame * 0.025f) * radius;
        float y = 214 + (float) Math.sin(angle + frame * 0.025f) * radius * 0.075f;
        rect(
            batch,
            x,
            y,
            7 + i % 4 * 3,
            2,
            FOAM,
            parentAlpha
                * (0.15f
                    + 0.18f * (float) Math.sin(clock * 2 + i) * (float) Math.sin(clock * 2 + i)));
      }
      float ripple = (clock * 0.65f) % 1f;
      for (int i = 0; i < 22; i++) {
        float angle = i * (float) Math.PI * 2 / 22;
        rect(
            batch,
            640 + (float) Math.cos(angle) * (18 + ripple * 64),
            216 + (float) Math.sin(angle) * (4 + ripple * 9),
            4,
            2,
            FOAM,
            (1 - ripple) * 0.44f * parentAlpha);
      }
      for (int i = 0; i < 8; i++) {
        rect(
            batch,
            612 + i * 8,
            211 + (i % 3) * 3,
            5,
            2,
            CrossGlow.GOLD,
            reflection * 0.48f * parentAlpha);
      }
      batch.setColor(original);
    }

    private void stream(Batch batch, float x, float top, float bottom, float width, float alpha) {
      rect(batch, x - width / 2, bottom, width, top - bottom, WATER, 0.34f * alpha);
      for (int i = 0; i < 16; i++) {
        float y = top - ((clock * 72 + i * 17) % (top - bottom));
        float offset = ((i * 7) % 5 - 2) * 2;
        rect(batch, x + offset, y, 2 + i % 2, 3 + i % 4 * 2, FOAM, 0.65f * alpha);
      }
      for (int i = 0; i < 6; i++) {
        float phase = (clock * 1.6f + i / 6f) % 1;
        rect(
            batch,
            x + (i % 2 == 0 ? -1 : 1) * (6 + phase * 16),
            bottom + (float) Math.sin(phase * Math.PI) * 8,
            3,
            2,
            FOAM,
            (1 - phase) * 0.6f * alpha);
      }
    }
  }

  /** An independent chest cross; the rest of the statue remains unlit. */
  private static final class CrossGlow extends PixelLayer {
    static final Color GOLD = new Color(0.98f, 0.72f, 0.30f, 1);
    static final Color CORE = new Color(1f, 0.90f, 0.63f, 1);
    float charge;

    CrossGlow(TextureRegion pixel) {
      super(pixel);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      Color original = new Color(batch.getColor());
      float pulse = 0.5f + 0.5f * (float) Math.sin(clock * Math.PI * 2 / 2.1f);
      float brightness = 0.48f + pulse * 0.22f + charge * 0.30f;
      for (int i = 3; i >= 1; i--) {
        rect(
            batch,
            640 - i * 7,
            504 - i * 7,
            i * 14,
            i * 14,
            GOLD,
            (0.025f + charge * 0.045f) * brightness * parentAlpha);
      }
      rect(batch, 638, 486, 4, 38, GOLD, brightness * parentAlpha);
      rect(batch, 626, 509, 28, 4, GOLD, brightness * parentAlpha);
      rect(batch, 639, 490, 2, 31, CORE, brightness * 0.8f * parentAlpha);
      rect(batch, 629, 510, 22, 2, CORE, brightness * 0.8f * parentAlpha);
      // Sparse halo glints, deliberately weaker than the chest cross.
      for (int i = 0; i < 38; i++) {
        float angle = i * (float) Math.PI * 2 / 38;
        rect(
            batch,
            640 + (float) Math.cos(angle) * 72,
            650 + (float) Math.sin(angle) * 72,
            2,
            2,
            GOLD,
            (0.08f + pulse * 0.02f + charge * 0.12f) * parentAlpha);
      }
      batch.setColor(original);
    }
  }

  /** One-shot activation/charge/release; invokes the supplied callback once after release. */
  private static final class WishEffect extends PixelLayer {
    final CrossGlow cross;
    final WaterAnimation water;
    WishState state = WishState.IDLE;
    float elapsed;
    Runnable completion;

    WishEffect(TextureRegion pixel, CrossGlow cross, WaterAnimation water) {
      super(pixel);
      this.cross = cross;
      this.water = water;
    }

    boolean isPlaying() {
      return state == WishState.WISH_CHARGING || state == WishState.WISH_RELEASE;
    }

    boolean play(Runnable finished) {
      if (state != WishState.IDLE) {
        return false;
      }
      state = WishState.WISH_CHARGING;
      elapsed = 0;
      completion = finished;
      return true;
    }

    void cancel() {
      completion = null;
      cross.charge = 0;
      water.reflection = 0;
    }

    @Override
    public void act(float delta) {
      super.act(delta);
      if (!isPlaying()) {
        return;
      }
      elapsed += delta;
      cross.charge = Math.min(1, elapsed / 1.1f);
      water.reflection = cross.charge;
      if (elapsed >= 1.1f) {
        state = WishState.WISH_RELEASE;
      }
      if (elapsed >= WISH_DURATION) {
        state = WishState.REWARD;
        cancelCharge();
        Runnable finished = completion;
        completion = null;
        if (finished != null) {
          finished.run();
        }
      }
    }

    private void cancelCharge() {
      cross.charge = 0;
      water.reflection = 0;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      if (!isPlaying()) {
        return;
      }
      Color original = new Color(batch.getColor());
      float progress = Math.min(1, elapsed / 1.1f);
      for (int i = 0; i < 10; i++) {
        float angle = i * (float) Math.PI * 2 / 10;
        float radius =
            state == WishState.WISH_CHARGING
                ? 58 * (1 - progress) + 10
                : 16 + (elapsed - 1.1f) * 90;
        rect(
            batch,
            640 + (float) Math.cos(angle) * radius,
            504 + (float) Math.sin(angle) * radius,
            2,
            2,
            CrossGlow.GOLD,
            0.65f * parentAlpha);
      }
      if (state == WishState.WISH_RELEASE) {
        float release = Math.min(1, (elapsed - 1.1f) / 0.55f);
        float alpha = (float) Math.sin(release * Math.PI) * parentAlpha;
        float radius = 12 + release * 58;
        for (int i = 0; i < 36; i++) {
          float angle = i * (float) Math.PI * 2 / 36;
          rect(
              batch,
              640 + (float) Math.cos(angle) * radius,
              504 + (float) Math.sin(angle) * radius,
              3,
              3,
              CrossGlow.GOLD,
              alpha * 0.45f);
        }
        rect(batch, 638, 469, 4, 74, CrossGlow.CORE, alpha * 0.55f);
        rect(batch, 606, 502, 68, 4, CrossGlow.CORE, alpha * 0.55f);
      }
      batch.setColor(original);
    }
  }
}
