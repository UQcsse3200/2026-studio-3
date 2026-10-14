package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BattleTutorialGuidanceViewTest {
  @Test
  void currentNumericStatsAndItemButtonAreBoundWithoutLegacyPrefixes() {
    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    Stage stage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
    try {
      Actor energy = new Actor();
      energy.setBounds(60, 150, 90, 90);
      Actor item = new Actor();
      item.setBounds(30, 560, 200, 40);
      com.badlogic.gdx.scenes.scene2d.ui.Label health =
          new com.badlogic.gdx.scenes.scene2d.ui.Label("100 / 100", skin);
      Group healthGroup = new Group();
      healthGroup.setBounds(250, 380, 150, 30);
      healthGroup.addActor(health);
      Actor enemyStats = new Actor();
      enemyStats.setBounds(780, 380, 180, 40);
      stage.addActor(energy);
      stage.addActor(item);
      stage.addActor(healthGroup);
      stage.addActor(enemyStats);
      var view =
          new BattleTutorialGuidanceView(stage, skin, List::of, () -> null, () -> item, () -> item);
      view.setStatTargets(() -> item, () -> energy, () -> health, () -> enemyStats);
      view.bindActions(() -> {}, () -> {});
      for (var step :
          List.of(
              BattleTutorialController.Step.ITEM_INVENTORY,
              BattleTutorialController.Step.ENERGY,
              BattleTutorialController.Step.HEALTH,
              BattleTutorialController.Step.ENEMY_STATS)) {
        view.show(BattleTutorialPrompt.referenceStep(step));
        assertEquals(1, view.highlightBounds().size());
        assertFalse(view.promptBounds().overlaps(view.highlightBounds().get(0)));
      }
      assertTrue(view.isPlayerHealthDisplayed(100));
      health.setText("96 / 100");
      assertFalse(view.isPlayerHealthDisplayed(100));
      assertTrue(view.isPlayerHealthDisplayed(96));
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.ENERGY));
      Rectangle before = view.highlightBounds().get(0);
      energy.moveBy(20, 30);
      Rectangle after = view.highlightBounds().get(0);
      assertEquals(before.x + 20, after.x, 0.01);
      assertEquals(before.y + 30, after.y, 0.01);
      view.clear();
    } finally {
      stage.dispose();
      skin.dispose();
    }
  }

  @Test
  void informationalTapConsumesBattleInputAndActionStepCannotBeSkipped() {
    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    try {
      Stage stage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
      Actor card = new Actor();
      card.setBounds(400, 200, 180, 300);
      stage.addActor(card);
      TextButton inventory = new TextButton("Inventory", skin);
      stage.addActor(inventory);
      BattleTutorialGuidanceView view =
          new BattleTutorialGuidanceView(
              stage, skin, () -> List.of(card), () -> card, () -> inventory, () -> inventory);
      AtomicInteger advances = new AtomicInteger();
      int listeners = stage.getRoot().getCaptureListeners().size;
      view.bindActions(advances::incrementAndGet, () -> {});
      view.bindActions(advances::incrementAndGet, () -> {});
      assertEquals(listeners + 1, stage.getRoot().getCaptureListeners().size);
      view.show(BattleTutorialPrompt.forStep(BattleTutorialController.Step.INTRO));
      tap(card);
      assertEquals(1, advances.get());
      view.show(BattleTutorialPrompt.forStep(BattleTutorialController.Step.PLAY_A_CARD));
      tap(inventory);
      assertEquals(1, advances.get());
      InputEvent cardDown = event(InputEvent.Type.touchDown);
      card.fire(cardDown);
      assertFalse(cardDown.isStopped());
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.END_TURN));
      tap(card);
      assertEquals(2, advances.get());
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.FREE_PLAY));
      assertNotNull(stage.getRoot().findActor("battle-tutorial-guidance"));
      InputEvent freePlayDown = event(InputEvent.Type.touchDown);
      card.fire(freePlayDown);
      assertFalse(freePlayDown.isStopped());
      view.clear();
      view.clear();
      assertEquals(listeners, stage.getRoot().getCaptureListeners().size);
      assertNull(stage.getRoot().findActor("battle-tutorial-guidance"));
      assertSame(stage, card.getStage());
      tap(card);
      assertEquals(2, advances.get());
      stage.dispose();
    } finally {
      skin.dispose();
    }
  }

  @Test
  void placementFallsBackAndStaysInsideViewport() {
    Rectangle target = new Rectangle(100, 700, 50, 40);
    Rectangle placed =
        BattleTutorialGuidanceView.place(
            target, 300, 100, 1280, 800, BattleTutorialGuidanceView.Placement.ABOVE);
    assertTrue(placed.x > target.x + target.width);
    assertFalse(placed.overlaps(target));
    for (var direction : BattleTutorialGuidanceView.Placement.values()) {
      Rectangle small =
          BattleTutorialGuidanceView.place(
              new Rectangle(310, 190, 40, 40), 390, 200, 360, 260, direction);
      assertTrue(small.x >= 16 && small.y >= 16);
      assertTrue(small.x + small.width <= 344);
      assertTrue(small.y + small.height <= 196);
    }
  }

  @Test
  void boundsFollowLiveMovementRotationAndParentScaling() {
    Group parent = new Group();
    parent.setPosition(80, 60);
    parent.setScale(2);
    Actor target = new Actor();
    target.setBounds(10, 20, 40, 30);
    parent.addActor(target);
    Rectangle first = BattleTutorialGuidanceView.bounds(target);
    assertEquals(94, first.x, 0.01);
    assertEquals(94, first.y, 0.01);
    assertEquals(92, first.width, 0.01);
    parent.moveBy(50, 25);
    Rectangle second = BattleTutorialGuidanceView.bounds(target);
    assertEquals(first.x + 50, second.x, 0.01);
    assertEquals(first.y + 25, second.y, 0.01);
    target.setRotation(90);
    Rectangle rotated = BattleTutorialGuidanceView.bounds(target);
    assertEquals(72, rotated.width, 0.01);
    assertEquals(92, rotated.height, 0.01);
  }

  @Test
  void handHasOneUnionAndDragPromptDoesNotFollowTheCard() {
    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    try {
      Stage stage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
      Actor first = new Actor();
      first.setBounds(300, 80, 200, 300);
      stage.addActor(first);
      Actor second = new Actor();
      second.setBounds(480, 100, 200, 300);
      stage.addActor(second);
      BattleTutorialGuidanceView view =
          new BattleTutorialGuidanceView(
              stage, skin, () -> List.of(first, second), () -> first, () -> second, () -> second);
      view.bindActions(() -> {}, () -> {});
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.HAND));
      List<Rectangle> hand = view.highlightBounds();
      assertEquals(1, hand.size());
      assertTrue(hand.get(0).contains(BattleTutorialGuidanceView.bounds(first)));
      assertTrue(hand.get(0).contains(BattleTutorialGuidanceView.bounds(second)));
      assertEquals(0.52f, view.dimAlpha());
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.USED_CARD));
      assertEquals(0.28f, view.dimAlpha());
      assertEquals(1, view.highlightBounds().size());
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.HAND));
      int z = first.getZIndex();
      view.show(BattleTutorialPrompt.referenceStep(BattleTutorialController.Step.PLAY_A_CARD));
      Rectangle before = view.promptBounds();
      first.moveBy(400, 100);
      view.highlightBounds();
      assertEquals(before, view.promptBounds());
      assertEquals(stage.getRoot().getChildren().size - 1, first.getZIndex());
      view.clear();
      assertEquals(z, first.getZIndex());
      stage.dispose();
    } finally {
      skin.dispose();
    }
  }

  private static InputEvent event(InputEvent.Type type) {
    InputEvent event = new InputEvent();
    event.setType(type);
    event.setPointer(0);
    event.setButton(0);
    return event;
  }

  private static void tap(Actor actor) {
    actor.fire(event(InputEvent.Type.touchDown));
    // Actor.fire does not dispatch Stage touch focus; dispatch its captured listener explicitly.
    InputEvent up = event(InputEvent.Type.touchUp);
    up.setListenerActor(actor.getStage().getRoot());
    up.setTarget(actor);
    for (var listener : actor.getStage().getRoot().getCaptureListeners()) listener.handle(up);
  }
}
