package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

  @Test
  void shouldShowCurrentHealthOnCreate() {
    display.create();

    String expected = stats.getHealth() + " / " + stats.getMaxHealth();
    assertEquals(expected, display.getHealthLabel().getText().toString());
  }

  @Test
  void shouldUpdateTheHealthText() {
    display.create();

    display.updatePlayerHealthUI(12, 30);

    assertEquals("12 / 30", display.getHealthLabel().getText().toString());
  }

  @Test
  void shouldOnlyShowTheArmourIconWhileThePlayerHasArmour() {
    display.create();
    assertEquals(1, display.table.getChildren().size);

    display.updateArmourUI(5);
    assertEquals(2, display.table.getChildren().size);

    display.updateArmourUI(0);
    assertEquals(1, display.table.getChildren().size);
  }

  @Test
  void shouldLeaveStatusEffectsToTheStatusIconDisplay() {
    stats.applyStatusEffect("SILENCE", 1, 3);

    display.create();
    display.update();

    assertEquals(1, display.table.getChildren().size);
  }

  @Test
  void shouldRemoveItsActorsAndTolerateBeingDisposedTwice() {
    display.create();
    Group parent = new Group();
    parent.addActor(display.table);

    display.dispose();

    assertNull(display.table.getParent());
    assertEquals(0, parent.getChildren().size);
    assertDoesNotThrow(display::dispose);
  }
}
