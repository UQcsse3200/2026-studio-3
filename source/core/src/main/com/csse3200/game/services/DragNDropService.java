package com.csse3200.game.services;

import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

public class DragNDropService {
  private final DragAndDrop dragAndDrop = new DragAndDrop();

  public DragNDropService() {}

  public DragAndDrop getDragAndDrop() {
    return dragAndDrop;
  }
}
