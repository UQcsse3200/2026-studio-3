package com.csse3200.game.components.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class StatusTooltipTest {
  private static final StatusTooltip.Content POISON =
      new StatusTooltip.Content("Poison", "Deals 1 damage per stack.", "2 turns remaining");

  private Skin skin;
  private Stage stage;
  private StatusTooltip tooltip;
  private Actor icon;
  private AtomicReference<StatusTooltip.Content> current;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    stage = new Stage(new FitViewport(1200f, 800f), mock(Batch.class));
    tooltip = new StatusTooltip(skin);
    icon = new Actor();
    icon.setBounds(500f, 300f, 40f, 40f);
    stage.addActor(icon);
    current = new AtomicReference<>(POISON);
    tooltip.attachTo(icon, current::get);
  }

  @AfterEach
  void tearDown() {
    tooltip.hide();
    stage.dispose();
    skin.dispose();
  }

  @Test
  void shouldShowTheStatusTextCentredAboveTheIcon() {
    mouse(icon, InputEvent.Type.enter, null);

    assertTrue(tooltip.isVisible());
    assertSame(stage, tooltip.getStage());
    assertEquals("Poison", text(0));
    assertEquals("Deals 1 damage per stack.", text(1));
    assertEquals("2 turns remaining", text(2));
    assertTrue(tooltip.getY() > icon.getTop());
    float iconCentre = icon.getX() + icon.getWidth() / 2f;
    assertEquals(iconCentre, tooltip.getX() + tooltip.getWidth() / 2f, 0.01f);
  }

  @Test
  void shouldNotShowWhenThereIsNothingToExplain() {
    current.set(null);

    mouse(icon, InputEvent.Type.enter, null);

    assertFalse(tooltip.isVisible());
    assertNull(tooltip.getStage());
  }

  @Test
  void shouldHideWhenThePointerLeavesTheIcon() {
    mouse(icon, InputEvent.Type.enter, null);
    mouse(icon, InputEvent.Type.exit, null);

    assertFalse(tooltip.isVisible());
    assertNull(tooltip.getStage());
  }

  @Test
  void shouldStayOpenWhenThePointerMovesOntoAChildOfTheIcon() {
    Group iconGroup = new Group();
    iconGroup.setBounds(600f, 300f, 40f, 40f);
    Actor child = new Actor();
    iconGroup.addActor(child);
    stage.addActor(iconGroup);
    tooltip.attachTo(iconGroup, current::get);

    mouse(iconGroup, InputEvent.Type.enter, null);
    mouse(iconGroup, InputEvent.Type.exit, child);

    assertTrue(tooltip.isVisible());
  }

  @Test
  void shouldIgnoreTouchPointers() {
    InputEvent touch = new InputEvent();
    touch.setType(InputEvent.Type.enter);
    touch.setPointer(0);
    icon.fire(touch);

    assertFalse(tooltip.isVisible());
  }

  @Test
  void shouldKeepTheNewIconsPanelWhenThePreviousIconExitsLate() {
    Actor otherIcon = new Actor();
    otherIcon.setBounds(700f, 300f, 40f, 40f);
    stage.addActor(otherIcon);
    StatusTooltip.Content silence =
        new StatusTooltip.Content("Silence", "Cannot play cards.", "1 turn remaining");
    tooltip.attachTo(otherIcon, () -> silence);

    mouse(icon, InputEvent.Type.enter, null);
    mouse(otherIcon, InputEvent.Type.enter, null);
    mouse(icon, InputEvent.Type.exit, null);

    assertTrue(tooltip.isVisible());
    assertEquals("Silence", text(0));
  }

  @Test
  void shouldRefreshTheTextWhenTheStatusChanges() {
    mouse(icon, InputEvent.Type.enter, null);

    current.set(
        new StatusTooltip.Content("Poison", "Deals 1 damage per stack.", "1 turn remaining"));
    stage.act(0f);

    assertEquals("1 turn remaining", text(2));
  }

  @Test
  void shouldHideWhenTheStatusEnds() {
    mouse(icon, InputEvent.Type.enter, null);

    current.set(null);
    stage.act(0f);

    assertFalse(tooltip.isVisible());
    assertNull(tooltip.getStage());
  }

  @Test
  void shouldHideWhenTheIconIsRemoved() {
    mouse(icon, InputEvent.Type.enter, null);

    icon.remove();
    stage.act(0f);

    assertFalse(tooltip.isVisible());
  }

  @Test
  void shouldHideWhenTheIconIsHidden() {
    mouse(icon, InputEvent.Type.enter, null);

    icon.setVisible(false);
    stage.act(0f);

    assertFalse(tooltip.isVisible());
  }

  @Test
  void shouldFollowTheIconWhenItMoves() {
    mouse(icon, InputEvent.Type.enter, null);
    float panelX = tooltip.getX();
    float panelY = tooltip.getY();

    icon.moveBy(30f, 20f);
    stage.act(0f);

    assertEquals(panelX + 30f, tooltip.getX(), 0.01f);
    assertEquals(panelY + 20f, tooltip.getY(), 0.01f);
  }

  @Test
  void shouldMoveBelowTheIconWhenThereIsNoRoomAbove() {
    icon.setPosition(500f, 770f);

    mouse(icon, InputEvent.Type.enter, null);

    assertTrue(tooltip.getTop() < icon.getY());
  }

  @Test
  void shouldStayInsideTheStageNearTheRightEdge() {
    icon.setPosition(1170f, 300f);

    mouse(icon, InputEvent.Type.enter, null);

    assertTrue(tooltip.getRight() <= stage.getWidth() + 0.01f);
    assertTrue(tooltip.getX() >= 0f);
  }

  @Test
  void shouldNeverBlockInputToActorsUnderneath() {
    mouse(icon, InputEvent.Type.enter, null);

    assertNull(tooltip.hit(10f, 10f, true));
  }

  @Test
  void shouldAllowHidingWhenNotShown() {
    tooltip.hide();

    assertFalse(tooltip.isVisible());
    assertNull(tooltip.getStage());
  }

  private String text(int index) {
    return ((Label) tooltip.getChildren().get(index)).getText().toString();
  }

  private static void mouse(Actor target, InputEvent.Type type, Actor related) {
    InputEvent event = new InputEvent();
    event.setType(type);
    event.setPointer(-1);
    event.setRelatedActor(related);
    target.fire(event);
  }
}
