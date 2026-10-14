package com.csse3200.game.components.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class StatusIconDisplayTest {
  private Stage stage;
  private ResourceService resources;
  private CombatStatsComponent stats;
  private Entity entity;
  private StatusIconDisplay display;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1200f, 800f), mock(Batch.class));
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(128);
    when(texture.getHeight()).thenReturn(128);
    when(resources.containsAsset(anyString(), eq(Texture.class))).thenReturn(true);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    stats = new CombatStatsComponent(30, 4);
    display = new StatusIconDisplay(-0.6f, false);
    entity = new Entity().addComponent(stats).addComponent(display);
  }

  @AfterEach
  void tearDown() {
    display.dispose();
    stage.dispose();
  }

  @Test
  void shouldStayHiddenWithoutStatuses() {
    display.create();

    assertFalse(row().isVisible());
    assertEquals(0, row().getChildren().size);
  }

  @Test
  void shouldShowStatusesAppliedBeforeCreate() {
    stats.applyStatusEffect("SILENCE", 1, 2);

    display.create();

    assertTrue(row().isVisible());
    assertEquals(1, row().getChildren().size);
  }

  @Test
  void shouldIgnoreStatusesWithoutDisplayInfo() {
    display.create();

    stats.applyStatusEffect("BURN", 1, 2);
    display.update();

    assertFalse(row().isVisible());
  }

  @Test
  void shouldOrderIconsByDisplayOrderNotApplicationOrder() {
    display.create();

    stats.applyStatusEffect("TAUNT:5", 5, 2);
    stats.applyStatusEffect("POISON", 3, 4);
    display.update();

    assertEquals("4", count(0));
    assertEquals("2", count(1));
  }

  @Test
  void shouldShowStacksForValueStatusesAndTurnsForTheRest() {
    display.create();

    stats.applyStatusEffect("STRENGTH", 2, 0);
    stats.applyStatusEffect("SILENCE", 1, 3);
    display.update();

    assertEquals("2", count(0));
    assertEquals("3", count(1));
  }

  @Test
  void shouldLeaveTheCountBlankForPermanentTurnBasedStatuses() {
    display.create();

    stats.applyStatusEffect("VULNERABLE", 1, 0);
    display.update();

    assertEquals("", count(0));
  }

  @Test
  void shouldUpdateTheCountWhenAStatusTicksDown() {
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.create();

    stats.tickStatusEffect("SILENCE");
    display.update();

    assertEquals("1", count(0));
  }

  @Test
  void shouldHideOnceTheLastStatusIsRemoved() {
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.create();

    stats.removeStatusEffect("SILENCE");
    display.update();

    assertFalse(row().isVisible());
    assertEquals(0, row().getChildren().size);
  }

  @Test
  void shouldSkipIconsWhoseTextureIsNotLoaded() {
    when(resources.containsAsset(anyString(), eq(Texture.class))).thenReturn(false);
    stats.applyStatusEffect("SILENCE", 1, 2);

    display.create();

    assertEquals(0, row().getChildren().size);
  }

  @Test
  void shouldExplainTheStatusWhenItsIconIsHovered() {
    stats.applyStatusEffect("POISON", 3, 2);
    display.create();

    hover(row().getChildren().first());

    StatusTooltip tooltip = tooltip();
    assertTrue(tooltip.isVisible());
    assertEquals("Poison 3", tooltipText(tooltip, 0));
    assertEquals(StatusInfo.POISON.describe(3), tooltipText(tooltip, 1));
    assertEquals("2 turns remaining", tooltipText(tooltip, 2));
  }

  @Test
  void shouldFillTheStatusValueIntoTheTooltipDescription() {
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 4, 2);
    display.create();

    hover(row().getChildren().first());

    assertEquals("Takes 4 damage each time a card is played.", tooltipText(tooltip(), 1));
  }

  @Test
  void shouldCloseTheTooltipOnceTheStatusEnds() {
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.create();
    hover(row().getChildren().first());
    StatusTooltip tooltip = tooltip();

    stats.removeStatusEffect("SILENCE");
    stage.act(0f);

    assertFalse(tooltip.isVisible());
  }

  @Test
  void shouldCentreTheRowJustBelowTheAnchor() {
    ServiceLocator.registerCamera(scalingCamera());
    entity.setPosition(5f, 3f);
    entity.setScale(1f, 1f);
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.create();

    display.update();

    assertEquals(550f, row().getX() + row().getWidth() / 2f, 0.01f);
    assertEquals(236f, row().getTop(), 0.01f);
  }

  @Test
  void shouldPlaceTheRowJustAboveTheAnchorWhenAskedTo() {
    display.dispose();
    display = new StatusIconDisplay(1.5f, true);
    entity = new Entity().addComponent(stats).addComponent(display);
    ServiceLocator.registerCamera(scalingCamera());
    entity.setPosition(5f, 3f);
    entity.setScale(1f, 1f);
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.create();

    display.update();

    assertEquals(454f, row().getY(), 0.01f);
  }

  @Test
  void shouldRemoveTheRowFromTheStageOnDispose() {
    display.create();
    Table row = row();

    display.dispose();

    assertNull(row.getStage());
  }

  /** The row is the first table the display adds to the stage; the tooltip is added later. */
  private Table row() {
    for (Actor actor : stage.getActors()) {
      if (actor instanceof Table table && !(actor instanceof StatusTooltip)) {
        return table;
      }
    }
    throw new AssertionError("No status row on the stage");
  }

  private StatusTooltip tooltip() {
    for (Actor actor : stage.getActors()) {
      if (actor instanceof StatusTooltip tooltip) {
        return tooltip;
      }
    }
    throw new AssertionError("No tooltip on the stage");
  }

  private String count(int iconIndex) {
    Stack icon = (Stack) row().getChildren().get(iconIndex);
    Table corner = (Table) icon.getChildren().get(1);
    return ((Label) corner.getChildren().first()).getText().toString();
  }

  private static String tooltipText(StatusTooltip tooltip, int line) {
    return ((Label) tooltip.getChildren().get(line)).getText().toString();
  }

  private static void hover(Actor icon) {
    InputEvent event = new InputEvent();
    event.setType(InputEvent.Type.enter);
    event.setPointer(-1);
    icon.fire(event);
  }

  /** A camera whose projection simply multiplies world coordinates by 100. */
  private static Camera scalingCamera() {
    return new OrthographicCamera() {
      @Override
      public Vector3 project(Vector3 worldCoords) {
        return worldCoords.scl(100f);
      }
    };
  }
}
