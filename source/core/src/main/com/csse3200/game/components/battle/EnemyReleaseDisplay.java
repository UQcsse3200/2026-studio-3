package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.rendering.RenderComponent;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

public class EnemyReleaseDisplay extends RenderComponent {
    private static final String TEXTURE_PATH =
            "images/enemy_release/heavens_grace.png";
    private static final int FRAME_SIZE = 128;
    private static final float FRAME_DURATION = 0.1f;

    private Animation<TextureRegion> beamAnimation;
    private EnemyReleaseComponent release;

    @Override
    public void create() {
        super.create();

        release = entity.getComponent(EnemyReleaseComponent.class);

        Texture texture = ServiceLocator.getResourceService()
                .getAsset(TEXTURE_PATH, Texture.class);

        texture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest);

        TextureRegion[][] frames =
                TextureRegion.split(texture, FRAME_SIZE, FRAME_SIZE);

        beamAnimation = new Animation<>(
                FRAME_DURATION, frames[0]);
        beamAnimation.setPlayMode(Animation.PlayMode.NORMAL);
    }

    @Override
    protected void draw(SpriteBatch batch) {
        if (release == null || !release.isPlaying()) {
            return;
        }

        // Let the recolour begin before the beam appears.
        float beamTime = release.getElapsedTime() - 0.4f;

        if (beamTime < 0f || beamAnimation.isAnimationFinished(beamTime)) {
            return;
        }

        TextureRegion frame = beamAnimation.getKeyFrame(beamTime, false);

        Vector2 position = entity.getPosition();
        Vector2 scale = entity.getScale();

        float size = Math.max(scale.x, scale.y) * 1.5f;
        float x = position.x + scale.x / 2f - size / 2f;
        float y = position.y;

        float previousColour = batch.getPackedColor();
        batch.setColor(1f, 1f, 1f, 0.75f);
        batch.draw(frame, x, y, size, size);
        batch.setPackedColor(previousColour);
    }
}