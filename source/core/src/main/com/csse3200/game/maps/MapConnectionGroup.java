package com.csse3200.game.maps;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.csse3200.game.services.ServiceLocator;

/**
 * MapConnectionGroup
 *
 * <p>Renders the line to connect connected Nodes together.
 */
public class MapConnectionGroup extends Group {
  private final float length;
  private final float angle;
  private Image mapConnection;

  /**
   * Constructer method to create the line. Calculates length and angle from @param start and @param
   * end Vector2s. The line is a image that is stretched towards to fit between the nodes.
   *
   * @param start Vector2 position of node 1
   * @param end Vector2 position of node 2
   */
  public MapConnectionGroup(Vector2 start, Vector2 end) {

    Vector2 difference = end.cpy().sub(start);
    this.length = difference.len();
    this.angle = difference.angleDeg() - 90f;

    mapConnection =
        new Image(
            ServiceLocator.getResourceService().getAsset("images/nodeLine.png", Texture.class));

    mapConnection.setSize(4, length);
    mapConnection.setOrigin(4, 0);
    mapConnection.setRotation(angle);
    setPosition(start.x - 2, start.y);
    mapConnection.setPosition(0, 0);
    mapConnection.getColor().a = 0.5f;

    addActor(mapConnection);
  }

  /**
   * Constructer used to test MapConnectionGroup without having to load assets
   *
   * @param start Vector2 position of node 1
   * @param end Vector2 position of node 2
   * @param test used to have a differnet signature to call only for test
   */
  public MapConnectionGroup(Vector2 start, Vector2 end, boolean test) {
    Vector2 difference = end.cpy().sub(start);
    this.length = difference.len();
    this.angle = difference.angleDeg() - 90f;

    mapConnection = new Image();

    mapConnection.setSize(4, length);
    mapConnection.setOrigin(4, 0);
    mapConnection.setRotation(angle);
    setPosition(start.x - 2, start.y);
    mapConnection.setPosition(0, 0);
    mapConnection.getColor().a = 0.5f;

    addActor(mapConnection);
  }

  /**
   * Returns @param angle
   *
   * @return the angle of the line
   */
  public double getAngle() {
    return this.angle;
  }
}
