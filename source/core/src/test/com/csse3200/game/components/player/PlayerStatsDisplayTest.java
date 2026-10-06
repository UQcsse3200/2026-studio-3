package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerStatsDisplayTest {
  private CombatStatsComponent stats;
  private PlayerStatsDisplay display;
  private ResourceService resources;

  @BeforeEach
  void setUp() {
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);
    resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(20);
    when(texture.getHeight()).thenReturn(20);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    stats = spy(new CombatStatsComponent(30, 4));
    display = new PlayerStatsDisplay();
    new Entity().addComponent(stats).addComponent(new EnergyComponent(3)).addComponent(display);
  }

  @AfterEach
  void tearDown() {
    display.dispose();
  }

  private Table statusRow() {
    return (Table) display.table.getChildren().get(5);
  }

  private Table statusIcons() {
    return (Table) statusRow().getChildren().get(1);
  }

  private String durationText() {
    return ((Label) statusIcons().getChildren().get(1)).getText().toString();
  }

  @Test
  void shouldInitiallyHideEmptyOrBuffOnlyStatusRow() {
    stats.applyStatusEffect("STRENGTH", 2, 0);
    display.create();

    assertFalse(statusRow().isVisible());
    assertEquals(0, statusIcons().getChildren().size);
  }

  @Test
  void shouldRenderStatusesAppliedBeforeCreate() {
    stats.applyStatusEffect("TAUNT:42", 42, 3);
    display.create();

    assertTrue(statusRow().isVisible());
    assertEquals(2, statusIcons().getChildren().size);
    assertEquals("3", durationText());
  }

  @Test
  void shouldReuseActorsAndAvoidStatusSnapshotsOnUnchangedFrames() {
    stats.applyStatusEffect("SILENCE", 1, 3);
    display.create();
    Actor icon = statusIcons().getChildren().first();
    Actor count = statusIcons().getChildren().get(1);
    clearInvocations(stats, resources);

    for (int frame = 0; frame < 120; frame++) {
      display.update();
    }

    assertSame(icon, statusIcons().getChildren().first());
    assertSame(count, statusIcons().getChildren().get(1));
    verify(stats, never()).getStatusEffectDurations();
    verify(resources, never()).getAsset(anyString(), eq(Texture.class));
  }

  @Test
  void shouldRebuildAfterApplyingOrRemovingDisplayedStatuses() {
    display.create();
    stats.applyStatusEffect("SILENCE", 1, 3);
    display.update();
    assertTrue(statusRow().isVisible());
    assertEquals("3", durationText());

    stats.applyStatusEffect("TAUNT:42", 42, 2);
    display.update();
    assertEquals(4, statusIcons().getChildren().size);

    stats.removeStatusEffect("SILENCE");
    display.update();
    assertEquals(2, statusIcons().getChildren().size);
    assertEquals("2", durationText());

    stats.removeStatusEffect("TAUNT:42");
    display.update();
    assertFalse(statusRow().isVisible());
    assertEquals(0, statusIcons().getChildren().size);
  }

  @Test
  void shouldRefreshDurationsAndHideExpiredStatusesAfterIndividualTicks() {
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.create();
    Actor oldCount = statusIcons().getChildren().get(1);

    assertFalse(stats.tickStatusEffect("SILENCE"));
    display.update();
    assertEquals("1", durationText());
    assertNotSame(oldCount, statusIcons().getChildren().get(1));

    assertTrue(stats.tickStatusEffect("SILENCE"));
    display.update();
    assertFalse(statusRow().isVisible());
    assertEquals(0, statusIcons().getChildren().size);
  }

  @Test
  void shouldRefreshAfterBulkOrDirectDurationTicksWithoutEvents() {
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 1, 3);
    display.create();

    stats.updateStatusEffects();
    display.update();
    assertEquals("2", durationText());

    stats.getStatusEffect("DAMAGE_ON_CARD_PLAY").tickAndCheckExpired();
    display.update();
    assertEquals("1", durationText());
  }

  @Test
  void shouldCoalesceDurationReapplicationsIntoOneRefresh() {
    stats.applyStatusEffect("SILENCE", 1, 3);
    display.create();
    Actor oldCount = statusIcons().getChildren().get(1);
    clearInvocations(stats, resources);

    stats.applyStatusEffect("SILENCE", 1, 4);
    stats.applyStatusEffect("SILENCE", 1, 5);
    display.update();

    assertEquals("5", durationText());
    assertNotSame(oldCount, statusIcons().getChildren().get(1));
    verify(stats).getStatusEffectDurations();
    verify(resources).getAsset(anyString(), eq(Texture.class));
  }

  @Test
  void shouldNotRebuildForSameDurationReapplicationOrBuffChanges() {
    stats.applyStatusEffect("SILENCE", 1, 3);
    display.create();
    Actor count = statusIcons().getChildren().get(1);

    stats.applyStatusEffect("SILENCE", 2, 3);
    display.update();
    assertSame(count, statusIcons().getChildren().get(1));
    clearInvocations(stats, resources);

    stats.applyStatusEffect("STRENGTH", 2, 0);
    stats.removeStatusEffect("STRENGTH");
    display.update();
    assertSame(count, statusIcons().getChildren().get(1));
    verify(stats, never()).getStatusEffectDurations();
    verify(resources, never()).getAsset(anyString(), eq(Texture.class));
  }

  @Test
  void shouldDetachStatusListenersAndActorsOnDispose() {
    stats.applyStatusEffect("SILENCE", 1, 3);
    display.create();
    Entity player = stats.getEntity();
    @SuppressWarnings("unchecked")
    EventListener1<String> applied = (EventListener1<String>) mock(EventListener1.class);
    player.getEvents().addListener("statusEffectApplied", applied);
    Group parent = new Group();
    parent.addActor(display.table);
    clearInvocations(stats, resources);

    display.dispose();
    stats.applyStatusEffect("SILENCE", 1, 2);
    display.update();

    assertEquals("3", durationText());
    verify(stats, never()).getStatusEffectDurations();
    verify(resources, never()).getAsset(anyString(), eq(Texture.class));
    verify(applied).handle("SILENCE");
    assertNull(display.table.getParent());
    assertEquals(0, parent.getChildren().size);
  }
}
