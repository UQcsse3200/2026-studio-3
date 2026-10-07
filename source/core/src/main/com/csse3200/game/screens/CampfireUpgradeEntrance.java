package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import java.util.function.BooleanSupplier;

/** Two equivalent entrances to the shared upgrade library; no card-selection logic lives here. */
final class CampfireUpgradeEntrance extends Group {
  private final RuneAltar altar;

  CampfireUpgradeEntrance(Skin skin, Runnable openUpgrade, BooleanSupplier blocked) {
    this(skin, openUpgrade, blocked, null);
  }

  CampfireUpgradeEntrance(Skin skin, Runnable openUpgrade, BooleanSupplier blocked, Texture scene) {
    setSize(1587, 992);
    Runnable open =
        () -> {
          if (!blocked.getAsBoolean()) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            openUpgrade.run();
          }
        };
    altar = new RuneAltar(skin.getDrawable("white"), open, blocked);
    if (scene != null)
      altar.centerArtwork =
          new TextureRegion(
              scene,
              Math.round(720 * scene.getWidth() / 1587f),
              Math.round(470 * scene.getHeight() / 992f),
              Math.round(148 * scene.getWidth() / 1587f),
              Math.round(116 * scene.getHeight() / 992f));
    altar.setName("upgrade-rune-altar");
    altar.setBounds(548, 292, 488, 286);
    addActor(altar);
    TextButton button = new TextButton("Upgrade", skin);
    button.setName("workbench-upgrade-button");
    button.setBounds((1587 - 300) / 2f, 119, 300, 76);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!button.isDisabled() && !altar.isActivating()) open.run();
          }
        });
    addActor(button);
  }

  void dispose() {
    if (altar.centerShader != null) altar.centerShader.dispose();
  }

  /** The complete physical disk is hit-tested, not just its central symbol. */
  private static final class RuneAltar extends Actor {
    private final Drawable pixel;
    private final ClickListener clicks;
    private float elapsed;
    private boolean hovered;
    private float hoverBlend;
    private float activation = -1;
    private float activationPhase;
    private final float[] runeAfterglow = new float[16];
    private final Runnable open;
    private final BooleanSupplier blocked;
    private static final float RELEASE_SECONDS = .22f;
    private static final float TAU = (float) (Math.PI * 2);
    private TextureRegion centerArtwork;
    private ShaderProgram centerShader;
    private boolean shaderAttempted;

    RuneAltar(Drawable pixel, Runnable open, BooleanSupplier blocked) {
      this.pixel = pixel;
      this.open = open;
      this.blocked = blocked;
      clicks =
          new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
              if (pointer == -1 && !blocked.getAsBoolean()) {
                hovered = true;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
              }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
              if (pointer == -1) {
                hovered = false;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
              }
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
              if (!blocked.getAsBoolean() && !isActivating()) {
                activation = 0;
                activationPhase = elapsed;
              }
            }
          };
      addListener(clicks);
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
      if (!isVisible() || (touchable && getTouchable() != Touchable.enabled)) return null;
      float nx = (x - getWidth() / 2) / (getWidth() / 2);
      float ny = (y - getHeight() / 2) / (getHeight() / 2);
      return nx * nx + ny * ny <= 1 ? this : null;
    }

    @Override
    public void act(float delta) {
      super.act(delta);
      elapsed += delta;
      float target = hovered && !blocked.getAsBoolean() ? 1 : 0;
      hoverBlend +=
          Math.signum(target - hoverBlend)
              * Math.min(
                  Math.abs(target - hoverBlend), delta / (target > hoverBlend ? .18f : .32f));
      for (int i = 0; i < runeAfterglow.length; i++) {
        float passing = outerLight(i * TAU / runeAfterglow.length, elapsed) * hoverBlend;
        runeAfterglow[i] = Math.max(passing, runeAfterglow[i] * (float) Math.exp(-delta / .38f));
      }
      if (isActivating()) {
        activation += delta;
        if (activation >= RELEASE_SECONDS) {
          activation = -1;
          hoverBlend = 0;
          hovered = false;
          java.util.Arrays.fill(runeAfterglow, 0);
          // Exactly one delivery after the local effect; gameplay remains in the shared callback.
          open.run();
        }
      }
    }

    boolean isActivating() {
      return activation >= 0;
    }

    private float outerLight(float angle, float phase) {
      float strength = 0;
      for (int i = 0; i < 3; i++) {
        strength = Math.max(strength, tail(angle, -phase * .42f + i * TAU / 3, .62f, true));
      }
      return strength;
    }

    private float tail(float angle, float head, float length, boolean clockwise) {
      float distance = ((clockwise ? angle - head : head - angle) % TAU + TAU) % TAU;
      return distance < length ? (float) Math.pow(1 - distance / length, 1.6) : 0;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      Color old = new Color(batch.getColor());
      float pulse = .5f + .5f * (float) Math.sin(elapsed * TAU / 2.6f);
      float progress = isActivating() ? Math.min(1, activation / RELEASE_SECONDS) : 0;
      float shrink = isActivating() ? 1 - progress * progress : 1;
      float phase = isActivating() ? activationPhase : elapsed;
      float energy = isActivating() ? 1 : hoverBlend;
      float cx = getX() + getWidth() / 2;
      float cy = getY() + getHeight() * .60f - (clicks.isPressed() ? 2 : 0);
      float alpha = parentAlpha * getColor().a;

      // Only the outer track travels; the inner stone ring remains unlit.
      for (int i = 0; i < 240; i++) {
        float angle = i * TAU / 240;
        trackPixel(
            batch,
            cx,
            cy,
            angle,
            190 * shrink,
            99 * shrink,
            outerLight(angle, phase) * energy * .85f,
            alpha);
      }
      // Rune echoes linger after a travelling head passes, then smoothly decay.
      for (int i = 0; i < runeAfterglow.length; i++) {
        float angle = i * TAU / runeAfterglow.length;
        float x = cx + (float) Math.cos(angle) * 166 * shrink;
        float y = cy + (float) Math.sin(angle) * 84 * shrink;
        float strength = runeAfterglow[i] * (isActivating() ? 1 - progress : 1);
        batch.setColor(1, .77f, .32f, alpha * strength * .55f);
        rune(batch, x, y, i);
      }

      // Four inward pixel motes connect both circuits to the center; no particle system needed.
      for (int i = 0; i < 4; i++) {
        float travel = isActivating() ? shrink : 1 - (elapsed * .23f + i * .25f) % 1;
        float angle = phase * .18f + i * TAU / 4;
        float x = cx + (float) Math.cos(angle) * 155 * travel;
        float y = cy + (float) Math.sin(angle) * 81 * travel;
        batch.setColor(1, .82f, .37f, alpha * energy * .65f);
        dot(batch, x, y, 2, 2);
        batch.setColor(1, .63f, .19f, alpha * energy * .18f);
        dot(batch, x + (float) Math.cos(angle) * 5, y + (float) Math.sin(angle) * 3, 2, 2);
      }

      float flash = isActivating() ? Math.max(0, (progress - .66f) / .34f) : 0;
      drawOriginalCenter(batch, alpha, .06f + hoverBlend * (.82f + pulse * .10f) + flash * .8f);
      batch.setColor(old);
    }

    /** Relight the authored gold/ivory pixels in place; never substitute a geometric shape. */
    private void drawOriginalCenter(Batch batch, float alpha, float intensity) {
      if (centerArtwork == null) return;
      if (!shaderAttempted) {
        shaderAttempted = true;
        centerShader =
            new ShaderProgram(
                "attribute vec4 a_position; attribute vec4 a_color; attribute vec2 a_texCoord0;"
                    + "uniform mat4 u_projTrans; varying vec4 v_color; varying vec2 v_texCoords;"
                    + "void main(){v_color=a_color; v_texCoords=a_texCoord0;"
                    + "gl_Position=u_projTrans*a_position;}",
                "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
                    + "varying vec4 v_color; varying vec2 v_texCoords; uniform sampler2D u_texture;"
                    + "uniform float u_energy; uniform vec4 u_region;"
                    + "void main(){vec4 p=texture2D(u_texture,v_texCoords);"
                    + "float ivory=step(.72,p.r)*step(.70,p.g)*step(.48,p.b);"
                    + "float gold=step(.34,p.r)*step(.23,p.g)*step(p.b*1.35,p.r);"
                    + "vec2 local=(v_texCoords-u_region.xy)/u_region.zw;"
                    + "vec2 d=abs((local-vec2(.493,.526))*vec2(148.0,116.0));"
                    + "float silhouette=step(d.x/70.0+d.y/55.0,1.03);"
                    + "float mask=max(ivory,gold)*silhouette;"
                    + "vec3 warm=min(p.rgb*1.45+vec3(.12,.055,.012),vec3(1.0,.77,.38));"
                    + "vec3 lit=mix(warm,vec3(1.0,.94,.79),ivory);"
                    + "gl_FragColor=vec4(lit,mask*v_color.a*min(1.0,u_energy*mix(.90,1.0,ivory)));}");
        if (!centerShader.isCompiled()) {
          Gdx.app.error("CampfireUpgradeEntrance", centerShader.getLog());
          centerShader.dispose();
          centerShader = null;
        }
      }
      if (centerShader == null) return;
      ShaderProgram previous = batch.getShader();
      batch.setShader(centerShader);
      centerShader.setUniformf("u_energy", intensity);
      centerShader.setUniformf(
          "u_region",
          centerArtwork.getU(),
          centerArtwork.getV(),
          centerArtwork.getU2() - centerArtwork.getU(),
          centerArtwork.getV2() - centerArtwork.getV());
      batch.setColor(1, 1, 1, alpha);
      batch.draw(centerArtwork, getX() + 172, getY() + 114, 148, 116);
      batch.setShader(previous);
    }

    private void trackPixel(
        Batch batch,
        float cx,
        float cy,
        float angle,
        float rx,
        float ry,
        float strength,
        float alpha) {
      if (strength < .005f) return;
      float x = cx + (float) Math.cos(angle) * rx;
      float y = cy + (float) Math.sin(angle) * ry;
      batch.setColor(1, .55f, .12f, alpha * strength * .16f);
      dot(batch, x - 2, y - 2, 5, 5);
      batch.setColor(1, .76f, .27f, alpha * strength);
      dot(batch, x, y, 2, 2);
    }

    private void rune(Batch batch, float x, float y, int index) {
      if (index % 4 == 0) {
        for (int j = 0; j <= 4; j++) {
          dot(batch, x - 4 + j, y + j, 2, 1);
          dot(batch, x + 4 - j, y + j, 2, 1);
          dot(batch, x - 4 + j, y - j, 2, 1);
          dot(batch, x + 4 - j, y - j, 2, 1);
        }
      } else {
        dot(batch, x, y - 3, 1, 7);
        dot(batch, x - 3, y + 1, 7, 1);
        dot(batch, x + (index % 2 == 0 ? -2 : 2), y - 2, 2, 1);
      }
    }

    private void dot(Batch batch, float x, float y, float w, float h) {
      pixel.draw(batch, Math.round(x), Math.round(y), w, h);
    }
  }
}
