package com.csse3200.game.maps;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import java.util.List;

/**
 * Attaches click listeners to node actors and routes clicks to MapSelectionController. Works with
 * any actor that exposes a node id, so this doesn't need to change when Damian's real node actors
 * replace NodeActor.
 */
public class MapInputHandler {

  private final MapSelectionController controller;

  public MapInputHandler(MapSelectionController controller) {
    this.controller = controller;
  }

  /**
   * Attaches input listeners to a single node actor. Routes clicks to the controller and fires
   * hover events ("nodeHovered" / "nodeUnhovered") so the UI can show hover feedback.
   *
   * @param actor node actor to listen on
   */
  public void attach(MapNodeActor actor) {
    actor.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            NodeState nodeState = actor.getNode().getState();

            if (nodeState == NodeState.COMPLETED || nodeState == NodeState.LOCKED) {
              AudioService.playSound(SoundId.ERROR, 0.5f);
            } else {

              switch (actor.getNode().getRoomType()) {
                case SHOP -> {
                  AudioService.playSound(SoundId.ENTER_SHOP, 0.5f);
                }
                case COMBAT -> {
                  AudioService.playSound(SoundId.ENTER_COMBAT, 0.4f);
                }
                case EVENT -> {
                  AudioService.playSound(SoundId.ENTER_ENCOUNTER, 0.4f);
                }
                case ELITE, FINAL -> {
                  AudioService.playSound(SoundId.ENTER_ELITE, 0.6f);
                }
              }
            }
            controller.onNodeClicked(actor.getNodeId());
            return true;
          }

          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            controller.getEvents().trigger("nodeHovered", actor.getNodeId());
            if (actor.getNode().getState() == NodeState.AVAILABLE) {
              actor.setHovered(true);
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            controller.getEvents().trigger("nodeUnhovered", actor.getNodeId());
            if (actor.getNode().getState() == NodeState.AVAILABLE) {
              actor.setHovered(false);
            }
          }
        });
  }

  /**
   * Attaches click listeners to a list of node actors.
   *
   * @param actors node actors to listen on
   */
  public void attachAll(List<MapNodeActor> actors) {
    for (MapNodeActor actor : actors) {
      attach(actor);
    }
  }
}
