package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnemyReleaseDisplay extends RenderComponent {
  private static final String TEXTURE_PATH = "images/enemy_release/heavens_grace.png";
  private static final int FRAME_SIZE = 128;

  private static final int PARTICLE_COUNT = 8;
  private final List<Entity> particles = new ArrayList<>();
  private boolean particlesSpawned;

  private EnemyReleaseComponent release;
  private TextureRegion beamRegion;
  private Texture particleTexture;

  @Override
  public void create() {
    super.create();

    release = entity.getComponent(EnemyReleaseComponent.class);

    Texture texture = ServiceLocator.getResourceService().getAsset(TEXTURE_PATH, Texture.class);

    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    beamRegion = new TextureRegion(texture, 4 * FRAME_SIZE + 54, 0, 24, 40);

    Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();

    particleTexture = new Texture(pixmap);
    particleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    pixmap.dispose();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (release == null || !release.isPlaying()) {
      return;
    }

    float progress = release.getProgress();

    // Fade in over the first quarter, then out over the last quarter.
    float fade = Math.min(Math.min(progress / 0.25f, (1f - progress) / 0.25f), 1f);
    float alpha = fade * 0.35f;

    Vector2 position = entity.getPosition();

    float normalWidth = release.getOriginalScaleX();
    float normalHeight = release.getOriginalScaleY();

    float width = normalWidth * 0.65f;
    float height = normalHeight * 2f;
    float x = position.x + (normalWidth - width) / 2f;

    float previousColour = batch.getPackedColor();
    batch.setColor(1f, 1f, 1f, alpha);
    batch.draw(beamRegion, x, position.y, width, height);
    batch.setPackedColor(previousColour);
  }

  private void spawnParticles() {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    EffectVisualStyle style =
        new EffectVisualStyle(
            null,
            new Color(1f, 0.95f, 0.7f, 1f),
            1.2f, // Lifetime in seconds
            0.08f, // Starting size
            0.03f, // Ending size
            scale.y * 0.75f); // Upward travel

    for (int i = 0; i < PARTICLE_COUNT; i++) {
      Entity particle =
          new Entity()
              .addComponent(new EffectVisualComponent(particleTexture, style, 1f, i * 0.07f));

      float x = position.x + scale.x * MathUtils.random(0.25f, 0.75f);
      float y = position.y + scale.y * MathUtils.random(0.1f, 0.3f);

      particle.setPosition(x, y);
      ServiceLocator.getEntityService().register(particle);
      particles.add(particle);
    }
  }

  @Override
  public void update() {
    if (release != null && release.isPlaying() && !particlesSpawned) {
      particlesSpawned = true;
      spawnParticles();
    }

    Iterator<Entity> iterator = particles.iterator();
    while (iterator.hasNext()) {
      Entity particle = iterator.next();
      EffectVisualComponent visual = particle.getComponent(EffectVisualComponent.class);

      if (visual.isExpired()) {
        iterator.remove();
        particle.dispose();
      }
    }
  }

  @Override
  public float getZIndex() {
    return 1000f;
  }

  @Override
  public void dispose() {
    particles.clear();
    if (particleTexture != null) {
      particleTexture.dispose();
      particleTexture = null;
    }
    super.dispose();
  }
}
