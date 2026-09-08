package com.csse3200.game.maps;

import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Throwaway stub for a clickable map node actor. Only carries a node id and clickable bounds, just
 * enough to test click handling before Damian's real map UI (#14) is ready. Replace with the real
 * node actor once it lands.
 */
public class NodeActor extends Actor {

  private final Integer nodeId;

  /**
   * Creates a stub node actor.
   *
   * @param nodeId id of the map node this actor represents
   * @param x x position
   * @param y y position
   * @param width clickable width
   * @param height clickable height
   */
  public NodeActor(Integer nodeId, float x, float y, float width, float height) {
    this.nodeId = nodeId;
    setBounds(x, y, width, height);
  }

  /**
   * Gets the id of the node this actor represents.
   *
   * @return node id
   */
  public Integer getNodeId() {
    return nodeId;
  }
}
