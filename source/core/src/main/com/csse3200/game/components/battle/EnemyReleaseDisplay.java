package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.rendering.RenderComponent;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class EnemyReleaseDisplay extends RenderComponent {
    private static final String TEXTURE_PATH =
            "images/enemy_release/heavens_grace.png";
    private static final int FRAME_SIZE = 128;

    private EnemyReleaseComponent release;
    private TextureRegion beamRegion;
    private Texture particleTexture;

    @Override
    public void create() {
        super.create();

        release = entity.getComponent(EnemyReleaseComponent.class);

        Texture texture = ServiceLocator.getResourceService()
                .getAsset(TEXTURE_PATH, Texture.class);

        texture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest);

        beamRegion = new TextureRegion(
                texture, 4 * FRAME_SIZE + 54, 0, 24, 40);

        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();

        particleTexture = new Texture(pixmap);
        particleTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest);

        pixmap.dispose();
    }

    @Override
    protected void draw(SpriteBatch batch) {
        if (release == null || !release.isPlaying()) {
            return;
        }

        float progress = release.getProgress();

        // Fade in over the first quarter, then out over the last quarter.
        float fade = Math.min(
                Math.min(progress / 0.25f, (1f - progress) / 0.25f),
                1f);
        float alpha = fade * 0.35f;

        Vector2 position = entity.getPosition();
        Vector2 scale = entity.getScale();

        float width = scale.x * 0.65f;
        float height = scale.y * 2f;
        float x = position.x + (scale.x - width) / 2f;

        float previousColour = batch.getPackedColor();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(beamRegion, x, position.y, width, height);
        batch.setPackedColor(previousColour);
    }

    @Override
    public float getZIndex() {
        return 1000f;
    }
}