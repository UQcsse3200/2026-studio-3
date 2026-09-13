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
