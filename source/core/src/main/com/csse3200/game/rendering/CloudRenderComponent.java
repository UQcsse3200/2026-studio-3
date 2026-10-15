package com.csse3200.game.rendering;

public class CloudRenderComponent extends TextureRenderComponent {
  private static final float CLOUD_Z = 2f;
  private static final int CLOUD_LAYER = 0;

  public CloudRenderComponent(String texturePath) {
    super(texturePath);
  }

  @Override
  public float getZIndex() {
    return CLOUD_Z;
  }

  @Override
  public int getLayer() {
    return CLOUD_LAYER;
  }
}
