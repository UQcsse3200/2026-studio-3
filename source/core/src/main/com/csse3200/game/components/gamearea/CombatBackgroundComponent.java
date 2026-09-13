package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RenderComponent;

public class CombatBackgroundComponent extends RenderComponent {
    private final Texture texture;
    private final OrthographicCamera camera;

    public CombatBackgroundComponent(Texture texture, OrthographicCamera camera) {
        this.texture = texture;
        this.camera  = camera;
    }

    @Override
    protected void draw(SpriteBatch batch) {
        float width  = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;

        float scale = Math.max(
                width / texture.getWidth(),
                height / texture.getHeight()
        );

        float drawWidth  = texture.getWidth() * scale;
        float drawHeight = texture.getHeight() * scale;

        float left   = camera.position.x - drawWidth / 2f;
        float bottom = camera.position.y - drawHeight / 2f;

        batch.draw(texture, left, bottom, drawWidth, drawHeight);
    }

    @Override
    public int getLayer() {
        return 0;
    }

    @Override
    public float getZIndex() {
        return 1f;
    }
}
